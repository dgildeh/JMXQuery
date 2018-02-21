package com.outlyer.jmx.jmxquery;

import java.util.HashMap;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularDataSupport;

/**
 * Stores parameters for a single metric query passed into command line in format:
 * 
 * {metricName}<{metricLabels}>=={mBeanName}/{attribute}/{attributeKey}
 * 
 * E.g. jvm.memory.heap.used<>=java.lang:type=Memory/HeapMemoryUsage/used
 * 
 * @author David Gildeh (www.outlyer.com)
 */
public class JMXMetric {
    
    private String metricName = null;
    private HashMap<String, String> metricLabels = new HashMap<String, String>();
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
    
    public JMXMetric(String metricName, String mBeanName, String attribute, String attributeKey) {
        this.metricName = metricName;
        this.mBeanName = mBeanName;
        this.attribute = attribute;
        this.attributeKey = attributeKey;
    }
    
    public JMXMetric(String metricQuery) throws ParseError {
        this.parseMetricQuery(metricQuery);
    }
    
    public String getmetricName() {
        return metricName;
    }

    public void setmetricName(String metricName) {
        this.metricName = metricName;
    }
    
    public HashMap<String, String> getmetricLabels() {
        return this.metricLabels;
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
     * {metricName}<{metricLabels}>=={mBeanName}/{attribute}/{attributeKey};
     * 
     * where {metricName}<{metricLabels}> is optional and can include tokens
     * 
     * E.g. java_lang_{attribute}_{key}<type={type},label=key>==java.lang:type=Memory/HeapMemoryUsage/used;
     * 
     * @param metricQuery 
     */
    private void parseMetricQuery(String metricQuery) throws ParseError {
        
        try {
            String query = metricQuery;
            
            // metricName is optional
            if (metricQuery.indexOf("==") > 0) {
                int seperator = metricQuery.indexOf("==");
                String metricNamePart = query.substring(0, seperator);
                query = query.substring(seperator + 2);
                
                // Parse metric name and labels
                if (metricNamePart.indexOf("<") > 0) {
                    int labelSeperator = metricNamePart.indexOf("<");
                    this.metricName = metricNamePart.substring(0, labelSeperator);
                    String labelsPart = metricNamePart.substring(labelSeperator + 1).replace(">", "");
                    // This finds all commas which are not inside double quotes.
                    String[] labels = labelsPart.split("(?!\\B\"[^\"]*),(?![^\"]*\"\\B)");
                    for (int i=0; i < labels.length; i++) {
                        String[] parts = labels[i].split("=");
                        if (parts.length < 2) {
                            throw new ParseError("Label format " + labelsPart + " is invalid.");
                        }
                        this.metricLabels.put(parts[0], parts[1]);
                    }
                } else {
                    this.metricName = metricNamePart;
                }
            }

            // Parse Query
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
        
        if (this.metricName != null) {
            s += this.metricName + "<";
            int keyCount = 0;
            for (String key : this.metricLabels.keySet()) {
                s += key + "=" + this.metricLabels.get(key);
                if (++keyCount < this.metricLabels.size()) {
                    s += ",";
                }
            }
            s += ">";
            
        } else {
            s += this.mBeanName;
            if (this.attribute != null) {
                s += "/" + this.attribute;
            }
            if (this.attributeKey != null) {
                s += "/" + this.attributeKey;
            }
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