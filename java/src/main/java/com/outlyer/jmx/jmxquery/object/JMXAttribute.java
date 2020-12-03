package com.outlyer.jmx.jmxquery.object;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * JMXAttribute is used for set the JMX attributes
 *
 * @author Tibor Kiss
 *
 */
public class JMXAttribute extends JMXObject {

    private JMXParam value = null;

    /**
     * get the JMX MBean value
     */
    public JMXParam getValue() {
        return value;
    }

    /**
     * return the actual value
     */
    @JsonIgnore
    public Object getTypedValue() {
        if(value == null)
            return null;
        return JMXObjectTypeConverter.toObject(value.getType(), (String)value.getValue());
    }
    
    /**
     * set the JMX MBean value
     * 
     * @param value
     *            JMX MBean new value
     */    
    public void setValue(JMXParam value) {
        this.value = value;
    }


}
