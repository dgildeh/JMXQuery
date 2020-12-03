package com.outlyer.jmx.jmxquery.object;

/**
 * This class is used to set a JMX object value 
 * @author Kiss Tibor
 *
 */
public class JMXObject {
    private String objectName;
    private String name;

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
}
