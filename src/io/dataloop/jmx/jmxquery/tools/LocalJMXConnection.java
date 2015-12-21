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
import java.util.Properties;

/**
 *
 * @author dgildeh
 */
public class LocalJMXConnection {
    
    private String displayName = "";
    private String id = "";
    private String jmxUrl = null;
    
    public LocalJMXConnection(VirtualMachineDescriptor desc) throws AttachNotSupportedException, IOException {
        VirtualMachine vm = VirtualMachine.attach(desc);        
        Properties props = vm.getAgentProperties();
        
        this.displayName = desc.displayName().split(" ")[0];
        this.id = desc.id();
        this.jmxUrl = props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return id;
    }

    public String getJmxUrl() {
        return jmxUrl;
    }
    
    @Override
    public String toString() {
        return displayName + " (" + id + "): " + jmxUrl;
    }
    
    /**
     * Returns JSON representation of LocalJMXConnection
     * 
     * @return  json
     */
    public String toJSON() {
        String json = "{";
        json += "\tdisplayname: " + displayName + ",\n";
        json += "\tid: " + id + ",\n";
        json += "\turl: " + jmxUrl + ",\n";
        json += "}";
        return json;
    }
}
