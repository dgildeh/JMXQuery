package com.outlyer.jmx.jmxquery;

import com.outlyer.jmx.jmxquery.tools.JMXTools;
import com.outlyer.jmx.jmxquery.tools.LocalJMXConnection;
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
    JMXMetric listQuery = null;
    boolean outputJSON = false;
    boolean includeJVMMetrics = false;
    
    // Constants
    public static final String JVM_LIST = "jvms";
    public static final String MBEAN_LIST = "mbeans";
    

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
                       
        // Initialise
        JMXQuery query = new JMXQuery();
        query.parse(args);
        
        // List JVMs if -list jvms provided
        if (query.list && query.listType.equals(JMXQuery.JVM_LIST)) {
          
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
            
        } else {
            
            // Initialise JMX Connection
            
            try {
                if (query.processName != null) {
                    query.connector = new JMXConnector(query.processName);
                } else {
                    query.connector = new JMXConnector(query.url, query.username, query.password);
                }
            } catch (IOException ioe) {
                System.out.println("Error connecting to JMX: " + ioe.getMessage());
                System.exit(2);
            }
                            
            // Handle list commands
            if (query.list) {
                if (query.listType.equals(JMXQuery.MBEAN_LIST)) {

                    ArrayList<JMXMetric> treeMetrics = query.connector.listMetrics(query.listQuery);
                    int count = 0;

                    if (query.outputJSON) {
                        System.out.println("[");
                        for (JMXMetric metric : treeMetrics) {
                            System.out.println(metric.toJSON());
                        } 
                        System.out.println("]");
                    } else {
                        System.out.println("Listing JMX MBean Tree for query " + query.listQuery.toString() + ": \n");
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
            ArrayList<JMXMetric> outputMetrics = query.connector.getMetrics(query.metrics);

            if (query.outputJSON) {
                System.out.println("[");
                int count = 0;
                for (JMXMetric metric : outputMetrics) {
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
                    System.out.println(metric.toString());
                }
            }

            // Disconnect from JMX Cleanly
            query.connector.disconnect();
        }
    }

    /**
     * Get key JVM stats. Utility method for quickly grabbing key java metrics
     * and also for testing
     */
    private void includeJVMStats() {
        
        // Class Loading
        metrics.add(new JMXMetric("jvm.classloading.loaded_class_count", "java.lang:type=ClassLoading", "LoadedClassCount", null));
        metrics.add(new JMXMetric("jvm.classloading.unloaded_class_count", "java.lang:type=ClassLoading", "UnloadedClassCount", null));
        metrics.add(new JMXMetric("jvm.classloading.total_loaded_class_count", "java.lang:type=ClassLoading", "TotalLoadedClassCount", null));
        
        // Garbage Collection
        metrics.add(new JMXMetric("jvm.gc.[name].collectiontime", "java.lang:type=GarbageCollector,*", "CollectionTime", null));
        metrics.add(new JMXMetric("jvm.gc.[name].collectioncount", "java.lang:type=GarbageCollector,*", "CollectionCount", null));
        
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
        metrics.add(new JMXMetric("jvm.os.open_fd_count", "java.lang:type=OperatingSystem", "OpenFileDescriptorCount", null));
        metrics.add(new JMXMetric("jvm.os.max_fd_count", "java.lang:type=OperatingSystem", "MaxFileDescriptorCount", null));
        metrics.add(new JMXMetric("jvm.os.committed_vm_size", "java.lang:type=OperatingSystem", "CommittedVirtualMemorySize", null));
        metrics.add(new JMXMetric("jvm.os.total_swap_size", "java.lang:type=OperatingSystem", "TotalSwapSpaceSize", null));
        metrics.add(new JMXMetric("jvm.os.free_swap_size", "java.lang:type=OperatingSystem", "FreeSwapSpaceSize", null));
        metrics.add(new JMXMetric("jvm.os.process_cpu_time", "java.lang:type=OperatingSystem", "ProcessCpuTime", null));
        metrics.add(new JMXMetric("jvm.os.free_phys_mem_size", "java.lang:type=OperatingSystem", "FreePhysicalMemorySize", null));
        metrics.add(new JMXMetric("jvm.os.total_phys_mem_size", "java.lang:type=OperatingSystem", "TotalPhysicalMemorySize", null));
        metrics.add(new JMXMetric("jvm.os.system_cpu_load", "java.lang:type=OperatingSystem", "SystemCpuLoad", null));
        metrics.add(new JMXMetric("jvm.os.process_cpu_load", "java.lang:type=OperatingSystem", "ProcessCpuLoad", null));
        metrics.add(new JMXMetric("jvm.os.system_load_avg", "java.lang:type=OperatingSystem", "SystemLoadAverage", null));
        
        // Runtime
        metrics.add(new JMXMetric("jvm.runtime.uptime", "java.lang:type=Runtime", "Uptime", null));

        // Threading   
        metrics.add(new JMXMetric("jvm.threading.thread_count", "java.lang:type=Threading", "ThreadCount", null));
        metrics.add(new JMXMetric("jvm.threading.peak_thread_count", "java.lang:type=Threading", "PeakThreadCount", null));
        metrics.add(new JMXMetric("jvm.threading.daemon_thread_count", "java.lang:type=Threading", "DaemonThreadCount", null));
        metrics.add(new JMXMetric("jvm.threading.total_started_thread_count", "java.lang:type=Threading", "TotalStartedThreadCount", null));

        // Memory Pools
        metrics.add(new JMXMetric("jvm.memory.[name].committed", "java.lang:type=MemoryPool,*", "Usage", "committed"));
        metrics.add(new JMXMetric("jvm.memory.[name].init", "java.lang:type=MemoryPool,*", "Usage", "init"));
        metrics.add(new JMXMetric("jvm.memory.[name].max", "java.lang:type=MemoryPool,*", "Usage", "max"));
        metrics.add(new JMXMetric("jvm.memory.[name].used", "java.lang:type=MemoryPool,*", "Usage", "used"));
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
                    System.exit(0);
                    
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
                    
                    if (listType.equals(JMXQuery.MBEAN_LIST)) {
                        
                        String[] query = args[++i].split("/");
                        String mbean = query[0];
                        String attribute = null;
                        String key = null;
                        if (query.length > 1) {
                            attribute = query[1];
                        }
                        if (query.length > 2) {
                            key = query[2];
                        }
                        
                        listQuery = new JMXMetric(null, mbean, attribute, key);
                    }

                } else if (option.equals("-json")) {
                    outputJSON = true;
                } else if (option.equals("-incjvm")) {
                    includeJVMMetrics = true;
                }
                
                // Check that required parameters are given
                if (url == null && processName == null && !listType.equals(JMXQuery.JVM_LIST)) {
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