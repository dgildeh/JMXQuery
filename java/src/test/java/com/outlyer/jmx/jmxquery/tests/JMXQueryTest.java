package com.outlyer.jmx.jmxquery.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outlyer.jmx.jmxquery.JMXQuery;
import com.outlyer.jmx.jmxquery.object.JMXAttribute;
import com.outlyer.jmx.jmxquery.object.JMXMethod;
import com.outlyer.jmx.jmxquery.object.JMXObject;
import com.outlyer.jmx.jmxquery.object.JMXParam;
import com.outlyer.jmx.jmxquery.tools.JMXTools;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Base64;
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
    public void testArrayAttribute() throws Exception {
        JMXQuery.main(new String[] { "-url", "service:jmx:rmi://localhost:12345/jndi/rmi://localhost:12345/jmxrmi",
                "-q", "com.outlyer.jmx.jmxquery.app.tests:type=Test/Names" });

    }

    @Test
    public void testArrayAttributeJSON() throws Exception {
        JMXQuery.main(new String[] { "-url", "service:jmx:rmi://localhost:12345/jndi/rmi://localhost:12345/jmxrmi",
                "-q", "-json", "com.outlyer.jmx.jmxquery.app.tests:type=Test/Names" });

    }
    
    @Test
    public void testHelpPage() throws Exception {
        //JMXQuery.main(new String[]{"-help"});
        // TODO - Figure out way to test without System.exit() crashing JUnit
    }
    
    @Test
    public void testLocalProcessConnection() throws Exception {
        
        JMXQuery.main(new String[]{"-proc", "org.netbeans.Main", "-metrics",
                        "java.lang:type=ClassLoading/LoadedClassCount;"
                      + "java.lang:type=Memory/HeapMemoryUsage/max"});
    }
    
    @Test
    public void testUrlConnection() throws Exception {
        
        String url = JMXTools.getLocalJMXConnection("org.netbeans.Main");
        System.out.println("Connection URL: " + url);
        
        JMXQuery.main(new String[]{"-url", url, 
                        "-metrics",
                        "java.lang:type=ClassLoading/LoadedClassCount;"
                      + "java.lang:type=Memory/HeapMemoryUsage/max"});
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

    @Test
    public void testInvokeSayHello() throws Exception {
        JMXMethod method = new JMXMethod();
        method.setName("sayHello");
        method.setObjectName("com.outlyer.jmx.jmxquery.app.tests:type=Test");

        invokeMethod(method);
    }

    @Test
    public void testInvokeValue() throws Exception {
        JMXMethod method = new JMXMethod();
        method.setName("value");
        method.setObjectName("com.outlyer.jmx.jmxquery.app.tests:type=Test");

        invokeMethod(method);
    }

    @Test
    public void testInvokeGetString() throws Exception {
        JMXMethod method = new JMXMethod();
        method.setName("valueString");
        method.setObjectName("com.outlyer.jmx.jmxquery.app.tests:type=Test");
        method.addParam("String\r\nok; hello test", "String");

        invokeMethod(method);
    }

    @Test
    public void testInvokeAddIntInt() throws Exception {
        JMXMethod method = new JMXMethod();
        method.setName("add");
        method.setObjectName("com.outlyer.jmx.jmxquery.app.tests:type=Test");
        method.addParam(23, "int");
        method.addParam(25, "int");

        invokeMethod(method);
    }

    @Test
    public void testInvokeAddDoubleInt() throws Exception {
        JMXMethod method = new JMXMethod();
        method.setName("add");
        method.setObjectName("com.outlyer.jmx.jmxquery.app.tests:type=Test");
        method.addParam(23.3, "Double");
        method.addParam(25, "int");

        invokeMethod(method);
    }

    @Test
    public void testSetIntegerValue() throws Exception {
        JMXAttribute attribute = new JMXAttribute();
        attribute.setName("Val");
        attribute.setObjectName("com.outlyer.jmx.jmxquery.app.tests:type=Test");
        attribute.setValue(new JMXParam(23, "Integer"));
        invokeSetter(attribute);
        JMXQuery.main(new String[] { "-url", "service:jmx:rmi://localhost:12345/jndi/rmi://localhost:12345/jmxrmi",
                "-q", "com.outlyer.jmx.jmxquery.app.tests:type=Test/Val" });
    }
    
    @Test
    public void testInvokeAddStringString() throws Exception {
        JMXMethod method = new JMXMethod();
        method.setName("add");
        method.setObjectName("com.outlyer.jmx.jmxquery.app.tests:type=Test");
        method.addParam("Test ", "String");
        method.addParam("test", "String");

        invokeMethod(method);
    }

    private static final String serialze(JMXObject object) {
        ObjectMapper objectMapper = new ObjectMapper();
        String originalInput = null;
        try {
            originalInput = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println(originalInput);
        String encodedString = Base64.getEncoder().encodeToString(originalInput.getBytes());
        System.out.println(encodedString);
        return encodedString;
    }

    private static void invokeMethod(JMXMethod method) throws Exception {
        String encodedString = serialze(method);
        JMXQuery.main(new String[] { "-url", "service:jmx:rmi://localhost:12345/jndi/rmi://localhost:12345/jmxrmi",
                "-c", encodedString });
    }
    
    private static void invokeSetter(JMXAttribute attribute) throws Exception {
        String encodedString = serialze(attribute);
        JMXQuery.main(new String[] { "-url", "service:jmx:rmi://localhost:12345/jndi/rmi://localhost:12345/jmxrmi",
                "-s", encodedString });
    }
}