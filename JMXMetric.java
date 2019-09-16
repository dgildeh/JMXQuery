package com.outlyer.jmx.jmxquery;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * Updated Colin Paice to prefix JSON with da
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
    
    public void setmetricLabels(HashMap<String, String> metricLabels) {
        this.metricLabels.clear();
        this.metricLabels.putAll(metricLabels);
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
     * Forces the object to replace any tokens in metricName or metricLabels from
     * the mBean object properties, attribute or attributeKey. The following
     * tokens are replaced:
     * 
     *  {attribute} - Will replace with this.attribute
     *  {attributeKey} - Will replace with this.attributeKey
     *  {XXX} - Will replace with any mBean object property with same name as XXX
     * 
     */
    public void replaceTokens() {
        // Only run if metricName isn't null
        if (this.metricName != null) {
            
            HashMap<String, String> replacements = new HashMap<String, String>();
            if (this.attribute != null) {
                replacements.put("attribute", this.attribute);
            }
            if (this.attributeKey != null) {
                replacements.put("attributeKey", this.attributeKey);
            }
            // Get properties from mBeanName
            int firstColon = this.mBeanName.indexOf(':');
            String[] props = this.mBeanName.substring(firstColon + 1)
                    .split("(?!\\B\"[^\"]*),(?![^\"]*\"\\B)");
            for (int i = 0; i < props.length; i++) {
                String[] parts = props[i].split("=");
                replacements.put(parts[0], parts[1]);
            } 
                      
            // First replace tokens in metricName
            this.metricName = this.replaceTokens(this.metricName, replacements);
            // Then labels
            for (String key : this.metricLabels.keySet()) {
                String value = this.metricLabels.get(key);
                if (value.indexOf("}") > 0) {
                    value = this.replaceTokens(value, replacements);
                    this.metricLabels.put(key, value);
                }
            }
        }
    }
    
    /**
     * Replaces the text tokens in {} with values if found in the replacements 
     * HashMap, otherwise just put the token name there instead
     * 
     * @param text              The text to replace
     * @param replacements      A HashMap of replacement tokens
     * @return                  The final string with tokens replaced
     */
    private String replaceTokens(String text, HashMap<String, String> replacements) {
        // Looking for tokens in {}, i.e. {name}
        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (matcher.find()) {
            String replacement = replacements.get(matcher.group(1));
            builder.append(text.substring(i, matcher.start()));
            if (replacement == null)
                builder.append(matcher.group(0));
            else
                builder.append(replacement);
            i = matcher.end();
        }
        builder.append(text.substring(i, text.length()));
        // Remove all quotations and spaces from any replacements
        return builder.toString().replaceAll("\"", "").replaceAll(" ", "_");
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
     * Returns JSON representation of metric, appended with any additional values passed in
     * 
     * @return  JSON String
     */
    public String toJSON(String[] passedStrings) {

        String beanName = this.mBeanName.replace("\"", "\\\"");

        String json = "{";
        if (this.metricName != null) {
            json += "\"metricName\" : \"" + this.metricName + "\",";
            json += "\"metricLabels\" : {";
            int keyCount = 0;
            for (String key : this.metricLabels.keySet()) {
                json += "\"" + key + "\" : \"" + this.metricLabels.get(key) + "\"";
                if (++keyCount < this.metricLabels.size()) {
                    json += ",";
                }
            }
            json += "},";
        }
        json += "\"mBeanName\" : \"" + beanName + "\"";
        json += ", \"attribute\" : \"" + this.attribute + "\"";
        if (this.attributeKey != null) {
            json += ", \"attributeKey\" : \"" + this.attributeKey + "\"";
        }
        if (this.attributeType != null) {
            json += ", \"attributeType\" : \"" + this.attributeType + "\"";
        }
        if (this.value != null) {
            if ((this.value instanceof Integer) || 
                    (this.value instanceof Long) || 
                    (this.value instanceof Double) || 
                    (this.value instanceof Boolean)) {
                json += ", \"value\" : " + this.value.toString();
            } else {
                json += ", \"value\" : \"" + this.value.toString() + "\"";
            }
        } 
        // add any passed in values
        for (int iStringArray =0; iStringArray < passedStrings.length; iStringArray++)
        {
            json += ","+passedStrings[iStringArray];      
        }

        json += "}";

        return json;
    }
}