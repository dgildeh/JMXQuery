package com.outlyer.jmx.jmxquery.tests;

import com.outlyer.jmx.jmxquery.JMXMetric;
import com.outlyer.jmx.jmxquery.ParseError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author dgildeh
 */

public class JMXParserTest {
    public JMXParserTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void testMetricNameOptional() throws ParseError {
        String q = "java.lang:type=Memory/HeapMemoryUsage/used";
        JMXMetric m = new JMXMetric(q);
        Assert.assertEquals(null, m.getmetricName());
        Assert.assertEquals(0, m.getmetricLabels().size());
        Assert.assertEquals("java.lang:type=Memory", m.getmBeanName());
        Assert.assertEquals("HeapMemoryUsage", m.getAttribute());
        Assert.assertEquals("used", m.getAttributeKey());
    }
    
    @Test
    public void testMetricNameWithoutLabels() throws ParseError {
        String q = "java_{token}_name==java.lang:type=Memory/HeapMemoryUsage/used";
        JMXMetric m = new JMXMetric(q);
        Assert.assertEquals("java_{token}_name", m.getmetricName());
        Assert.assertEquals(0, m.getmetricLabels().size());
        Assert.assertEquals("java.lang:type=Memory", m.getmBeanName());
        Assert.assertEquals("HeapMemoryUsage", m.getAttribute());
        Assert.assertEquals("used", m.getAttributeKey());
    }
    
    @Test
    public void testMetricNameWithLabels() throws ParseError {
        String q = "java_{token}_name<label1={token},label2=value>==java.lang:type=Memory/HeapMemoryUsage/used";
        JMXMetric m = new JMXMetric(q);
        Assert.assertEquals("java_{token}_name", m.getmetricName());
        Assert.assertEquals(2, m.getmetricLabels().size());
        Assert.assertEquals("{token}", m.getmetricLabels().get("label1"));
        Assert.assertEquals("value", m.getmetricLabels().get("label2"));
        Assert.assertEquals("java.lang:type=Memory", m.getmBeanName());
        Assert.assertEquals("HeapMemoryUsage", m.getAttribute());
        Assert.assertEquals("used", m.getAttributeKey());
    }
    
    @Test
    public void testSlashesInsidePath() throws ParseError {
        String q = "Tomcat:type=DataSource,context=/,host=localhost,class=javax.sql.DataSource,name=\"jdbc/storage\"/numIdle";
        JMXMetric m = new JMXMetric(q);
        Assert.assertEquals("Tomcat:type=DataSource,context=/,host=localhost,class=javax.sql.DataSource,name=\"jdbc/storage\"", m.getmBeanName());
        Assert.assertEquals("numIdle", m.getAttribute());
    }

    @Test
    public void testAttributeKey() throws ParseError {
        String q = "java.lang:type=Memory/HeapMemoryUsage/used";
        JMXMetric m = new JMXMetric(q);
        Assert.assertEquals("java.lang:type=Memory", m.getmBeanName());
        Assert.assertEquals("HeapMemoryUsage", m.getAttribute());
        Assert.assertEquals("used", m.getAttributeKey());
    }
}
