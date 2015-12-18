package jmxquery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
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
        private int verbatim;
        private JMXConnector connector;
        private MBeanServerConnection connection;
        private String warning, critical;
        private String attribute, info_attribute;
        private String attribute_key, info_key;
        private String object;
        private String username, password;
        
        private Object checkData;
        private Object infoData;
        
        private static final int RETURN_OK = 0; //       The plugin was able to check the service and it appeared to be functioning properly
        private static final String OK_STRING = "JMX OK -"; 
        private static final int RETURN_WARNING = 1; // The plugin was able to check the service, but it appeared to be above some "warning" threshold or did not appear to be working properly
        private static final String WARNING_STRING = "JMX WARNING -"; 
        private static final int RETURN_CRITICAL = 2; // The plugin detected that either the service was not running or it was above some "critical" threshold
        private static final String CRITICAL_STRING = "JMX CRITICAL -"; 
        private static final int RETURN_UNKNOWN = 3; // Invalid command line arguments were supplied to the plugin or low-level failures internal to the plugin (such as unable to fork, or open a tcp socket) that prevent it from performing the specified operation. Higher-level errors (such as name resolution errors, socket timeouts, etc) are outside of the control of plugins and should generally NOT be reported as UNKNOWN states.
        private static final String UNKNOWN_STRING = "JMX UNKNOWN"; 

        private void connect() throws IOException
        {
            JMXServiceURL jmxUrl = new JMXServiceURL(url);
                  
            if(username!=null) {
                 Map<String, String[]> m = new HashMap<String,String[]>();
                 m.put(JMXConnector.CREDENTIALS,new String[] {username,password});
                 connector = JMXConnectorFactory.connect(jmxUrl,m);
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
                    } else {
                        if (name.equals(process)) {
                            return connectorAddress;
                        }
                    }
                    
                } catch (AttachNotSupportedException | IOException e) {
                    // Skip
                }                          
            }
            
            return null;
        }
        

        private void disconnect() throws IOException {
                if(connector!=null){
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
            
            query.test();
                        
            //Object attr = query.connection.getAttribute(new ObjectName("java.lang.Memory"), "HeapMemoryUsage");
            
                        
//                        int status;
//                        try{
//                                query.parse(args);
//                                query.connect();
//                                query.execute();
//                                status = query.report(System.out);
//
//                        }catch(Exception ex){
//                                status = query.report(ex, System.out);
//                        }finally{
//                                try {
//                                        query.disconnect();
//                                } catch (IOException e) {
//                                        status = query.report(e, System.out);                                   
//                                }
//                        }       
                        System.exit(0);
                }
        
        /**
         * Main function for fetching JMX metrics. Will return null if the metric path 
         * is not found in MBean tree.
         * 
         * @param domain    The domain name (i.e. java.lang)
         * @param type      The type of mbean
         * @param name      (Optional) They name of the mbean is needed. Null if not
         * @param attribute The attribute to read from the mbean
         * @param key       (Option) If the attribute is Composite, specify the key of the
         *                      specific value to read
         * @return          The string value of the metric, or null if not found
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
                
            } catch (MalformedObjectNameException | MBeanException | AttributeNotFoundException 
                    | InstanceNotFoundException | ReflectionException | IOException e) {
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
         * Get key JVM stats to test connection is working
         */
        private void test() {
            
            HashMap<String, String> values = new HashMap<String, String>();
            
            values.put("jvm.classloading.loadedclasscount", fetchValue("java.lang", "ClassLoading", null, "LoadedClassCount", null));
            values.put("jvm.memory.committed", fetchValue("java.lang", "Memory", null, "HeapMemoryUsage", "committed"));
            values.put("jvm.memory.init", fetchValue("java.lang", "Memory", null, "HeapMemoryUsage", "init"));
            values.put("jvm.memory.max", fetchValue("java.lang", "Memory", null, "HeapMemoryUsage", "max"));
            values.put("jvm.memory.used", fetchValue("java.lang", "Memory", null, "HeapMemoryUsage", "used"));
            values.put("jvm.threadcount", fetchValue("java.lang", "Threading", null, "ThreadCount", null));
            values.put("jvm.gc.collectioncount", fetchValue("java.lang", "GarbageCollector", "PS MarkSweep", "CollectionCount", null));

            String output = "OK | ";
            
            for (String key : values.keySet()) {
                output += key + "=" + values.get(key) + ";;;; ";
            }
            
            System.out.println(output);
        }

        private int report(Exception ex, PrintStream out)
        {
                if(ex instanceof ParseError){
                        out.print(UNKNOWN_STRING+" ");
                        reportException(ex, out);               
                        out.println(" Usage: check_jmx -help ");
                        return RETURN_UNKNOWN;
                }else{
                        out.print(CRITICAL_STRING+" ");
                        reportException(ex, out);               
                        out.println();
                        return RETURN_CRITICAL;
                }
        }

        private void reportException(Exception ex, PrintStream out) {

                if(verbatim<2)
                        out.print(rootCause(ex).getMessage());
                else{
                        out.print(ex.getMessage()+" connecting to "+object+" by URL "+url);
                }
        
                
                if(verbatim>=3)         
                        ex.printStackTrace(out);

        }


        private static Throwable rootCause(Throwable ex) {
                if(ex.getCause()==null)
                        return ex;
                return rootCause(ex.getCause());
        }


        private int report(PrintStream out)
        {
                int status;
                if(compare( critical )){
                        status = RETURN_CRITICAL;                       
                        out.print(CRITICAL_STRING);
                }else if (compare( warning)){
                        status = RETURN_WARNING;
                        out.print(WARNING_STRING);
                }else{
                        status = RETURN_OK;
                        out.print(OK_STRING);
                }
                
                boolean shown = false;
                if(infoData==null || verbatim>=2){
                        out.print(' ');
                        if(attribute_key!=null)
                                out.print(attribute+'.'+attribute_key+" is "+checkData);
                        else {
                                out.print(attribute+" is "+checkData);
                                shown=true;
                        }
                        
                }
                        
                if(!shown && infoData!=null){
                        if(infoData instanceof CompositeDataSupport)
                                report((CompositeDataSupport)infoData, out);
                        else
                                out.print(infoData.toString());
                }
                
                out.println();
                return status;
        }

        @SuppressWarnings("unchecked")
        private void report(CompositeDataSupport data, PrintStream out) {
                CompositeType type = data.getCompositeType();
                out.print(",");
                for(Iterator it = type.keySet().iterator();it.hasNext();){
                        String key = (String) it.next();
                        if(data.containsKey(key))
                                out.print(key+'='+data.get(key));
                        if(it.hasNext())
                                out.print(';');
                }
        }


        private boolean compare(String level) {         
                if(checkData instanceof Number) {
                        Number check = (Number)checkData;
                        if(check.doubleValue()==Math.floor(check.doubleValue())) {
                                return check.doubleValue()>=Double.parseDouble(level);
                        } else {
                                return check.longValue()>=Long.parseLong(level);
                        }
                }
                if(checkData instanceof String) {
                        return checkData.equals(level);
                }
                if(checkData instanceof Boolean) {
                        return checkData.equals(Boolean.parseBoolean(level));
                }
                throw new RuntimeException(level + "is not of type Number,String or Boolean");
        }


        private void execute() throws Exception{
                Object attr = connection.getAttribute(new ObjectName(object), attribute);
                if(attr instanceof CompositeDataSupport){
                        CompositeDataSupport cds = (CompositeDataSupport) attr;
                        if(attribute_key==null)
                                throw new ParseError("Attribute key is null for composed data "+object);
                        checkData = cds.get(attribute_key);
                }else{
                        checkData = attr;
                }
                
                if(info_attribute!=null){
                        Object info_attr = info_attribute.equals(attribute) ? 
                                                                        attr : 
                                                                        connection.getAttribute(new ObjectName(object), info_attribute);
                        if(info_key!=null && (info_attr instanceof CompositeDataSupport) && verbatim<4){
                                CompositeDataSupport cds = (CompositeDataSupport) attr;
                                infoData = cds.get(info_key);
                        }else{
                                infoData = info_attr;
                        }
                }
                
        }

        private void parse(String[] args) throws ParseError
        {
                try{
                        for(int i=0;i<args.length;i++){
                                String option = args[i];
                                if(option.equals("-help"))
                                {
                                        printHelp(System.out);
                                        System.exit(RETURN_UNKNOWN);
                                }else if(option.equals("-U")){
                                        this.url = args[++i];
                                }else if(option.equals("-O")){
                                        this.object = args[++i];
                                }else if(option.equals("-A")){
                                        this.attribute = args[++i];
                                }else if(option.equals("-I")){
                                        this.info_attribute = args[++i];
                                }else if(option.equals("-J")){
                                        this.info_key = args[++i];
                                }else if(option.equals("-K")){
                                        this.attribute_key = args[++i];
                                }else if(option.startsWith("-v")){
                                        this.verbatim = option.length()-1;
                                }else if(option.equals("-w")){
                                        this.warning = args[++i];
                                }else if(option.equals("-c")){
                                        this.critical = args[++i];
                                }else if(option.equals("-username")) {
                                        this.username = args[++i];
                                }else if(option.equals("-password")) {
                                        this.password = args[++i];
                                }
                        }
                        
                        if(url==null || object==null || attribute==null)
                                throw new Exception("Required options not specified");
                }catch(Exception e){
                        throw new ParseError(e);
                }
                
        }

        /*
         * Prints Help Text
        */
        private void printHelp(PrintStream out) {
                InputStream is = JMXQuery.class.getClassLoader().getResourceAsStream("jmxquery/HELP");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                try{
                        while(true){
                                String s = reader.readLine();
                                if(s==null)
                                        break;
                                out.println(s);
                        }
                } catch (IOException e) {
                        out.println(e);
                }finally{
                        try {
                                reader.close();
                        } catch (IOException e) {
                                out.println(e);
                        }
                }       
        }



}