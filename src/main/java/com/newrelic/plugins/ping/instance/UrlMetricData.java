package com.newrelic.plugins.ping.instance;

import java.util.Date;

public class UrlMetricData {

	private String name;
	private String url;
	private int httpStatusCode;
	private int urlResponseStatus;
	private long executionTime;
	private Date firstFailureTime;
	private Date lastFailureTime;
	private boolean urlDown = false;
	
	private int failStatusTime = 5;			// after this # of minutes report this url increment UnresponsiveUrls (special summary metric) 
	private int failStatusResetTime = 3;	// when url is alive for 3 minutes, take it out of Unresponsive Urls count
	
	private long pingCount = 0;
	private long successCount = 0;		// set for response codes 1xx & 2xx & 3xx
	private long failureCount = 0; 		// all failures - including bad http response or any exception / timeout / etc.
	private long errorCount = 0; 		// set for response codes 4xx & 5xx 
	private long timeoutCount = 0;
	private long unknownHostCount = 0;
	private long httpExceptionCount = 0;
	private long ioExceptionCount = 0;
		
	
	
	public UrlMetricData() {
		
	}
	
	public UrlMetricData(String name, String url) {
		this.name = name;
		this.url = url;
		this.httpStatusCode = 0;
		this.urlResponseStatus = 0;
		this.executionTime = 0;
	}
	
	public UrlMetricData(String name, String url, int failStatusTime, int failStatusResetTime) {
		this.name = name;
		this.url = url;
		this.httpStatusCode = 0;
		this.urlResponseStatus = 0;
		this.executionTime = 0;
		this.failStatusTime = failStatusTime;
		this.failStatusResetTime = failStatusResetTime;
	}
	
	public UrlMetricData(String name, String url, int httpStatusCode, int urlResponseStatus, long executionTime, int failStatusTime, int failStatusResetTime) {
		this.name = name;
		this.url = url;
		this.httpStatusCode = httpStatusCode;
		this.urlResponseStatus = urlResponseStatus;
		this.executionTime = executionTime;
		this.failStatusTime = failStatusTime;
		this.failStatusResetTime = failStatusResetTime;
	}
	
	
	public void clearMetrics() {
		setHttpStatusCode(0);
		setUrlResponseStatus(0);
		setExecutionTime(0);
	}
	
	
	public void updateMetricStatus() {
		
		Date currentTime = new Date();
		switch (urlResponseStatus) {
		
		case 1: // current ping successful
			if ((currentTime.getTime() - lastFailureTime.getTime()) >= (failStatusResetTime * 60 * 1000)) {
				// if server has been available for the past 3 minutes, reset the required metrics and declare the url as available
				failureCount = 0;
				setUrlDown(false);
				firstFailureTime = null;
				lastFailureTime = null;
			}
			break;
			
		case 2: case 3: case 4: case 5: case 6: // some failure occurred (http status code 4xx / 5xx / exceptions)
			if (failureCount == 1) { // url has failed once - set the start time of the failure
				firstFailureTime = lastFailureTime;
			}
			// else if (failureCount >= 5 && (lastFailureTime.getTime() - firstFailureTime.getTime()) >= (failStatusTime * 60 * 1000)) { // 5+  minutes
			else if ( (lastFailureTime.getTime() - firstFailureTime.getTime()) >= (failStatusTime * 60 * 1000) ) { // 5+  minutes
				// there were more failures previously - now there are enough to trigger an alert
				// need to report *special summary* metric to indicate a server has been unavailable for 5 minutes or more
				setUrlDown(true);
			}				
			break;			
		}

	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(int httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public int getUrlResponseStatus() {
		return urlResponseStatus;
	}

	public void setUrlResponseStatus(int urlResponseStatus) {
		this.urlResponseStatus = urlResponseStatus;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public Date getLastFailureTime() {
		return lastFailureTime;
	}

	public void setLastFailureTime(Date lastFailureTime) {
		this.lastFailureTime = lastFailureTime;
	}

	public void incrementPingCount() {
		pingCount++;
	}
	
	public long getPingCount() {
		return pingCount;
	}

	public void incrementSuccessCount() {
		successCount++;
	}
	
	public long getSuccessCount() {
		return successCount;
	}

	public void incrementFailureCount() {
		failureCount++;
	}
	
	public long getFailureCount() {
		return failureCount;
	}

	public void incrementErrorCount() {
		errorCount++;
	}
	
	public long getErrorCount() {
		return errorCount;
	}

	public void incrementTimeoutCount() {
		timeoutCount++;
	}
	
	public long getTimeoutCount() {
		return timeoutCount;
	}

	public void incrementUnknownHostCount() {
		unknownHostCount++;
	}
	
	public long getUnknownHostCount() {
		return unknownHostCount;
	}

	public void incrementHttpExceptionCount() {
		httpExceptionCount++;
	}
	
	public long getHttpExceptionCount() {
		return httpExceptionCount;
	}

	public void incrementIoExceptionCount() {
		ioExceptionCount++;
	}
	
	public long getIoExceptionCount() {
		return ioExceptionCount;
	}

	public boolean isUrlDown() {
		return urlDown;
	}

	public void setUrlDown(boolean urlDown) {
		this.urlDown = urlDown;
	}

}
