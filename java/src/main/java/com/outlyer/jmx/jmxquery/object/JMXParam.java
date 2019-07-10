package com.outlyer.jmx.jmxquery.object;

/**
*
* JMXParam is used for pass parameters to a JMXMethod
*
* @author Tibor Kiss
*
*/
public class JMXParam {
    private Object value;
    private String type;

    /**
     * Default constructor
     */
    public JMXParam() {
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
    public JMXParam(Object value, String type) {
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
