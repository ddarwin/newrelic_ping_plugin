package com.newrelic.plugins.ping.instance;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import java.net.URL;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLSession;

import com.newrelic.metrics.publish.util.Logger;

//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.HttpException;
//import org.apache.commons.httpclient.HttpMethod;
//import org.apache.commons.httpclient.methods.GetMethod;
//import org.apache.commons.httpclient.params.HttpClientParams;

public class Ping implements Runnable, X509TrustManager {

	private UrlMetricData urlMetricData;
	private Map<Integer, Long> httpStatusCodes;
	private Map<String, Long>summaryMetrics;
	private Map<String, Long>unresponsiveUrlList;
	private int timeout;

    private static final Logger logger = Logger.getLogger(Ping.class);

	public Ping() {
		
	}
	
	public Ping(UrlMetricData urlMetricData, Map<Integer, Long>httpStatusCodes, Map<String, Long> summaryMetrics, Map<String, Long>unresponsiveUrlList, int timeout) {
		this.urlMetricData = urlMetricData;
		this.httpStatusCodes = httpStatusCodes;
		this.summaryMetrics = summaryMetrics;
		this.unresponsiveUrlList = unresponsiveUrlList;
		this.timeout = timeout;
	}

	
    @Override
    public void run() {
    	
        int httpStatusCode = 0;
        Date startTime, endTime;

        URL url = null;
        HttpURLConnection connection = null;
    	try {
            urlMetricData.clearMetrics(); // set httpStatusCode, urlResponseStatus, executionTime to 0
	        url = new URL(urlMetricData.getUrl());
	        
        	startTime = new Date();
        	//System.out.println("protocol: " + url.getProtocol());
	        if (url.getProtocol().equals("https")) {  //HTTPS ping
	        	// handle SSL

	    		// set ssl socket factory
	    		SSLContext sslCtx = SSLContext.getInstance("TLSv1"); // ("SSLv3");
	    		TrustManager[] trustManager = new TrustManager[] {new Ping()};
	    		sslCtx.init(null, trustManager, null);
	    		HttpsURLConnection.setDefaultSSLSocketFactory(sslCtx.getSocketFactory());

	    		// set default host verifier (true for all hosts/sessions)
	    		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
	    			public boolean verify(String urlHostName, SSLSession session) {
	    				//System.out.println(">>>>>>>>>>>>>>>>>> in VERIFY: " + urlHostName + "    ---- session: " + session);
	    				return true;
	    			}
	    		});

    			connection = (HttpsURLConnection) url.openConnection();
    			connection.setConnectTimeout(timeout);
    			// connection.setDoInput(true); no need to set - default is true
    			connection.setDoOutput(true);
    			connection.setRequestMethod("GET"); 
    			httpStatusCode = connection.getResponseCode();
    			connection.connect();
	        }
	        else { //HTTP ping

	        	connection = (HttpURLConnection) url.openConnection();
    			connection.setConnectTimeout(timeout);
	        	httpStatusCode = connection.getResponseCode();
    			connection.connect();
	        }
            endTime = new Date();
            urlMetricData.setExecutionTime(endTime.getTime() - startTime.getTime());
            
            urlMetricData.setHttpStatusCode(httpStatusCode);
            logger.info("url: " + urlMetricData.getUrl() + " -- status code: " + httpStatusCode);
            if (httpStatusCode >= 100 && httpStatusCode < 400) { 
            	urlMetricData.setUrlResponseStatus(1);
        		urlMetricData.incrementSuccessCount();
        		incrementSummaryMetric("SuccessCount");
            }
            else {
            	urlMetricData.setUrlResponseStatus(2); // 1=success, 2=error
            	urlMetricData.incrementErrorCount();
        		incrementSummaryMetric("ErrorCount");
            }
        } catch(SocketTimeoutException e) {
        	urlMetricData.setUrlResponseStatus(3); // 3=SocketTimeoutException
    		urlMetricData.incrementTimeoutCount();
    		incrementSummaryMetric("TimeoutExceptionCount");
    	} catch(java.net.UnknownHostException e) {
        	urlMetricData.setUrlResponseStatus(4); // 4=UnknownHostException
    		urlMetricData.incrementUnknownHostCount();
    		incrementSummaryMetric("UnknownHostCount");
    	}/* catch (HttpException e) {
        	urlMetricData.setUrlResponseStatus(5); // 5=HttpException
        	urlMetricData.incrementHttpExceptionCount();
        	incrementSummaryMetric("HttpExceptionCount");
		}*/ catch (IOException e) {
			urlMetricData.setUrlResponseStatus(6); // 6=IOException
        	urlMetricData.incrementIoExceptionCount();
        	incrementSummaryMetric("IOExceptionCount");
		} catch (NoSuchAlgorithmException e) {
			urlMetricData.setUrlResponseStatus(7); // 6=IOException
        	urlMetricData.incrementIoExceptionCount();
        	incrementSummaryMetric("NoSuchAlgorithmException");
		} catch (KeyManagementException e) {
			urlMetricData.setUrlResponseStatus(8); // 6=IOException
        	urlMetricData.incrementIoExceptionCount();
        	incrementSummaryMetric("KeyManagementException");
		} finally {
			try { 
				connection.disconnect();
			} catch (Throwable t) {}
		}


    	
		urlMetricData.incrementPingCount(); // always increment even if there was an exception and/or the connection was not successful
		
		if (httpStatusCode == 0 || httpStatusCode >= 400) {
			urlMetricData.incrementFailureCount();		// always increment for all failures, both bad http responsecode and exceptions
			incrementSummaryMetric("FailureCount");
			urlMetricData.setLastFailureTime(new Date());
			if (httpStatusCode == 0 ) {
				incrementSummaryMetric("TotalExceptions");
			}
		}

		if (httpStatusCode > 0)
			incrementHttpStatusCodesCount(httpStatusCode);
		
		// set special flag if the url has been down for 5 minutes or more
		// also reset some metric data if url has been responding for at least 3 minutes
		urlMetricData.updateMetricStatus(); 
		
		if (urlMetricData.isUrlDown()) {
			unresponsiveUrlList.put(urlMetricData.getName(), urlMetricData.getFailureCount()); 
		}

    }


	private void incrementHttpStatusCodesCount(int httpStatusCode) {      
		long oldValue = 0;
		if (httpStatusCodes.containsKey(httpStatusCode)) {
			oldValue = httpStatusCodes.get(httpStatusCode);
		}	
		httpStatusCodes.put(httpStatusCode, oldValue + 1); // count # of occurrences for the http status code
	}
	
	
	
	private void incrementSummaryMetric(String metricKey) {
		long oldValue = 0;
    	if (summaryMetrics.containsKey(metricKey)) {
    		oldValue = summaryMetrics.get(metricKey);
    	}	
    	summaryMetrics.put(metricKey, oldValue + 1);
	}
	
	
	
	// TrustManager Methods
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		//  Auto-generated method stub
		
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		//  Auto-generated method stub
		
	}
}

