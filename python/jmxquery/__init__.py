#!/usr/bin/env python3

"""
    Python interface to JMX. Uses local jar to pass commands to JMX and read JSON
    results returned.
"""

import subprocess
import json

# Relative path to Jar
JAR_PATH = 'JMXQuery.jar'
# Default Java path
DEFAULT_JAVA_PATH = 'java'


class JMXQuery(object):
    def __init__(self, connection_uri: str, jmx_username: str = None, jmx_password: str = None, java_path: str = None):
        """
        Creates instance of JMXQuery set to a specific connection uri for the JMX endpoint

        :param connection_uri:  The JMX connection URL. E.g.  service:jmx:rmi:///jndi/rmi://localhost:7199/jmxrmi
        :param jmx_username:    (Optional) Username if JMX endpoint is secured
        :param jmx_password:    (Optional) Password if JMX endpoint is secured
        :param java_path:       (Optional) Provide an alternative Java path on the machine to run the JAR.
                                Default is 'java' which will use the machines default JVM
        """
        self.connection_uri = connection_uri
        self.jmx_username = jmx_username
        self.jmx_password = jmx_password
        self.java_path = DEFAULT_JAVA_PATH
        if java_path != None:
            self.java_path = java_path


    def getMBeanValues(self):

        query = ""

        command = [self.java_path, '-jar', JAR_PATH, '-url', 'service:jmx:rmi:///jndi/rmi://localhost:7199/jmxrmi',
                   "-metrics", query, "-json"]

        jsonOutput = subprocess.check_output(command)
        metrics = json.loads(jsonOutput)