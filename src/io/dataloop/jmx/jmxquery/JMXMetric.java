package io.dataloop.jmx.jmxquery;

/**
 * Stores parameters for a single metric query passed into command line in format:
 * 
 *  {metric}={mBeanName}/{attribute}/{attributeKey}
 * 
 * E.g. jvm.memory.heap.used=java.lang:type=Memory/HeapMemoryUsage/used
 * 
 * @author dgildeh
 */
public class JMXMetric {
    
    private String metric;
    private String mBeanName;
    private String attribute;
    private String attributeKey = null;
    private String attributeType = null;
    private Object value = null;
    
    public JMXMetric(String metric, String mBeanName, String attribute, String attributeKey) {
        this.metric = metric;
        this.mBeanName = mBeanName;
        this.attribute = attribute;
        this.attributeKey = attributeKey;
    }
    
    public JMXMetric(String metricQuery) throws ParseError {
        this.parseMetricQuery(metricQuery);
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
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

    public void setAttributeType(String attributeType) {
        this.attributeType = attributeType;
    }
    
    /**
     * Helper function to parse query string in following format and initialise
     * Metric class:
     * 
     * {metric}={mBeanName}/{attribute}/{attributeKey};
     * 
     * E.g. jvm.memory.heap.used=java.lang:type=Memory/HeapMemoryUsage/used;
     * 
     * @param metricQuery 
     */
    private void parseMetricQuery(String metricQuery) throws ParseError {
        
        try {
            this.metric = metricQuery.split("=")[0];
            String query = metricQuery.substring(this.metric.length() + 1);
            String[] parts = query.split("/");
            this.mBeanName = parts[0];
            this.attribute = parts[1];
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
        if (this.metric != null) {
            s += this.metric + "=";
        }
        s += this.mBeanName + "/" + this.attribute;
        if (this.attributeKey != null) {
            s += "/" + this.attributeKey;
        }
        if (value != null) {
            s += "=" + value.toString();
        }
        if (attributeType != null) {
            s += " (" + attributeType + ")";
        }
        
        return s;
    }
    
    /**
     * Returns JSON representation of metric
     * 
     * @return  JSON String
     */
    public String toJSON() {
        String json = "{";
        if (this.metric != null) {
            json += "\"metricName\" : \"" + this.metric + "\"";
        }
        json += ", \"mBeanName\" : \"" + this.mBeanName + "\"";
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