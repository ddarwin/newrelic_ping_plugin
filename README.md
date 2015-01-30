URL Ping Plugin
==========================================================
- - -
The URL Ping plugin allows users to ping multiple URL's and keep track of the health of each URL.


##Prerequisites

*    A New Relic account. If you are not already a New Relic user, you can signup for a free account at http://newrelic.com
*    A New Relic valid license key. You can obtain the license key fromn your New Relic account under 'Account Settings'
*    Obtain the New Relic Ping plugin
*    A configured Java Runtime (JRE) environment Version 1.7+
*    Network access to New Relic


##Additional Plugin Details:

*	This plugin pings URL's read from config/ping.instance.json once a minute, and reports the status of each URL to New Relic.


##Installation

Linux example:

*    $ mkdir /path/to/newrelic-plugin
*    $ cd /path/to/newrelic-plugin
*    $ tar xfz /path/to/newrelic_ping_plugin-\*.tar.gz
   


## Configure the agent environment
New Relic plugin runs an agent process to collect and report metrics to New Relic. In order for that to happen you need to configure your New Relic license and plugin-specific properties. Additionally you can set the logging properties.


### Configure your New Relic license
Specify your license key in a file by the name 'newrelic.json' in the 'config' directory.
Your license key can be obtained under "Account Settings" at https://rpm.newrelic.com. See https://newrelic.com/docs/subscriptions/license-key for more details.

Linux example:

*    $ cp config/newrelic_template.json config/newrelic.json
*    Edit config/newrelic.json and paste in your license key
*    Change logging ptoperties as necessary

### Configure plugin properties
Running the plugin agent requires a JSON configuration file defining the plugin-specific properties in the 'config' directory. An example file is provided in the config directory.

*    $ cp config/plugin_template.json config/plugin.json
*    Specify the necessary property values in config/plugin.json. 
*    Change the values for all the properties that apply to your server and environment. 

**Note:** The value of the "name" field will appear in the New Relic user interface for the url pinger instance (i.e. "My Pinger"). 

    [
      {
		"name" 					: "My Pinger",
		"host" 					: "localhost",
		"useproxy" 				: "false",
		"proxyprotocol" 		: "https",
		"proxyhost" 			: "",
		"proxyport" 			: "",
		"proxyuser" 			: "",
		"proxypassword" 		: "",
		"nonproxyhosts" 		: "",
		"timeout" 				: "3000",
		"failstatustime" 		: "5",
		"failstatusresettime" 	: "3",
		"threadpoolsize" 		: "20",
		"urls" 					: [
			{
				"name" 	: "localhost",
				"url" 	: "http://localhost/"
			},
			{
				"name" 	: "www.google.com",
				"url" 	: "http://www.google.com/"
			},
			{
				"name" 	: "local tomcat(ssl)",
				"url" 	: "https://localhost:8443"
			},
			{
				"name" 	: "google(ssl)",
				"url" 	: "https://www.google.com/"
			}
		]
       }
    ]

  * mandatory properties	: name, host, useproxy, timeout, failstatustime, failstatusresettime, threadpoolsize, urls

  * name					- A friendly name that will show up in the New Relic Dashboard.
  * host 					- Hostname of the server being monitored.
  * useproxy 				- true/false - whether or not to use proxy settings 
  							- if useproxy is set to false, none of the oyher proxy setting properties below are read
  							- if useproxy is set to true, all proxy setting properties must be present, even if empty string ""
  * proxyprotocol 			- http/https
  * proxyhost 				- proxy host address (ignored if port not present)
  * proxyport 				- proxy port number
  * proxyuser 				- proxy user for specified protocol (ignored if proxy password not present)
  * proxypassword 			- proxy password for specified protocol
  * nonproxyhosts 			- comma separated list of hosts which should bypass the proxy
  * timeout 				- connection timeout (for HttpURLConnection & HttpsURLConnection)
  * failstatustime 			- number of minutes to consider a url ping a failed
  * failstatusresettime 	- number of minutes for a failed url ping to respond before considering that it's responsive
  * threadpoolsize 			- maximum number of cocurrent url pings 
  * urls 					- a json array of url friendly names and url's - note this is url "name". different than plugin "name"

**Note:** Specify the above set of properties for each plugin instance. You will have to follow the syntax (embed the properties for each instance of the plugin in a pair of curley braces separated by a comma).

**Note:** If you would like to execute multiple instances of the plugin, copy the block of JSON properties (separated by comma), and change the values accordingly. Example:

    [
      {
		"name" 					: "My Pinger",
		"host" 					: "localhost",
		"useproxy" 				: "false",
		"proxyprotocol" 		: "https",
		"proxyhost" 			: "",
		"proxyport" 			: "",
		"proxyuser" 			: "",
		"proxypassword" 		: "",
		"nonproxyhosts" 		: "",
		"timeout" 				: "3000",
		"failstatustime" 		: "5",
		"failstatusresettime" 	: "3",
		"threadpoolsize" 		: "20",
		"urls" 					: [
			{
				"name" 	: "localhost",
				"url" 	: "http://localhost/"
			},
			{
				"name" 	: "www.google.com",
				"url" 	: "http://www.google.com/"
			}
		]
      },
      {
		"name" 					: "My SSL Pinger",
		"host" 					: "localhost",
		"useproxy" 				: "false",
		"proxyprotocol" 		: "https",
		"proxyhost" 			: "",
		"proxyport" 			: "",
		"proxyuser" 			: "",
		"proxypassword" 		: "",
		"nonproxyhosts" 		: "",
		"timeout" 				: "3000",
		"failstatustime" 		: "5",
		"failstatusresettime" 	: "3",
		"threadpoolsize" 		: "20",
		"urls" 					: [
			{
				"name" 	: "local tomcat(ssl)",
				"url" 	: "https://localhost:8443"
			},
			{
				"name" 	: "google(ssl)",
				"url" 	: "https://www.google.com/"
			}
		]
      }
    ]


## Running the agent
To run the plugin from the command line: 

*    `$ java -jar newrelic_ping_plugin-*.jar`

If your host needs a proxy server to access the Internet, you can specify a proxy server & port: 

*    `$ java -Dhttps.proxyHost="PROXY_HOST" -Dhttps.proxyPort="PROXY_PORT" -jar newrelic_ping_plugin-*.jar`

To run the plugin from the command line and detach the process so it will run in the background:

*    `$ nohup java -jar newrelic_ping_plugin-*.jar &`

**Note:** At present there are no [init.d](http://en.wikipedia.org/wiki/Init) scripts to start this plugin at system startup. You can create your own script, or use one of the services below to manage the process and keep it running:

*    [Upstart] "(http://upstart.ubuntu.com/)"
*    [Systemd] "(http://www.freedesktop.org/wiki/Software/systemd/)"
*    [Runit] "(http://smarden.org/runit/)"
*    [Monit] "(http://mmonit.com/monit/)"
