package io.dataloop.jmx.jmxquery;

import io.dataloop.jmx.jmxquery.tools.JMXTools;
import io.dataloop.jmx.jmxquery.tools.LocalJMXConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.util.ArrayList;

/**
 *
 * JMXQuery is used for local or remote request of JMX attributes
 *
 * @author David Gildeh (www.dataloop.io)
 *
 */
public class JMXQuery {

    private JMXConnector connector;
    private final ArrayList<JMXMetric> metrics = new ArrayList<JMXMetric>();
    
    // Command Line Parameters
    String url = null;
    String processName = null;
    String username = null;
    String password = null;
    boolean list = false;
    String listType = null;
    boolean outputJSON = false;
    boolean includeJVMMetrics = false;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        // Initialise
        JMXQuery query = new JMXQuery();
        query.parse(args);
        
        // Initialise JMX Connection
        if (query.processName != null) {
            query.connector = new JMXConnector(query.processName);
        } else {
            query.connector = new JMXConnector(query.url, query.username, query.password);
        }
        
        // Handle list commands
        if (query.list) {
            if (query.listType.equals("jvms")) {
                
                ArrayList<LocalJMXConnection> localJVMs = JMXTools.getLocalJVMs();
                
                if (query.outputJSON) {
                    System.out.println("[");
                    for (LocalJMXConnection jvm : localJVMs) {
                        System.out.println(jvm.toJSON());
                    } 
                    System.out.println("]");
                } else {
                    System.out.println("Listing Local JVMs: \n");
                    for (LocalJMXConnection jvm : localJVMs) {
                        System.out.println(jvm.toString());
                    } 
                }
                
            } else if (query.listType.equals("mbeans")) {
                
                ArrayList<JMXMetric> treeMetrics = query.connector.getMBeansTree();
                int count = 0;
                
                if (query.outputJSON) {
                    System.out.println("[");
                    for (JMXMetric metric : treeMetrics) {
                        System.out.println(metric.toJSON());
                    } 
                    System.out.println("]");
                } else {
                    System.out.println("Listing JMX MBean Tree: \n");
                    for (JMXMetric metric : treeMetrics) {
                        System.out.println(metric.toString());
                        count++;
                    } 
                    System.out.println("\n" + count + " Total");
                }
                
            }
        }
        
        // Handle options
        if (query.includeJVMMetrics) {
            query.includeJVMStats();
        }
        
        // Get metrics and print out
        query.connector.fetchValues(query.metrics);
       
        if (query.outputJSON) {
            System.out.println("[");
            int count = 0;
            for (JMXMetric metric : query.metrics) {
                if (count > 0) {
                    System.out.print(", \n" + metric.toJSON());
                } else {
                    count++;
                    System.out.print(metric.toJSON());
                }
                
            }
            System.out.println("]");
        } else {
            for (JMXMetric metric : query.metrics) {
                System.out.println(metric.toString());
            }
        }
    }

    /**
     * Get key JVM stats. Utility method for quickly grabbing key java metrics
     * and also for testing
     */
    private void includeJVMStats() {
        
        // Class Loading
        metrics.add(new JMXMetric("jvm.classloading.loadedclasscount", "java.lang:type=ClassLoading", "LoadedClassCount", null));
        metrics.add(new JMXMetric("jvm.classloading.unloadedclasscount", "java.lang:type=ClassLoading", "UnloadedClassCount", null));
        metrics.add(new JMXMetric("jvm.classloading.totalloadedclasscount", "java.lang:type=ClassLoading", "TotalLoadedClassCount", null));

        // Memory            
        metrics.add(new JMXMetric("jvm.memory.heap.committed", "java.lang:type=Memory", "HeapMemoryUsage", "committed"));
        metrics.add(new JMXMetric("jvm.memory.heap.init", "java.lang:type=Memory", "HeapMemoryUsage", "init"));
        metrics.add(new JMXMetric("jvm.memory.heap.max", "java.lang:type=Memory", "HeapMemoryUsage", "max"));
        metrics.add(new JMXMetric("jvm.memory.heap.used", "java.lang:type=Memory", "HeapMemoryUsage", "used"));
        metrics.add(new JMXMetric("jvm.memory.nonheap.committed", "java.lang:type=Memory", "NonHeapMemoryUsage", "committed"));
        metrics.add(new JMXMetric("jvm.memory.nonheap.init", "java.lang:type=Memory", "NonHeapMemoryUsage", "init"));
        metrics.add(new JMXMetric("jvm.memory.nonheap.max", "java.lang:type=Memory", "NonHeapMemoryUsage", "max"));
        metrics.add(new JMXMetric("jvm.memory.nonheap.used", "java.lang:type=Memory", "NonHeapMemoryUsage", "used"));

        // Operating System
        metrics.add(new JMXMetric("jvm.os.OpenFileDescriptorCount", "java.lang:type=OperatingSystem", "OpenFileDescriptorCount", null));
        metrics.add(new JMXMetric("jvm.os.MaxFileDescriptorCount", "java.lang:type=OperatingSystem", "MaxFileDescriptorCount", null));
        metrics.add(new JMXMetric("jvm.os.CommittedVirtualMemorySize", "java.lang:type=OperatingSystem", "CommittedVirtualMemorySize", null));
        metrics.add(new JMXMetric("jvm.os.TotalSwapSpaceSize", "java.lang:type=OperatingSystem", "TotalSwapSpaceSize", null));
        metrics.add(new JMXMetric("jvm.os.FreeSwapSpaceSize", "java.lang:type=OperatingSystem", "FreeSwapSpaceSize", null));
        metrics.add(new JMXMetric("jvm.os.ProcessCpuTime", "java.lang:type=OperatingSystem", "ProcessCpuTime", null));
        metrics.add(new JMXMetric("jvm.os.FreePhysicalMemorySize", "java.lang:type=OperatingSystem", "FreePhysicalMemorySize", null));
        metrics.add(new JMXMetric("jvm.os.TotalPhysicalMemorySize", "java.lang:type=OperatingSystem", "TotalPhysicalMemorySize", null));
        metrics.add(new JMXMetric("jvm.os.SystemCpuLoad", "java.lang:type=OperatingSystem", "SystemCpuLoad", null));
        metrics.add(new JMXMetric("jvm.os.ProcessCpuLoad", "java.lang:type=OperatingSystem", "ProcessCpuLoad", null));
        metrics.add(new JMXMetric("jvm.os.SystemLoadAverage", "java.lang:type=OperatingSystem", "SystemLoadAverage", null));
        
        // Runtime
        metrics.add(new JMXMetric("jvm.runtime.Uptime", "java.lang:type=Runtime", "Uptime", null));

        // Threading   
        metrics.add(new JMXMetric("jvm.threading.threadcount", "java.lang:type=Threading", "ThreadCount", null));
        metrics.add(new JMXMetric("jvm.threading.peakthreadcount", "java.lang:type=Threading", "PeakThreadCount", null));
        metrics.add(new JMXMetric("jvm.threading.daemonthreadcount", "java.lang:type=Threading", "DaemonThreadCount", null));
        metrics.add(new JMXMetric("jvm.threading.totalstartedthreadcount", "java.lang:type=Threading", "TotalStartedThreadCount", null));
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
                if (option.equals("-help")) {
                    
                    printHelp(System.out);
                    System.exit(3);
                    
                } else if (option.equals("-url")) {
                    url = args[++i];
                } else if (option.equals("-proc")) {
                    processName = args[++i];
                } else if (option.equals("-username")) {
                    username = args[++i];
                } else if (option.equals("-password")) {
                    password = args[++i];
                } else if (option.equals("-metrics")) {
                    
                    // Parse query string to break up string in format:
                    // {metric}={mbean}/{attribute}/{key};
                    String[] query = args[++i].split(";");
                    for (String metricQuery : query) {
                        metrics.add(new JMXMetric(metricQuery));
                    }
                    
                } else if (option.equals("-list")) {
                    
                    list = true;
                    listType = args[++i];

                } else if (option.equals("-json")) {
                    outputJSON = true;
                } else if (option.equals("-incjvm")) {
                    includeJVMMetrics = true;
                }
                
                // Check that required parameters are given
                if (url == null && processName == null) {
                    throw new Exception("Required options not specified");
                }
            }
        } catch (Exception e) {
            throw new ParseError(e);
        }
    }

    /*
     * Prints Help Text
     */
    private void printHelp(PrintStream out) {
        InputStream is = JMXQuery.class.getClassLoader().getResourceAsStream("io/dataloop/jmx/jmxquery/HELP");
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