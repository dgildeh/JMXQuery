package com.outlyer.jmx.jmxquery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import javax.management.MalformedObjectNameException;

/**
 *
 * JMXQuery is used for local or remote request of JMX attributes
 *
 * @author David Gildeh (www.outlyer.com)
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

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
                       
        // Initialise
        JMXQuery query = new JMXQuery();
        query.parse(args);
            
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
        
        // Process Query
        try {
            ArrayList<JMXMetric> outputMetrics = query.connector.getMetrics(query.metrics);  
            if (query.outputJSON) {
                System.out.println("[");
                int count = 0;
                for (JMXMetric metric : outputMetrics) {
                    metric.replaceTokens();
                    if (count > 0) {                        
                        System.out.print(", \n" + metric.toJSON());
                    } else {
                        count++;
                        System.out.print(metric.toJSON());
                    }
                }
                System.out.println("]");
            } else {
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
        }
        
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