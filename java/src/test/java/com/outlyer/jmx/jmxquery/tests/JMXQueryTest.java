package com.outlyer.jmx.jmxquery.tests;

import com.outlyer.jmx.jmxquery.JMXQuery;
import com.outlyer.jmx.jmxquery.tools.JMXTools;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Runs set of tests for JMXQuery command line tool
 * 
 * @author dgildeh
 */
public class JMXQueryTest {
    
    public JMXQueryTest() {
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
    public void testHelpPage() throws Exception {
        //JMXQuery.main(new String[]{"-help"});
        // TODO - Figure out way to test without System.exit() crashing JUnit
    }
    
    @Test
    public void testLocalProcessConnection() throws Exception {
        
        JMXQuery.main(new String[]{"-proc", "org.netbeans.Main", "-metrics",
                        "jvm.test.classes=java.lang:type=ClassLoading/LoadedClassCount;"
                      + "jvm.test.mem.max=java.lang:type=Memory/HeapMemoryUsage/max"});
    }
    
    @Test
    public void testUrlConnection() throws Exception {
        
        String url = JMXTools.getLocalJMXConnection("org.netbeans.Main");
        System.out.println(url);
        
        JMXQuery.main(new String[]{"-url", url, 
                        "-metrics",
                        "jvm.test.classes=java.lang:type=ClassLoading/LoadedClassCount;"
                      + "jvm.test.mem.max=java.lang:type=Memory/HeapMemoryUsage/max"});
    }
    
    @Test
    public void testListJVMs() throws Exception {
        
        String url = JMXTools.getLocalJMXConnection("org.netbeans.Main");
        System.out.println(url);
        
        JMXQuery.main(new String[]{"-url", url, 
                        "-list", "jvms"});
    }
    
    @Test
    public void testListMBeans() throws Exception {
        
        String url = JMXTools.getLocalJMXConnection("org.netbeans.Main");
        System.out.println(url);
        
        JMXQuery.main(new String[]{"-url", url, "-list", "mbeans", "*:*"});
    }
}