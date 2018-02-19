package com.outlyer.jmx.jmxquery.tests;

import com.outlyer.jmx.jmxquery.tools.JMXTools;
import com.outlyer.jmx.jmxquery.tools.LocalJMXConnection;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dgildeh
 */
public class JMXToolsTest {
    
    public JMXToolsTest() {
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

    //  Test we can list the JVMs running and their connection URLs
    @Test
    public void testGetLocalJVMs() {
        ArrayList<LocalJMXConnection> connections = JMXTools.getLocalJVMs();
        
        for (LocalJMXConnection connection : connections) {
            System.out.println("JVM: " + connection.getDisplayName() 
                    + " (" + connection.getId() + "): " + connection.getJmxUrl());
        }
    }
    
    // Test we can get a specific JVM Connection for Netbeans
    @Test
    public void testGetLocalJMXConnection() {
        Assert.assertNotNull(JMXTools.getLocalJMXConnection("org.netbeans.Main"));
    }
}
