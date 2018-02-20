package com.outlyer.jmx.jmxquery;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularDataSupport;

/**
 * Stores parameters for a single metric query passed into command line in format:
 * 
 *  {metric}={mBeanName}/{attribute}/{attributeKey}
 * 
 * E.g. jvm.memory.heap.used=java.lang:type=Memory/HeapMemoryUsage/used
 * 
 * @author David Gildeh (www.outlyer.com)
 */
public class JMXMetric {
    
    private String mBeanName;
    private String attribute;
    private String attributeKey = null;
    private String attributeType = null;
    private Object value = null;
    
    public JMXMetric(String mBeanName, String attribute, String attributeKey) {
        this.mBeanName = mBeanName;
        this.attribute = attribute;
        this.attributeKey = attributeKey;
    }
    
    public JMXMetric(String metricQuery) throws ParseError {
        this.parseMetricQuery(metricQuery);
    }

    public String getmBeanName() {
        return mBeanName;
    }

    public void setmBeanName(String mBeanName) {
        this.mBeanName = mBeanName;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getAttributeType() {
        return attributeType;
    }

    /**
     * Will set type based on class instance of value
     * 
     * @param value     The value to get the type for
     */
    public void setAttributeType(Object value) {
        
        if (value instanceof String) {
            this.attributeType = "String";
        } else if (value == null) {
            this.attributeType = "Null";
        } else if (value instanceof CompositeData) {
            this.attributeType = "CompositeData";
        } else if (value instanceof TabularDataSupport) {
            this.attributeType = "TabularDataSupport";
        } else if (value instanceof ObjectName) {
            this.attributeType = "ObjectName";
        } else if (value instanceof Integer) {
            this.attributeType = "Integer";
        } else if (value instanceof Long) {
            this.attributeType = "Long";
        } else if (value instanceof Double) {
            this.attributeType = "Double";
        } else if (value instanceof Boolean) {
            this.attributeType = "Boolean";
        } else {
            this.attributeType = value.getClass().getSimpleName();
        }
    }
    
    /**
     * Helper function to parse query string in following format and initialise
     * Metric class:
     * 
     * {mBeanName}/{attribute}/{attributeKey};
     * 
     * E.g. jvm.memory.heap.used=java.lang:type=Memory/HeapMemoryUsage/used;
     * Tomcat:type=DataSource,context=/,host=localhost,class=javax.sql.DataSource,name="jdbc/storage"/numIdle;
     * 
     * @param metricQuery 
     */
    private void parseMetricQuery(String metricQuery) throws ParseError {
        
        try {
            String query = metricQuery;

            int firstColon = query.indexOf(':');
            String beanName = query.substring(0, firstColon + 1);
            query = query.substring(firstColon + 1);

            // This finds all commas which are not inside double quotes.
            String[] paths = query.split("(?!\\B\"[^\"]*),(?![^\"]*\"\\B)");
            for (int i=0; i < paths.length - 1; i++) {
                beanName += paths[i] + ",";
            }

            query = paths[paths.length - 1];
            String[] parts = query.split("(?!\\B\"[^\"]*)/(?![^\"]*\"\\B)");

            beanName += parts[0];
            this.mBeanName = beanName;

            if (parts.length > 1) {
                this.attribute = parts[1];
            }
            if (parts.length > 2) {
                this.attributeKey = parts[2];
            }
            
        } catch (Exception e) {
            throw new ParseError("Error Parsing Metic Query: " + metricQuery , e);
        }       
    }
    
    @Override
    public String toString() {
        String s = "";
        s += this.mBeanName;
        if (this.attribute != null) {
            s += "/" + this.attribute;
        }
        if (this.attributeKey != null) {
            s += "/" + this.attributeKey;
        }
        if (attributeType != null) {
            s += " (" + attributeType + ")";
        }
        if (value != null) {
            s += " = " + value.toString();
        }
        
        return s;
    }
    
    /**
     * Returns JSON representation of metric
     * 
     * @return  JSON String
     */
    public String toJSON() {

        String beanName = this.mBeanName.replace("\"", "\\\"");

        String json = "{";
        json += "\"mBeanName\" : \"" + beanName + "\"";
        json += ", \"attribute\" : \"" + this.attribute + "\"";
        if (this.attributeKey != null) {
            json += ", \"attributeKey\" : \"" + this.attributeKey + "\"";
        }
        if (attributeType != null) {
            json += ", \"attributeType\" : \"" + this.attributeType + "\"";
        }
        if (value != null) {
            json += ", \"value\" : \"" + this.value.toString() + "\"";
        }
        json += "}";

        return json;
    }
}