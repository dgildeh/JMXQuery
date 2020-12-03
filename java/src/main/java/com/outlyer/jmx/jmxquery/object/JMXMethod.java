package com.outlyer.jmx.jmxquery.object;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * JMXMethod is used for local or remote invoke of JMX methods
 *
 * @author Tibor Kiss
 *
 */
public class JMXMethod extends JMXObject {

    private final List<JMXParam> params = new ArrayList<JMXParam>();


    /**
     * return the list of parameters
     * 
     * @return list of parameters
     */
    public List<JMXParam> getParams() {
        return params;
    }

    /**
     * This method can be used to add a parameter to a method
     * 
     * @param value
     * @param type
     */
    public void addParam(Object value, String type) {
        params.add(new JMXParam(value, type));
    }

    /**
     * This method returns the values as an array
     * 
     * @return array of value
     */
    @JsonIgnore
    public Object[] getValues() {
        List<Object> result = new ArrayList<Object>();
        for (JMXParam par : params) {
            result.add(JMXObjectTypeConverter.toObject(JMXObjectTypeConverter.typeMap.get(par.getType()), par.getValue().toString()));
        }
        return result.toArray(new Object[result.size()]);
    }

    /**
     * This method returns the values types as an array
     * 
     * @return array of the type of values
     */
    @JsonIgnore
    public String[] getTypes() {
        List<String> result = new ArrayList<String>();
        for (JMXParam par : params) {
            result.add(JMXObjectTypeConverter.typeMap.get(par.getType()));
        }
        return result.toArray(new String[result.size()]);
    }

}
