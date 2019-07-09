package com.outlyer.jmx.jmxquery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * JMXMethod is used for local or remote invoke of JMX methods
 *
 * @author Tibor Kiss
 *
 */
public class JMXMethod {

    /**
     *
     * MethodParam is used for pass parameters to a JMXMethod
     *
     * @author Tibor Kiss
     *
     */
    public static class MethodParam {
        private Object value;
        private String type;

        /**
         * Default constructor
         */
        public MethodParam() {
            this(null, null);
        }

        /**
         * This constructor is used to create a parameter
         * 
         * @param value
         *            is the actual value
         * @param type
         *            is a basic type from:
         *            (String,Double,Float,Short,Integer,Long,Boolean,char,double,float,short,integer,long,
         *            boolean)
         */
        public MethodParam(Object value, String type) {
            setValue(value);
            setType(type);
        }

        /**
         * returns the parameter value
         * 
         * @return parameter value
         */
        public Object getValue() {
            return value;
        }

        /**
         * set the parameters value
         * 
         * @param value
         *            parameters value
         */
        public void setValue(Object value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    private static final Map<String, String> typeMap = new HashMap<String, String>();
    static {
        typeMap.put("String", String.class.getName());
        typeMap.put("Double", Double.class.getName());
        typeMap.put("Float", Float.class.getName());
        typeMap.put("Short", Short.class.getName());
        typeMap.put("Integer", Integer.class.getName());
        typeMap.put("Long", Long.class.getName());
        typeMap.put("Boolean", Boolean.class.getName());

        typeMap.put("char", char.class.getName());
        typeMap.put("short", short.class.getName());
        typeMap.put("int", int.class.getName());
        typeMap.put("long", long.class.getName());
        typeMap.put("double", double.class.getName());
        typeMap.put("float", float.class.getName());
        typeMap.put("boolean", boolean.class.getName());
        typeMap.put("double", double.class.getName());

    }

    private final static Object toObject(final String typeName, final String value) {
        if (Boolean.class.getName().endsWith(typeName))
            return Boolean.parseBoolean(value);
        if (Byte.class.getName().endsWith(typeName))
            return Byte.parseByte(value);
        if (Short.class.getName().endsWith(typeName))
            return Short.parseShort(value);
        if (Integer.class.getName().endsWith(typeName))
            return Integer.parseInt(value);
        if (Long.class.getName().endsWith(typeName))
            return Long.parseLong(value);
        if (Float.class.getName().endsWith(typeName))
            return Float.parseFloat(value);
        if (Double.class.getName().endsWith(typeName))
            return Double.parseDouble(value);

        return value;
    }

    private String objectName;
    private String name;

    private final List<MethodParam> params = new ArrayList<MethodParam>();

    /**
     * return the JMX MBean name
     * 
     * @return JMX MBean name
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * Set the JMX MBean name
     * 
     * @param objectName
     *            JMX MBean name
     */
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    /**
     * return the name of the JMX method
     * 
     * @return name of the JMX method
     */
    public String getName() {
        return name;
    }

    /**
     * set the name of the JMX method
     * 
     * @param name
     *            name of the JMX method
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * return the list of parameters
     * 
     * @return list of parameters
     */
    public List<MethodParam> getParams() {
        return params;
    }

    /**
     * This method can be used to add a parameter to a method
     * 
     * @param value
     * @param type
     */
    public void addParam(Object value, String type) {
        params.add(new MethodParam(value, type));
    }

    /**
     * This method returns the values as an array
     * 
     * @return array of value
     */
    @JsonIgnore
    public Object[] getValues() {
        List<Object> result = new ArrayList<Object>();
        for (MethodParam par : params) {
            result.add(toObject(typeMap.get(par.getType()), par.getValue().toString()));
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
        for (MethodParam par : params) {
            result.add(typeMap.get(par.getType()));
        }
        return result.toArray(new String[result.size()]);
    }

}
