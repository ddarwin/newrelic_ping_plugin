package com.newrelic.plugins.ping.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.configuration.Config;
import com.newrelic.metrics.publish.util.Logger;

/**
 * This class obtains server ping metrics, and passes
 * these metrics to New Relic controller component using New Relic java SDK
 * 
 */
public class PingAgent extends Agent {
    private static final String GUID = "com.newrelic.plugins.urlping";
    private static final String version = "2.0";

    public static final String AGENT_DEFAULT_HOST = "localhost"; // Default values for Ping Agent

    public static final String AGENT_CONFIG_FILE = "plugin.json";

    public static final String COMMA = ",";
    public static final String SEPARATOR = "/";

    private String name; // Agent Name

//    private String host;
    private int timeout;
    private int failStatusTime;
    private int failStatusResetTime;
    private int threadPoolSize;
    private Map<String, String> urls;

    private Map<String, UrlMetricData> results = new HashMap<String, UrlMetricData>();
    private Map<Integer, Long> httpStatusCodes = new HashMap<Integer, Long>();
    private Map<String, Long> summaryMetrics = new HashMap<String, Long>();
    private Map<String, Long>unresponsiveUrlList = new HashMap<String, Long>();


    private boolean firstHarvest = true;

    long harvestCount = 0;

    //final Logger logger; // Local convenience variable
    //private static final Logger logger = Logger.getLogger(PingAgent.class);
    
    private static final Logger logger = Logger.getLogger(PingAgent.class);

    /**
     * Default constructor to create a new Oracle Agent
     * 
     * @param map
     * @param String Human name for Agent
     * @param String Oracle Instance host:port
     * @param String Oracle user
     * @param String Oracle user password
     * @param String CSVm List of metrics to be monitored
     */
    public PingAgent(String name, int timeout, int failStatusTime, int failStatusResetTime, int threadPoolSize, Map<String, String> urls) {
        super(GUID, version);

        this.name = name; // Set local attributes for new class object
//        this.host = host;
        this.timeout = timeout;
        this.failStatusTime = failStatusTime;
        this.failStatusResetTime = failStatusResetTime;
        this.threadPoolSize = threadPoolSize;
        this.urls = urls;

//        logger = Context.getLogger(); // Set logging to current Context
        
    }

    
    /**
     * This method is run for every poll cycle of the Agent.
     * 
     */
    public void pollCycle() {
        logger.debug("Gathering Server Ping metrics. " + getAgentInfo());
        logger.info("Reporting Server Ping metrics: Pinger instance \"" + this.name + "\": harvest cycle: " + (++harvestCount) + ".");
        httpStatusCodes.clear();
        summaryMetrics.clear();
        unresponsiveUrlList.clear();
        // initializeMetrics(); //used only when building dashboards -- no need to call metho otherwise
        if (!firstHarvest) {
        	refreshUrlList(); // read the url list from config file, in case it was modified
        }
        
        try {
        	pingUrls();
        } catch(Throwable e) {
        	e.printStackTrace();
        }
        reportMetrics(); // results
        reportHttpStatusCodes(); // httpStatusCodes
        reportSummaryMetrics();
        reportUnresponsiveUrls();
        firstHarvest = false;
        logger.info("-----------------------------------------------------------------------------");
    }

    
    
    @SuppressWarnings("unchecked")
	private int refreshUrlList() { // read the url list from config file, in case it was modified
    	
    	JSONArray json = null;
    	// get the configuration file
    	String path = Config.getConfigDirectory() + File.separatorChar + AGENT_CONFIG_FILE;
    	
    	File file = new File(path);
    	if (!file.exists()) {
    		System.err.println("Cannot find config file " + path);
    		return -1;
    	}
    	
    	long now = new Date().getTime();
    	logger.debug("refreshUrlList: Now: " + now + " -- " + AGENT_CONFIG_FILE + " timestamp: " + file.lastModified());
    	if (now - file.lastModified() > 60000) { // file was not updated since the last poll cycle
    		return 0;
    	}
    	
		// readJSONFile
    	Object parseResult = null;
		try {
	   	    FileReader reader = new FileReader(file);        
		    JSONParser parser = new JSONParser();
		    
		    try {
		    	parseResult = parser.parse(reader);
			} catch (ParseException e) {
				System.err.println("Error parsing config file " + file.getAbsolutePath());
			}
		} catch(IOException ioEx) {
			System.err.println("Error reading config file " + file.getAbsolutePath());
		}
		
		try {
			json = (JSONArray) ((JSONObject) parseResult).get("agents");
		} catch(ClassCastException e) {
			System.err.println("Parsed JSON data cannot be cast to JSONArray");
			e.printStackTrace();
		}

		// read json array and find the array item corresponding to current instance
		for (int i = 0; i < json.size(); i++) {
        	JSONObject obj = (JSONObject) json.get(i);
        	Map<String, Object> map = obj;
        	if (map.get("name").equals(this.name)) {
	        	logger.debug("name: " + map.get("name") + "  -  " + map.get("host"));
	        	

				Iterator<Object> iterator = ((JSONArray) map.get("urls")).iterator();
				urls.clear();
				
				while (iterator.hasNext()) {
					Map<String, String> urlItem = (Map<String, String>)iterator.next();
					urls.put(urlItem.get("name"), urlItem.get("url"));
					logger.debug("refreshUrlList: added: " + urlItem.get("name") + ": " + urlItem.get("url") + " to urlList");
				}
				logger.debug("refreshUrlList: after clearing urlList: " + urls.toString());
				
				// if this url was removed from the json file, then remove the corresponding urlMetricData object from results as well
				logger.debug("before deteling UrlMetricData objects from results: " + results.toString());
				Iterator<Map.Entry<String,UrlMetricData>> iter = results.entrySet().iterator();
				while (iter.hasNext()) {
				    Map.Entry<String,UrlMetricData> entry = iter.next();
				    if(!urls.containsKey(entry.getKey())){
				        iter.remove();
				        logger.debug("refreshUrlList: deleted: " + entry.getKey() + " from results");
				    }
				}
				logger.debug("after deteling UrlMetricData objects from results: " + results.toString());
				break; // get out of the for loop. here we only care about the data for the current instance
        	}
		}
		return 0;
    }
    
    

    @SuppressWarnings("unchecked")
	public void pingUrls() throws Throwable {
	
		//set the number of worker threads for the Executor
	    ExecutorService service = Executors.newFixedThreadPool(threadPoolSize);
	    List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();

    	for (String urlName : urls.keySet()) {
			UrlMetricData umd = results.get(urlName);
			if (umd == null) {
				umd = new UrlMetricData(urlName, urls.get(urlName), failStatusTime, failStatusResetTime);
				results.put(umd.getName(), umd);
			}			
	       logger.info("starting ping thread for \"" + umd.getName() + "\" -- url: " + umd.getUrl());
	       Future<?> future = service.submit(new Ping(umd, httpStatusCodes, summaryMetrics, unresponsiveUrlList, timeout));
	       futures.add((Future<Runnable>) future);	       
	    }
	    //shut down the executor service so that this thread can exit
    	try {    		
    		service.shutdown();
    	    // Wait for existing tasks to terminate
    	    logger.debug("waiting for all jobs to complete...");
    	    if (!service.awaitTermination(20, TimeUnit.SECONDS)) {
    	    	service.shutdownNow(); // Cancel currently executing tasks
    	        // Wait a while for tasks to respond to being cancelled
    	        service.awaitTermination(10, TimeUnit.SECONDS);
    	     }
    	} catch (InterruptedException ie) {
    		service.shutdownNow();
    	}
	    logger.debug("continuing...");
    }

    
    /**
     * This method handles the reporting of url metrics to New Relic
     * 
     */
    public void reportMetrics() { // HashMap<String, UrlMetricData> results) {
        //int count = 0;
        String metricBranch = null;
        int urlDownCount = 0;
        long urlExecutionTime = 0;

        logger.debug("Collected " + results.size() + " Ping metrics. " + getAgentInfo());
        logger.debug(results.toString());

        for(String urlName : results.keySet()) {
        	//count++;
        	UrlMetricData umd = results.get(urlName);
    		int responseStatus = umd.getUrlResponseStatus();
        	metricBranch = (responseStatus == 1) ? "Success" : "Fail";
        	reportMetric("UrlPing/PingResults/" + metricBranch + SEPARATOR + umd.getName(), "count", 1); //responseStatus);
        	logger.debug("Metric " + " " + "UrlPing/PingResults/" + metricBranch + SEPARATOR + umd.getName()); // + "(count)=" + responseStatus);

        	urlExecutionTime = umd.getExecutionTime();
        	if (urlExecutionTime > 0) {
        		reportMetric("UrlPing/ExecutionTimes/" + metricBranch + SEPARATOR + umd.getName(), "ms", urlExecutionTime);
        		logger.debug("Metric " + " " + "UrlPing/ExecutionTimes/" + metricBranch + SEPARATOR + umd.getName() + " (ms)=" + urlExecutionTime);
        	}

        	if (umd.isUrlDown()) {
        		urlDownCount++;
            	reportMetric("UrlPing/UnresponsiveUrls/" + umd.getName(), "count", 1); //responseStatus);
            	logger.debug("Metric " + " " + "UrlPing/UnresponsiveUrls/" + umd.getName()); // + "(count)=" + responseStatus);
        	}
       	
        	// additionally send failed pings separately for additional dashboard reporting
        	if (responseStatus > 1) {
        		// add data for reporting the number of ping failures for each url
        		logger.info("url: " + umd.getName() + "  failed to ping...");
        		reportMetric("UrlPing/FailedPingCounts" + SEPARATOR + umd.getName(), "count", umd.getFailureCount());
            	logger.debug("Metric " + " " + "UrlPing/FailedPingCounts/" + umd.getName() + "(count)=" + umd.getFailureCount());

	        	switch (responseStatus) {
	        	case 2: 
	        		metricBranch = "HttpErrors/" + umd.getHttpStatusCode();
	        		break;
	        	case 3:
	        		metricBranch = "SocketTimeoutException";
	        		break;
	        		
	        	case 4:
	        		metricBranch = "UnknownHostException";
	        		break;
	        		
	        	case 5:
	        		metricBranch = "HttpException";
	        		break;
	        		
	        	case 6:
	        		metricBranch = "IOException";
	        		break;
	        		
	        	case 7:
	        		metricBranch = "NoSuchAlgorithmException";
	        		break;
	        		
	        	case 8:
	        		metricBranch = "KeyManagementException";
	        		break;
	        		
	        	default:
	        		// do nothing
	       			break;
	        	}
	        	reportMetric("UrlPing/FailedPings/" + metricBranch + SEPARATOR + umd.getName(), "count",responseStatus);
	        	logger.debug("Metric " + " " + "UrlPing/FailedPings/" + metricBranch + SEPARATOR + umd.getName() + "(code)=" + responseStatus);        	}
        }
        //TODO - is it necessary to report count of all urls (successful + failed)
        
        // if any url has been unresponsive for at least "<failStatusTime>" minutes (read from plugin config file)
        // then generate *special summary metric* to indicate the need for an alert
        if (urlDownCount > 0) {
        	reportMetric("UrlPing/DerivedMetrics/UnresponsiveUrls", "count", urlDownCount);
        	logger.debug("Metric " + " " + "UrlPing/DerivedMetrics/UnresponsiveUrls" + " (urls)=" + urlDownCount);
        }
    }

    
    /**
     * This method handles the reporting of http status codes to New Relic
     * 
     */
    public void reportHttpStatusCodes() {
        //int count = 0;
        logger.debug("Collected " + httpStatusCodes.size() + " Http Status Code Types. " + getAgentInfo());
        logger.debug(httpStatusCodes.toString());

        for(Integer httpStatusCode : httpStatusCodes.keySet()) {
        	//count++;
        	Long statusCount = httpStatusCodes.get(httpStatusCode);
        	reportMetric("UrlPing/HttpStatusCodes/" + httpStatusCode, "count", statusCount);
        	logger.debug("Metric " + " " + "UrlPing/HttpStatusCodes/" + httpStatusCode + " (count)=" + statusCount);
        }
    	// TODO - 
    }
    
    

    public void reportSummaryMetrics() {
    	// SuccessCount, FailedCount, exceptions (one per exception type), Derived Metric: TotalExceptions
        //int count = 0;
        String metricBranch = null;
        logger.debug("Collected " + summaryMetrics.size() + " Summary Metrics. " + getAgentInfo());
        logger.debug(summaryMetrics.toString());

        for(String summaryMetric : summaryMetrics.keySet()) {
        	//count++;
        	Long summaryMetricValue = summaryMetrics.get(summaryMetric);
        	metricBranch = (summaryMetric.equals("TotalExceptions")) ? "DerivedMetrics" : "SummaryMetrics";
        	reportMetric("UrlPing/" + metricBranch + SEPARATOR + summaryMetric, "count", summaryMetricValue);
        	logger.debug("Metric " + " " + "UrlPing/" + metricBranch + SEPARATOR + summaryMetric + "(count)=" + summaryMetricValue);
        }
    	// TODO - 
    }
    

    public void reportUnresponsiveUrls() {
    	// report urls that have been unresponsive for *failStatusTime* minutes
        //int count = 0;
        logger.debug("Collected " + unresponsiveUrlList.size() + " Summary Metrics. " + getAgentInfo());
        logger.debug(unresponsiveUrlList.toString());

        for(String unresponsiveUrl : unresponsiveUrlList.keySet()) {
        	//count++;
        	Long minutesUnresponsive = unresponsiveUrlList.get(unresponsiveUrl);
        	reportMetric("UrlPing/UnresponsiveUrls/" + unresponsiveUrl, "min", minutesUnresponsive);
        	logger.debug("Metric " + " " + "UrlPing/UnresponsiveUrls/" + unresponsiveUrl + "(min)=" + minutesUnresponsive);
        }
    	// TODO - 
    }

    
    @SuppressWarnings("unused")
	private void initializeMetrics() {
        reportMetric("UrlPing/FailedPings/HttpErrors/https://citibank.com/X", "count",0);
        reportMetric("UrlPing/FailedPings/SocketTimeoutException/https://citibank.com/X", "count",0);
        reportMetric("UrlPing/FailedPings/UnknownHostException/https://citibank.com/X", "count",0);
        reportMetric("UrlPing/FailedPings/HttpException/https://citibank.com/X", "count",0);
        reportMetric("UrlPing/FailedPings/IOException/https://citibank.com/X", "count",0);
        reportMetric("UrlPing/DerivedMetrics/UnresponsiveUrls", "count", 0);
        reportMetric("UrlPing/DerivedMetrics/TotalExceptions", "count", 0);
    }
    
    private String getAgentInfo() {
        return "Agent Name: " + name + ". Agent Version: " + version;
    }

    /**
     * Return the human readable name for this agent.
     * 
     * @return String
     */
    //@Override
    public String getComponentHumanLabel() {
        return name;
    }


	@Override
	public String getAgentName() {
		// TODO Auto-generated method stub
		return this.name;
	}

}
