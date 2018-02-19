package com.outlyer.jmx.jmxquery;

import com.outlyer.jmx.jmxquery.tools.JMXTools;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.InvalidKeyException;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Connection class with utility functions for querying the JVM 
 * JMX interface for values
 * 
 * @author David Gildeh (www.outlyer.com)
 */
public class JMXConnector {
    
    private javax.management.remote.JMXConnector connector;
    private MBeanServerConnection connection;
    
    /**
     * Connects to local process 
     * 
     * @param processName   The name of the process to attach too, i.e. org.netbean.Main
     * @throws IOException
     */
    public JMXConnector(String processName) throws IOException {
        connectLocalProcess(processName);
    }
    
    /**
     * Connect to the Java VM JMX
     * 
     * @param url       JMX Connection URL
     * @param username  JMX Connection username, null if none
     * @param password  JMX Connection password, null if none
     * 
     * @throws IOException 
     */
    public JMXConnector(String url, String username, String password) throws IOException {
        connect(url, username, password);
    }
    
    /**
     * Connect to a local JVM process via its displayName
     * 
     * @param processName   The displayName of the process, i.e. org.netbeans.Main
     * @throws IOException 
     */
    private void connectLocalProcess(String processName) throws IOException {
        String url = JMXTools.getLocalJMXConnection(processName);
        connect(url, null, null);
    }
    
    /**
     * Connect to the Java VM JMX
     * 
     * @param url       JMX Connection URL
     * @param username  JMX Connection username, null if none
     * @param password  JMX Connection password, null if none
     * 
     * @throws IOException 
     */
    private void connect(String url, String username, String password) throws IOException {
        
        if (url == null) {
            throw new IOException("Cannot connect to null URL. If connecting via -proc option, check the JVM process name is correct or running.");
        }
        
        JMXServiceURL jmxUrl = new JMXServiceURL(url);

        if (username != null) {
            Map<String, String[]> m = new HashMap<String, String[]>();
            m.put(javax.management.remote.JMXConnector.CREDENTIALS, new String[]{username, password});
            connector = JMXConnectorFactory.connect(jmxUrl, m);
        } else {
            connector = JMXConnectorFactory.connect(jmxUrl);
        }

        connection = connector.getMBeanServerConnection();
    }

    /**
     * Disconnect JMX Connection
     * 
     * @throws IOException 
     */
    public void disconnect() throws IOException {
        if (connector != null) {
            connector.close();
            connector = null;
        }
    }
    
    /**
     * Fetches a list of metrics and their values in one go
     * 
     * @param metricsList   List of JMXMetrics to fetch
     * @return              metricsList with values for each metric populated
     * @throws java.io.IOException
     */
    public ArrayList<JMXMetric> getMetrics(ArrayList<JMXMetric> metricsList) throws IOException {
        ArrayList<JMXMetric> newMetricList = new ArrayList<JMXMetric>();
        for (JMXMetric metric : metricsList) {
            ArrayList<JMXMetric> fetchedMetrics = getMetrics(metric, true);
            newMetricList.addAll(fetchedMetrics);
        }  
        return newMetricList;
    } 
    
    /**
     * List all the metrics for a particular query
     * 
     * @param metricQuery   Search query to filter on, *:* will return everything
     * @return              JMX Metrics that match search query
     */
    public ArrayList<JMXMetric> listMetrics(JMXMetric metricQuery) throws IOException {
        return getMetrics(metricQuery, false);
    }
    
    /**
     * Main function to query and get metrics from JMX
     * 
     * @param metricQuery       Metric query to filter on, use *:* to list everything
     * @param getValues         true = get values for each metric, false = only get metric paths
     * @return
     * @throws IOException 
     */
    private ArrayList<JMXMetric> getMetrics(JMXMetric metricQuery, boolean getValues) throws IOException {
        
        ArrayList<JMXMetric> metrics = new ArrayList<JMXMetric>();
        
        try {
        
            // Get list of MBeans from MBean Query           
            Set<ObjectInstance> instances = connection.queryMBeans(new ObjectName(metricQuery.getmBeanName()), null);
            Iterator<ObjectInstance> iterator = instances.iterator();
                
            // Iterate through results
            while (iterator.hasNext()) {
                
                ObjectInstance instance = iterator.next(); 
                JMXMetric attributeMetric = new JMXMetric(metricQuery.getMetric(),
                                                          instance.getObjectName().toString(),
                                                          metricQuery.getAttribute(), 
                                                          metricQuery.getAttributeKey());
                
                // Get list of attributes for MBean
                MBeanInfo info = connection.getMBeanInfo(new ObjectName(instance.getObjectName().toString()));                
                MBeanAttributeInfo[] attributes = info.getAttributes();
                for (MBeanAttributeInfo attribute : attributes) {
                    
                    // If attribute given in query, only return those attributes
                    if ((metricQuery.getAttribute() != null) &&
                            (! metricQuery.getAttribute().equals("*"))) {
                             
                        if (attribute.getName().equals(metricQuery.getAttribute())) {
                            // Set attribute type and get the metric(s)
                            attributeMetric.setAttributeType(attribute.getType());
                            attributeMetric.setAttribute(attribute.getName());
                            metrics.addAll(getAttribute(attributeMetric, getValues));
                        }
                    } else {
                        
                        // Get all attributes for MBean Query
                        attributeMetric.setAttributeType(attribute.getType());
                        attributeMetric.setAttribute(attribute.getName());
                        metrics.addAll(getAttribute(attributeMetric, getValues));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error listing MBean Tree for metric " + metricQuery.toString()
                    + ": " + Arrays.toString(e.getStackTrace()));    
        }
        
        return metrics;
    }
    
    /**
     * Get an attribute for an MBean Query
     * 
     * @param attribute  The 
     * @param getValues
     * @return 
     * @throws Exception
     */
    private ArrayList<JMXMetric> getAttribute(JMXMetric attribute, boolean getValues) throws Exception {
        
        ArrayList<JMXMetric> attributes = new ArrayList<JMXMetric>();
        
        // Format attribute metric name if needed
        formatMetricName(attribute);
        
        // Get keys if Composite Data Attribute
        if (attribute.getAttributeType().contains("CompositeData") || getValues) {
            
            // Get value
            Object value = connection.getAttribute(new ObjectName(attribute.getmBeanName()), attribute.getAttribute());
            
            if (value instanceof CompositeData) {
                
                CompositeData cData = (CompositeData) value;
                
                if (attribute.getAttributeKey() != null) {
                    
                    try {
                        
                        value = cData.get(attribute.getAttributeKey());
                    
                        // Only get values that have the key
                        JMXMetric foundAttribute = new JMXMetric(attribute.getMetric(), attribute.getmBeanName(),
                                                     attribute.getAttribute(), attribute.getAttributeKey());
                        if (getValues) {
                            foundAttribute.setValue(value);
                        }
                        
                        attributes.add(foundAttribute);
                        
                    } catch (InvalidKeyException e) {
                        // Key doesn't exist so don't add to list
                    }
                    
                } else {
                    
                    // List all the attribute keys
                    Set<String> keys = cData.getCompositeType().keySet(); 
                    for (String key : keys) {
                            JMXMetric foundAttribute = new JMXMetric(attribute.getMetric(), attribute.getmBeanName(),
                                                     attribute.getAttribute(), key);
                            if (getValues) {
                                foundAttribute.setValue(cData.get(key));
                            }
                            attributes.add(foundAttribute);
                        }
                }
            } else {
                
                JMXMetric foundAttribute = new JMXMetric(attribute.getMetric(), attribute.getmBeanName(),
                                                     attribute.getAttribute(), null);
                
                // Get the values for the attribute
                if (getValues) {
                    foundAttribute.setValue(value);
                }
                
                attributes.add(foundAttribute);
            }
        } else {
            
                // Just list the metric attributes
                
                JMXMetric foundAttribute = new JMXMetric(attribute.getMetric(), attribute.getmBeanName(),
                                                     attribute.getAttribute(), null);
                
                attributes.add(foundAttribute);      
        }
        
        return attributes;
    }
    
    /**
     * Replaces any [] tokens in metric names with the value from the MBean name if
     * present otherwise will not replace. 
     * 
     * For example:
     * 
     * jvm.gc.[name].collectioncount = java.lang:type=GarbageCollector,name=PS Scavenge/CollectionTime
     * 
     * Will update [name] to: jvm.gc.PS_Scavenge.collectioncount
     * 
     * @param metric    The metric to update name for
     */
    private void formatMetricName(JMXMetric metric) {
        
        // Check if variable in name
        if (metric.getMetric() != null && metric.getMetric().contains("[")) {
            
            Pattern pattern = Pattern.compile("\\[(.+?)\\]");
            Matcher matcher = pattern.matcher(metric.getMetric());
            Hashtable<String, String> replacements;
            
            // Populate replacements from MBean ObjectName
            try {
                ObjectName name = new ObjectName(metric.getmBeanName());
                replacements = name.getKeyPropertyList();
            } catch (MalformedObjectNameException e) {
                return;
            }
            
            StringBuilder builder = new StringBuilder();
            int i = 0;
            while (matcher.find()) {
                String replacement = replacements.get(matcher.group(1));
                builder.append(metric.getMetric().substring(i, matcher.start()));
                if (replacement == null) {
                    builder.append(matcher.group(0));
                    i = matcher.end();
                } else {
                    builder.append(replacement.replaceAll(" ", "_").toLowerCase());
                    i = matcher.end();
                }
            }
            builder.append(metric.getMetric().substring(i, metric.getMetric().length()));
            metric.setMetric(builder.toString());
        }
    }
}