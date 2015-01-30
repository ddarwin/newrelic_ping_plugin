package com.newrelic.plugins.ping.instance;

import com.newrelic.metrics.publish.Runner;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

/**
 *  This is the main calling class for a New Relic Agent
 *  This class instantiates the agent factory for Ping plugin
 *  The Runner object starts this agent and runs indefinitely
 *  
 */
public class Main {	
    public static void main(String[] args) {
        try {
            Runner runner = new Runner();
            runner.add(new PingAgentFactory());
            runner.setupAndRun();           // Never returns
        } catch (ConfigurationException e) {
            e.printStackTrace();
            System.err.println("Error configuring New Relic Agent");
            System.exit(1);
        }

    }
}
