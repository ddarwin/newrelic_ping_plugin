package com.newrelic.plugins.ping.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

/**
 * This class produces the necessary Agents to perform gathering and reporting
 * metrics for the Ping plugin
 * 
 */
public class PingAgentFactory extends AgentFactory {
    /**
     * Construct an Agent Factory based on the default properties file
     */
    public PingAgentFactory() {
        super(); //(PingAgent.AGENT_CONFIG_FILE);
    }

    /**
     * Configure an agent based on an entry in the ping json file. There may
     * be multiple agents per Plugin
     * 
     */
    @Override
    public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
        String name = (String) properties.get("name");
        // String host = (String) properties.get("host");
        
        if (((String)properties.get("useproxy")).toLowerCase().equals("true")) {
            Properties systemSettings = System.getProperties();
        	String proxyProtocol = (String) properties.get("proxyprotocol"); // supports http & https
        	if (!proxyProtocol.isEmpty()) {
	        	String proxyHost = (String) properties.get("proxyhost");
	        	String proxyPort = (String) properties.get("proxyport");
	        	if (!proxyHost.isEmpty() && !proxyPort.isEmpty() && Integer.parseInt(proxyPort)>0) {
		            systemSettings.put(proxyProtocol + ".proxyHost", proxyHost);
		            systemSettings.put(proxyProtocol + ".proxyPort", proxyPort);
		            String nonProxyHosts = (String) properties.get("nonproxyhosts");
		            if (!nonProxyHosts.isEmpty()) {
		            	systemSettings.put("http.nonProxyHosts", nonProxyHosts);
		            }
		            String proxyUser = (String) properties.get("proxyuser");
		            if (!proxyUser.isEmpty()) {
		            	String proxyPassword = (String) properties.get("proxypassword");
		            	if (!proxyPassword.isEmpty()) {
		            		systemSettings.put(proxyProtocol + ".proxyUser", proxyUser);
		            		systemSettings.put(proxyProtocol + ".proxyPassword", proxyPassword);
		            	}
		            }
	        	}
        	}
        }
        
        int timeout = Integer.parseInt((String) properties.get("timeout")); // url ping timeout
        int failstatustime = Integer.parseInt((String) properties.get("failstatustime")); // time (min) to declare url as unrespnsive
        int failstatusresettime = Integer.parseInt((String) properties.get("failstatusresettime")); // time (min) to declare url as responsive again after being unresponsive for some time
        int threadpoolsize = Integer.parseInt((String) properties.get("threadpoolsize"));
        
		@SuppressWarnings("unchecked")
		Iterator<Object> iterator = ((JSONArray) properties.get("urls")).iterator();
		Map<String, String> urls = new HashMap<String, String>();
		
		while (iterator.hasNext()) {
			@SuppressWarnings("unchecked")
			Map<String, String> map = (Map<String, String>)iterator.next();
			urls.put(map.get("name"), map.get("url"));
		}

        /**
         * Use pre-defined defaults to simplify configuration
         */
//        if (host == null || "".equals(host))
//            host = PingAgent.AGENT_DEFAULT_HOST;
        if (timeout<0) timeout = 0;
        if (threadpoolsize<0) threadpoolsize = 5; // default threadpoolsize

        return new PingAgent(name, timeout, failstatustime, failstatusresettime, threadpoolsize, urls);
    }
}
