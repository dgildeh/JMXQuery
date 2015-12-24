package io.dataloop.jmx.jmxquery;

import io.dataloop.jmx.jmxquery.tools.JMXTools;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.InvalidKeyException;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Connection class with utility functions for querying the JVM 
 * JMX interface for values
 * 
 * @author dgildeh
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
     * Fetches a list of metrics in one go
     * 
     * @param metricsList   List of JMXMetrics to fetch
     * @return              metricsList with values for each metric populated
     */
    public ArrayList<JMXMetric> fetchValues(ArrayList<JMXMetric> metricsList) {
        for (JMXMetric metric : metricsList) {
            fetchValue(metric);
        }
        
        return metricsList;
    } 

    /**
     * Main function for fetching JMX metrics. Will return null if the metric
     * path is not found in MBean tree.
     *
     * @param metric    
     * @return The string value of the metric, or null if not found
     */
    public String fetchValue(JMXMetric metric) {
        
        Object value; 
        
        try {
            
            ObjectName objectName = new ObjectName(metric.getmBeanName());
            value = connection.getAttribute(objectName, metric.getAttribute());

        } catch (MalformedObjectNameException e) {
            // If we can't find the value specified return null
            value = null;
        } catch (MBeanException e) {
            // If we can't find the value specified return null
            value = null;
        } catch (AttributeNotFoundException e) {
            // If we can't find the value specified return null
            value = null;
        } catch (InstanceNotFoundException e) {
            // If we can't find the value specified return null
            value = null;
        } catch (ReflectionException e) {
            // If we can't find the value specified return null
            value = null;
        } catch (IOException e) {
            // If we can't find the value specified return null
            value = null;
        }

        if (value instanceof CompositeData) {
            CompositeData data = (CompositeData) value;
            try {                
                if (metric.getAttributeKey() != null) {
                    value = data.get(metric.getAttributeKey());
                } else {
                    value = null;
                }
            } catch (InvalidKeyException e) {
                // Ket doesn't exist so return null
                value = null;
            }
        } 
        
        metric.setValue(value);
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }    
    }
    
    /**
     * Gets the full JMX MBean tree with all their attributes
     * 
     * @param metricQuery       Metric query to filter tree on, use *:* to list everything
     * @param getValues         true = get values for each metric, false = only get metric paths
     * @return
     * @throws IOException 
     */
    public ArrayList<JMXMetric> getMBeansTree(JMXMetric metricQuery, boolean getValues) throws IOException {
        
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
}