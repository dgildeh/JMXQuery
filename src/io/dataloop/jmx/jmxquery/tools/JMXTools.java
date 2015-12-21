/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.dataloop.jmx.jmxquery.tools;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility tools for listing and connecting to local JVMs. Note this will use
 * the tools.jar in JDK, so will require JDK installed on the host machine.
 * 
 * @author dgildeh
 */
public class JMXTools {
    
    /**
     * Lists all the JVMs on a machine with their connection URLs
     * 
     * @return  Array of JVMs with their JMX Connection URLs, null if JMX not enabled
     */
    public static ArrayList<LocalJMXConnection> getLocalJVMs() {
        
        ArrayList<LocalJMXConnection> connections = new ArrayList<LocalJMXConnection>();

        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        for (VirtualMachineDescriptor desc : vms) {
            try {
                connections.add(new LocalJMXConnection(desc));
            } catch (AttachNotSupportedException ae) {
                // Skip
            } catch (IOException ie) {
                // Skip
            }
        }
        
        return connections;
    }
    
    /**
     * Get a local process by displayName and send back the URL
     * 
     * @return  JMX Connection URL, null if not enabled or found
     */
    public static String getLocalJMXConnection(String process) {
        
        ArrayList<LocalJMXConnection> connections = JMXTools.getLocalJVMs();
        
        for (LocalJMXConnection connection : connections) {
            if (process.equals(connection.getDisplayName())) {
                return connection.getJmxUrl();
            }
        }
        
        // Not found
        return null;
    }
}
