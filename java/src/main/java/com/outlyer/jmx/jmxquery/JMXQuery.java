package com.outlyer.jmx.jmxquery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import javax.management.MalformedObjectNameException;
// add the support for the time of day formatting etc
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.lang.System;
import java.lang.Runtime;



/**
 *
 * JMXQuery is used for local or remote request of JMX attributes
 *
 * @author David Gildeh (www.outlyer.com)
 * updates by Colin Paice to support loop, and date time support
 *
 */
public class JMXQuery {

    private JMXConnector connector;
    private final ArrayList<JMXMetric> metrics = new ArrayList<JMXMetric>();
    
    // Command Line Parameters
    String url = null;
    String username = null;
    String password = null;
    boolean outputJSON = false;
    long  loopCount = 0;
    long  every = 0;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        SimpleDateFormat sdfdate = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat sdftime = new SimpleDateFormat("HH:mm:ss.SSS");                
        Timestamp timestampStart = new Timestamp(System.currentTimeMillis());
        Timestamp timestampCollectTime = timestampStart;
        Timestamp timestampEndPreviousCollectTime = timestampCollectTime;
        String dateNow ;
        String timeNow ;
        long secondsFromStart = 0;

        String arrayPrefix = "";
        String passedStrings[];
        String header = "";
      
        // Initialise
        JMXQuery query = new JMXQuery();
        query.parse(args);
        
        // if we have a loop, and we are doing JSON output trap CTRL-C to write a closing ]
        if (query.outputJSON)
        	Runtime.getRuntime().addShutdownHook(
        		new Thread() {
        		  public void run() { System.out.println("]");}
        	       }
        		);
                           
        // Initialise JMX Connection    
        try {
            query.connector = new JMXConnector(query.url, query.username, query.password);
        } catch (IOException ioe) {
            if (query.outputJSON) {
                System.out.println("{ \"error\": \"connection-error\", \"message\":\"" + ioe.getMessage() + "\"}");
                System.exit(2);
            } else {
                System.out.println("Error connecting to JMX endpoint: " + ioe.getMessage());
                System.exit(2);
            }
        }
        
 


   if (query.outputJSON)  System.out.println("[");
   // check we have sensible values for loop time and loop count
   if (query.every == 0 ) query.every = 10000; // millisecond value 
   if (query.loopCount == 0 ) query.loopCount = 1;
   for (long iLoop = 0;; iLoop ++)
   {
	  //  check to see if we have been asked to loop
      if (iLoop >= query.loopCount)
          break;
      if (iLoop > 0) 
      {
    	// wait for the user specified period
        // try to keep sleep time as close to specified time by excluding the time
        // getting the data
    	// if it takes 2 second to collect the data, and the interval between collect data is 10 seconds
    	// then we should wait for 10 - 2 seconds
    	// if the time to collect is longer than specified interval just use the sleep time as specified
    	// by the end user
         
        long sleepTime = query.every - (timestampEndPreviousCollectTime.getTime()-timestampCollectTime.getTime());
        if ( sleepTime < 1) sleepTime = query.every;
         Thread.sleep(sleepTime);
      }
      timestampCollectTime = new Timestamp(System.currentTimeMillis());
      dateNow  = sdfdate.format(timestampCollectTime);
      timeNow  = sdftime.format(timestampCollectTime);
      // convert time delta from milliseconds to seconds
      secondsFromStart = (timestampCollectTime.getTime() - timestampStart.getTime())/1000;

      if (query.loopCount > 0)
      {
    	  passedStrings = new String[]{ " \"Date\" : \"" + dateNow + " \"",
    			  " \"Time\" : \"" + timeNow + " \"",
    			  " \"secondsFromStart\" : " + secondsFromStart,
    			  " \"loop\" : " + iLoop
    	  };
    	  header = "Date:"+dateNow+ " Time:"+ timeNow+" Seconds from start:"+secondsFromStart +" loop:" +iLoop;
      }
      else
      {
    	  passedStrings = new String[] {}; // empty, no additional data appended
      }
      // Process Query
      try {
            ArrayList<JMXMetric> outputMetrics = query.connector.getMetrics(query.metrics);  

            if (query.outputJSON) {
                System.out.println(arrayPrefix + "["); // either '' or ,
                arrayPrefix = ",";   // separate each array with a ,
                int count = 0;

                for (JMXMetric metric : outputMetrics) {
                    metric.replaceTokens();
                    if (count > 0) {                        
                        System.out.print(", \n" + metric.toJSON(passedStrings));
                    } else {
                        count++;
                        System.out.print(metric.toJSON(passedStrings));
                    }
                }
                System.out.println("]");
                System.out.flush();  // so it gets passed on to the next stage
            } else {
            	if (query.loopCount > 0)
                    System.out.println(header);
                for (JMXMetric metric : outputMetrics) {
                    metric.replaceTokens();
                    System.out.println(metric.toString());
                }
                System.out.println("=====================");
                System.out.println("Total Metrics Found: " + String.valueOf(outputMetrics.size()));
            }
        } catch (IOException ioe) {
            if (query.outputJSON) {
                System.out.println("{ \"error\": \"query-connection-error\", \"message\":\"" + ioe.getMessage() + "\"}");
                System.exit(2);
            } else {
                System.out.println("There was an IO Error running the query '" + query.metrics.toString() + "': " + ioe.getMessage());
                System.exit(2);
            }
        } catch (MalformedObjectNameException me) {
            if (query.outputJSON) {
                System.out.println("{ \"error\": \"bad-query\", \"message\":\"" + me.getMessage() + "\"}");
                System.exit(2);
            } else {
                System.out.println("The query '" + query.metrics.toString() + "' is invalid: " + me.getMessage());
                System.exit(2);
            }
        } catch (Exception e) {
            if (query.outputJSON) {
                System.out.println("{ \"error\": \"general-exception\", \"message\":\"" + e.getMessage() + "\"}");
                System.exit(2);
            } else {
                System.out.println("An exception was thrown while running the query '" + query.metrics.toString() + "': " + e.getMessage());
                System.out.println(Arrays.toString(e.getStackTrace()));
                System.exit(2);
            }
        } // end of try
        timestampEndPreviousCollectTime = new Timestamp(System.currentTimeMillis());

     }  // end of for iloop      
     //  Do not put out trailing ] as the shutdown exit does it
     //   if (query.outputJSON) 
     //                System.out.println("]");

    // Disconnect from JMX Cleanly
        query.connector.disconnect(); 
    }

    /**
     * Get key JVM stats. Utility method for quickly grabbing key java metrics
     * and also for testing
     */
    private void includeJVMStats() {
        
        // Class Loading
        metrics.add(new JMXMetric("java.lang:type=ClassLoading", "LoadedClassCount", null));
        metrics.add(new JMXMetric("java.lang:type=ClassLoading", "UnloadedClassCount", null));
        metrics.add(new JMXMetric("java.lang:type=ClassLoading", "TotalLoadedClassCount", null));
        
        // Garbage Collection
        metrics.add(new JMXMetric("java.lang:type=GarbageCollector,*", "CollectionTime", null));
        metrics.add(new JMXMetric("java.lang:type=GarbageCollector,*", "CollectionCount", null));
        
        // Memory            
        metrics.add(new JMXMetric("java.lang:type=Memory", "HeapMemoryUsage", "committed"));
        metrics.add(new JMXMetric("java.lang:type=Memory", "HeapMemoryUsage", "init"));
        metrics.add(new JMXMetric("java.lang:type=Memory", "HeapMemoryUsage", "max"));
        metrics.add(new JMXMetric("java.lang:type=Memory", "HeapMemoryUsage", "used"));
        metrics.add(new JMXMetric("java.lang:type=Memory", "NonHeapMemoryUsage", "committed"));
        metrics.add(new JMXMetric("java.lang:type=Memory", "NonHeapMemoryUsage", "init"));
        metrics.add(new JMXMetric("java.lang:type=Memory", "NonHeapMemoryUsage", "max"));
        metrics.add(new JMXMetric("java.lang:type=Memory", "NonHeapMemoryUsage", "used"));

        // Operating System
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "OpenFileDescriptorCount", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "MaxFileDescriptorCount", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "CommittedVirtualMemorySize", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "TotalSwapSpaceSize", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "FreeSwapSpaceSize", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "ProcessCpuTime", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "FreePhysicalMemorySize", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "TotalPhysicalMemorySize", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "SystemCpuLoad", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "ProcessCpuLoad", null));
        metrics.add(new JMXMetric("java.lang:type=OperatingSystem", "SystemLoadAverage", null));
        
        // Runtime
        metrics.add(new JMXMetric("java.lang:type=Runtime", "Uptime", null));

        // Threading   
        metrics.add(new JMXMetric("java.lang:type=Threading", "ThreadCount", null));
        metrics.add(new JMXMetric("java.lang:type=Threading", "PeakThreadCount", null));
        metrics.add(new JMXMetric("java.lang:type=Threading", "DaemonThreadCount", null));
        metrics.add(new JMXMetric("java.lang:type=Threading", "TotalStartedThreadCount", null));

        // Memory Pools
        metrics.add(new JMXMetric("java.lang:type=MemoryPool,*", "Usage", "committed"));
        metrics.add(new JMXMetric("java.lang:type=MemoryPool,*", "Usage", "init"));
        metrics.add(new JMXMetric("java.lang:type=MemoryPool,*", "Usage", "max"));
        metrics.add(new JMXMetric("java.lang:type=MemoryPool,*", "Usage", "used"));
    }
    
    /**
     * Parse runtime argument commands
     *
     * @param args Command line arguments
     * @throws ParseError
     */
    private void parse(String[] args) throws ParseError {

        try {
            for (int i = 0; i < args.length; i++) {
                String option = args[i];
                if (option.equals("-help") || option.equals("-h")) {
                    
                    printHelp(System.out);
                    System.exit(0);
                    
                } else if (option.equals("-url")) {
                    url = args[++i];
                } else if (option.equals("-username") || option.equals("-u")) {
                    username = args[++i];
                } else if (option.equals("-password") || option.equals("-p")) {
                    password = args[++i];
                    // additional variables for looping and duration of loop
                } else if (option.equals("-count") || option.equals("-c")) {
                    loopCount = Long.parseLong(args[++i]);
                } else if (option.equals("-every") || option.equals("-e")) {
                    every = 1000 *Long.parseLong(args[++i]); // in millseconds

                } else if (option.equals("-query") || option.equals("-q")) {
                    
                    // Parse query string to break up string in format:
                    // {mbean}/{attribute}/{key};
                    String[] query = args[++i].split(";");
                    for (String metricQuery : query) {
                        metrics.add(new JMXMetric(metricQuery));
                    }
                
                } else if (option.equals("-json")) {
                    outputJSON = true;
                } else if (option.equals("-incjvm")) {
                    includeJVMStats();
                }
            }
            
            // Check that required parameters are given
            if (url == null && (metrics.size() > 1)) {
                System.out.println("Required options not specified.");
                printHelp(System.out);
                System.exit(0);
            }
        } catch (Exception e) {
            throw new ParseError(e);
        }
    }

    /*
     * Prints Help Text
     */
    private void printHelp(PrintStream out) {
        InputStream is = JMXQuery.class.getClassLoader().getResourceAsStream("com/outlyer/jmx/jmxquery/HELP");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            while (true) {
                String s = reader.readLine();
                if (s == null) {
                    break;
                }
                out.println(s);
            }
        } catch (IOException e) {
            out.println(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                out.println(e);
            }
        }
    }
}
