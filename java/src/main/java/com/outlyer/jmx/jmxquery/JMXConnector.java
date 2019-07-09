package com.outlyer.jmx.jmxquery;

import com.outlyer.jmx.jmxquery.tools.JMXTools;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
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
import javax.management.openmbean.TabularDataSupport;
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
     * invokes a method on the server using JMX
     * 
     * @param name
     *            - The object name of the MBean on which the method is to be
     *            invoked.
     * @param method
     *            - The name of the method to be invoked.
     * @param args
     *            - An array containing the arguments of the method to be set
     *            when the operation is invoked
     * @param signatures
     *            - An array containing the signatures of the arguments, an
     *            array of class names in the format returned by
     *            Class.getName().
     * @return result of invoking the operation on the MBean specified
     * @throws java.io.IOException
     * @throws javax.management.MalformedObjectNameException
     * @throws javax.management.InstanceNotFoundException
     * @throws javax.management.IntrospectionException
     * @throws javax.management.ReflectionException
     * @throws MBeanException
     */
    public Object invoke(String objectName, String method, Object[] args, String[] signatures)
            throws IOException, MalformedObjectNameException, InstanceNotFoundException, IntrospectionException,
            ReflectionException, MBeanException {
        return connection.invoke(new ObjectName(objectName), method, args, signatures);
    }

    /**
     * query the actual name of the MBean
     * 
     * @param name
     *            - The object search string.
     * @return actual name of the MBean
     * @throws java.io.IOException
     * @throws javax.management.MalformedObjectNameException
     */
    public ObjectName queryName(String name) throws MalformedObjectNameException, IOException {
        return connection.queryNames(new ObjectName(name), null).iterator().next();
    }

    /**
     * Fetches a list of metrics and their values in one go
     * 
     * @param metricsList   List of JMXMetrics to fetch
     * @return              A list of all the MBean metrics found from the query
     * @throws java.io.IOException
     * @throws javax.management.MalformedObjectNameException
     * @throws javax.management.InstanceNotFoundException
     * @throws javax.management.IntrospectionException
     * @throws javax.management.ReflectionException
     */
    public ArrayList<JMXMetric> getMetrics(ArrayList<JMXMetric> metricsList) throws IOException, 
            MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException {
        
        ArrayList<JMXMetric> newMetricList = new ArrayList<JMXMetric>();
        for (JMXMetric metric : metricsList) {
            ArrayList<JMXMetric> fetchedMetrics = getMetrics(metric);
            newMetricList.addAll(fetchedMetrics);
        }  
        return newMetricList;
    }
    
    /**
     * Main function to query and get metrics from JMX
     * 
     * @param metricQuery       Metric query to filter on, use *:* to list everything
     * @return                  A list of all the MBean metrics found from the query
     * @throws java.io.IOException
     * @throws javax.management.MalformedObjectNameException
     * @throws javax.management.InstanceNotFoundException
     * @throws javax.management.IntrospectionException
     * @throws javax.management.ReflectionException
     */
    private ArrayList<JMXMetric> getMetrics(JMXMetric metricQuery) throws IOException, 
            MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException {
        
        ArrayList<JMXMetric> metrics = new ArrayList<JMXMetric>();
        
        MBeanInfo info = null;
        JMXMetric attributeMetric = null;
        
        // Get list of MBeans from MBean Query           
        Set<ObjectInstance> instances = connection.queryMBeans(new ObjectName(metricQuery.getmBeanName()), null);
        Iterator<ObjectInstance> iterator = instances.iterator();

        // Iterate through results
        while (iterator.hasNext()) {

            ObjectInstance instance = iterator.next();

            try {

                // Get list of attributes for MBean
                info = connection.getMBeanInfo(new ObjectName(instance.getObjectName().toString()));                
                MBeanAttributeInfo[] attributes = info.getAttributes();
                for (MBeanAttributeInfo attribute : attributes) {

                    attributeMetric= new JMXMetric(instance.getObjectName().toString(),
                                                    attribute.getName(), 
                                                    null);
                    attributeMetric.setmetricName(metricQuery.getmetricName());
                    attributeMetric.setmetricLabels(metricQuery.getmetricLabels());

                    // If attribute given in query, only return those attributes
                    if ((metricQuery.getAttribute() != null) &&
                            (! metricQuery.getAttribute().equals("*"))) {

                        if (attribute.getName().equals(metricQuery.getAttribute())) {
                            // Set attribute type and get the metric(s)
                            attributeMetric.setAttributeType(attribute.getType());
                            attributeMetric.setAttribute(attribute.getName());
                            metrics.addAll(getAttributes(attributeMetric));
                        }
                    } else {

                        // Get all attributes for MBean Query
                        attributeMetric.setAttributeType(attribute.getType());
                        attributeMetric.setAttribute(attribute.getName());
                        metrics.addAll(getAttributes(attributeMetric));
                    }
                }
            } catch (NullPointerException e) {
                attributeMetric.setAttributeType(null);
                attributeMetric.setValue(null);
                metrics.add(attributeMetric);
            }   
        }
     
        return metrics;
    }
    
    /**
     * Expand an attribute to get all keys and values for it
     * 
     * @param attribute     The attribute to expand
     * @return              A list of all the attribute keys/values
     */
    private ArrayList<JMXMetric> getAttributes(JMXMetric attribute) {
        return getAttributes(attribute, null);
    }
    
    /**
     * Recursive function to expand Attributes and get any values for them
     * 
     * @param attribute     The top attribute to expand values for
     * @param value         Null if calling, used to recursively get values 
     * @return              A list of all the attributes and values for the attribute
     */
    private ArrayList<JMXMetric> getAttributes(JMXMetric attribute, Object value) {
        
        ArrayList<JMXMetric> attributes = new ArrayList<JMXMetric>();
        
        if (value == null) {
            // First time running so get value from JMX connection
            try { 
               value = connection.getAttribute(new ObjectName(attribute.getmBeanName()), attribute.getAttribute()); 
            } catch(Exception e) {
                // Do nothing - these are thrown if value is UnAvailable
            }
        }
        
        if (value instanceof CompositeData) {
            CompositeData cData = (CompositeData) value;
            // If attribute has key specified, only get that otherwise get all keys
            if (attribute.getAttributeKey() != null) {
                try {
                    JMXMetric foundKey = new JMXMetric(attribute.getmBeanName(),
                                                attribute.getAttribute(),
                                                attribute.getAttributeKey());
                    foundKey.setAttributeType(cData.get(attribute.getAttributeKey()));
                    foundKey.setmetricName(attribute.getmetricName());
                    foundKey.setmetricLabels(attribute.getmetricLabels()); 
                    attributes.addAll(getAttributes(foundKey, cData.get(attribute.getAttributeKey())));                    
                } catch (InvalidKeyException e) {
                    // Key doesn't exist so don't add to list
                }    
            } else {
                // List all the attribute keys
                Set<String> keys = cData.getCompositeType().keySet(); 
                for (String key : keys) {
                    JMXMetric foundKey = new JMXMetric(attribute.getmBeanName(),
                                             attribute.getAttribute(), key);
                    foundKey.setAttributeType(cData.get(key));
                    foundKey.setmetricName(attribute.getmetricName());
                    foundKey.setmetricLabels(attribute.getmetricLabels()); 
                    attributes.addAll(getAttributes(foundKey, cData.get(key)));
                }
            }    
        } else if (value instanceof TabularDataSupport) {
            // Ignore getting values for these types
            attribute.setAttributeType(value);
            attributes.add(attribute);
        } else {
            attribute.setAttributeType(value);
            attribute.setValue(value);
            attributes.add(attribute);
        }
        
        return attributes;
    }
}