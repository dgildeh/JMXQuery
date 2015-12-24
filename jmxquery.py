#!/usr/bin/env python

"""
    Python wrapper for JMXQuery.jar
"""

import subprocess
import json

# Relative path to Jar
JAR_PATH = 'dist/JMXQuery.jar'
# Set JDK Path in order to use tools.jar for listing local VMs
JDK_PATH = '/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/bin/java'

def getMetrics():

    query = "jvm.classloading.loadedclasscount=java.lang:type=ClassLoading/LoadedClassCount;" + \
            "jvm.memory.heap.committed=java.lang:type=Memory/HeapMemoryUsage/committed"

    command = [JDK_PATH, '-jar', JAR_PATH, '-url', 'service:jmx:rmi:///jndi/rmi://localhost:7199/jmxrmi', "-metrics", query, "-json"]
    jsonOutput = subprocess.check_output(command)

    metrics = json.loads(jsonOutput)

    # Print Nagios output
    output = "OK | "
    for metric in metrics:
        output += metric['metricName'] + "=" + metric['value'] + ";;;; "
    print output
  
def listMBeans():
    query = "java.lang:type=MemoryPool,*/Usage/"
    command = [JDK_PATH, '-jar', JAR_PATH, '-url', 'service:jmx:rmi:///jndi/rmi://localhost:7199/jmxrmi', "-list", "mbeans", query]
    output = subprocess.check_output(command)
    print output

listMBeans()