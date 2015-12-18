package jmxquery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.InvalidKeyException;

import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.AttachNotSupportedException;

/**
 *
 * JMXQuery is used for local or remote request of JMX attributes
 *
 * @author David Gildeh (www.dataloop.io)
 *
 */
public class JMXQuery {

    private String url;
    private JMXConnector connector;
    private MBeanServerConnection connection;
    private String query;
    private String username, password;

    private Object checkData;
    private Object infoData;

    private static final int RETURN_OK = 0; //       The plugin was able to check the service and it appeared to be functioning properly
    private static final int RETURN_WARNING = 1; // The plugin was able to check the service, but it appeared to be above some "warning" threshold or did not appear to be working properly
    private static final int RETURN_CRITICAL = 2; // The plugin detected that either the service was not running or it was above some "critical" threshold
    private static final int RETURN_UNKNOWN = 3; // Invalid command line arguments were supplied to the plugin or low-level failures internal to the plugin (such as unable to fork, or open a tcp socket) that prevent it from performing the specified operation. Higher-level errors (such as name resolution errors, socket timeouts, etc) are outside of the control of plugins and should generally NOT be reported as UNKNOWN states.

    private void connect() throws IOException {
        JMXServiceURL jmxUrl = new JMXServiceURL(url);

        if (username != null) {
            Map<String, String[]> m = new HashMap<String, String[]>();
            m.put(JMXConnector.CREDENTIALS, new String[]{username, password});
            connector = JMXConnectorFactory.connect(jmxUrl, m);
        } else {
            connector = JMXConnectorFactory.connect(jmxUrl);
        }

        connection = connector.getMBeanServerConnection();
    }

    private String getLocalJMXUrls(String process) {

        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        for (VirtualMachineDescriptor desc : vms) {
            VirtualMachine vm;
            try {
                vm = VirtualMachine.attach(desc);
                Properties props = vm.getAgentProperties();
                String connectorAddress = props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
                String name = desc.displayName().split(" ")[0];
                String pid = desc.id();

                // Print out processes if process is null, else return string when process found
                if (process == null) {
                    System.out.append("JVM: " + name + " (" + pid + "): " + connectorAddress + "\n");
                } else if (name.equals(process)) {
                    return connectorAddress;
                }

            } catch (AttachNotSupportedException | IOException e) {
                // Skip
            }
        }

        return null;
    }

    private void disconnect() throws IOException {
        if (connector != null) {
            connector.close();
            connector = null;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException, MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, MalformedObjectNameException, ParseError {

        JMXQuery query = new JMXQuery();

        query.url = query.getLocalJMXUrls("org.netbeans.Main");

        if (query.url == null) {
            System.out.print("Could not find URL");
            System.exit(3);
        }

        query.connect();

        HashMap<String, String> values = query.getJVMStats();
        for (String key : values.keySet()) {
            System.out.println(key + "=" + values.get(key));
        }
    }

    /**
     * Main function for fetching JMX metrics. Will return null if the metric
     * path is not found in MBean tree.
     *
     * @param domain The domain name (i.e. java.lang)
     * @param type The type of mbean
     * @param name (Optional) They name of the mbean is needed. Null if not
     * @param attribute The attribute to read from the mbean
     * @param key (Option) If the attribute is Composite, specify the key of the
     * specific value to read
     * @return The string value of the metric, or null if not found
     */
    private String fetchValue(String domain, String type, String name, String attribute, String key) {

        Object value;
        String strValue;

        try {
            String obName = domain + ":type=" + type;
            if (name != null) {
                obName += ",name=" + name;
            }
            ObjectName objectName = new ObjectName(obName);
            value = connection.getAttribute(objectName, attribute);

        } catch (MalformedObjectNameException | MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException e) {
            // If we can't find the value specified return null
            return null;
        }

        if (value instanceof CompositeData) {
            CompositeData data = (CompositeData) value;
            try {
                if ((!key.equals("")) && (key != null)) {
                    strValue = data.get(key).toString();
                } else {
                    return null;
                }
            } catch (InvalidKeyException e) {
                // Ket doesn't exist so return null
                return null;
            }

        } else {
            strValue = value.toString();
        }

        return strValue;
    }

    /**
     * Get key JVM stats. Utility method for quickly grabbing key java metrics
     * and also for testing
     */
    private HashMap<String, String> getJVMStats() {

        HashMap<String, String> values = new HashMap<String, String>();

        // Class Loading
        values.put("jvm.classloading.loadedclasscount", fetchValue("java.lang", "ClassLoading", null, "LoadedClassCount", null));
        values.put("jvm.classloading.unloadedclasscount", fetchValue("java.lang", "ClassLoading", null, "UnloadedClassCount", null));
        values.put("jvm.classloading.totalloadedclasscount", fetchValue("java.lang", "ClassLoading", null, "TotalLoadedClassCount", null));

        // Memory            
        values.put("jvm.memory.heap.committed", fetchValue("java.lang", "Memory", null, "HeapMemoryUsage", "committed"));
        values.put("jvm.memory.heap.init", fetchValue("java.lang", "Memory", null, "HeapMemoryUsage", "init"));
        values.put("jvm.memory.heap.max", fetchValue("java.lang", "Memory", null, "HeapMemoryUsage", "max"));
        values.put("jvm.memory.heap.used", fetchValue("java.lang", "Memory", null, "HeapMemoryUsage", "used"));
        values.put("jvm.memory.nonheap.committed", fetchValue("java.lang", "Memory", null, "NonHeapMemoryUsage", "committed"));
        values.put("jvm.memory.nonheap.init", fetchValue("java.lang", "Memory", null, "NonHeapMemoryUsage", "init"));
        values.put("jvm.memory.nonheap.max", fetchValue("java.lang", "Memory", null, "NonHeapMemoryUsage", "max"));
        values.put("jvm.memory.nonheap.used", fetchValue("java.lang", "Memory", null, "NonHeapMemoryUsage", "used"));

        // Garbage Collection
        values.put("jvm.gc.collectioncount", fetchValue("java.lang", "GarbageCollector", "PS MarkSweep", "CollectionCount", null));
        values.put("jvm.gc.collectiontime", fetchValue("java.lang", "GarbageCollector", "PS MarkSweep", "CollectionTime", null));

        // Operating System
        values.put("jvm.os.OpenFileDescriptorCount", fetchValue("java.lang", "OperatingSystem", null, "OpenFileDescriptorCount", null));
        values.put("jvm.os.MaxFileDescriptorCount", fetchValue("java.lang", "OperatingSystem", null, "MaxFileDescriptorCount", null));
        values.put("jvm.os.CommittedVirtualMemorySize", fetchValue("java.lang", "OperatingSystem", null, "CommittedVirtualMemorySize", null));
        values.put("jvm.os.TotalSwapSpaceSize", fetchValue("java.lang", "OperatingSystem", null, "TotalSwapSpaceSize", null));
        values.put("jvm.os.FreeSwapSpaceSize", fetchValue("java.lang", "OperatingSystem", null, "FreeSwapSpaceSize", null));
        values.put("jvm.os.ProcessCpuTime", fetchValue("java.lang", "OperatingSystem", null, "ProcessCpuTime", null));
        values.put("jvm.os.FreePhysicalMemorySize", fetchValue("java.lang", "OperatingSystem", null, "FreePhysicalMemorySize", null));
        values.put("jvm.os.TotalPhysicalMemorySize", fetchValue("java.lang", "OperatingSystem", null, "TotalPhysicalMemorySize", null));
        values.put("jvm.os.SystemCpuLoad", fetchValue("java.lang", "OperatingSystem", null, "SystemCpuLoad", null));
        values.put("jvm.os.ProcessCpuLoad", fetchValue("java.lang", "OperatingSystem", null, "ProcessCpuLoad", null));
        values.put("jvm.os.SystemLoadAverage", fetchValue("java.lang", "OperatingSystem", null, "SystemLoadAverage", null));

        // Runtime
        values.put("jvm.runtime.Uptime", fetchValue("java.lang", "Runtime", null, "Uptime", null));

        // Threading    
        values.put("jvm.threading.threadcount", fetchValue("java.lang", "Threading", null, "ThreadCount", null));
        values.put("jvm.threading.peakthreadcount", fetchValue("java.lang", "Threading", null, "PeakThreadCount", null));
        values.put("jvm.threading.daemonthreadcount", fetchValue("java.lang", "Threading", null, "DaemonThreadCount", null));
        values.put("jvm.threading.totalstartedthreadcount", fetchValue("java.lang", "Threading", null, "TotalStartedThreadCount", null));

        return values;
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
                    System.exit(RETURN_UNKNOWN);
                } else if (option.equals("-U")) {
                    this.url = args[++i];
                } else if (option.equals("-username")) {
                    this.username = args[++i];
                } else if (option.equals("-password")) {
                    this.password = args[++i];
                } else if (option.equals("-q")) {
                    this.query = args[++i];
                } else if (option.equals("-test")) {

                }

                if (url == null) {
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
        InputStream is = JMXQuery.class.getClassLoader().getResourceAsStream("jmxquery/HELP");
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