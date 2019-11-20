#!/usr/bin/env python3

"""
    Python interface to JMX. Uses local jar to pass commands to JMX.
    Results returned.
"""
try:
    import subprocess32 as subprocess
except:
    import subprocess
import os
import json
from enum import Enum
import logging

# Full Path to Jar
JAR_PATH = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    'JMXQuery-0.1.8.jar'
    )

# Default Java path and default timeout in seconds
DEFAULT_JAVA_PATH = 'java'
DEFAULT_JAR_TIMEOUT = 10

log = logging.getLogger('jmxQuery')


class MetricType(Enum):
    COUNTER = 'counter'
    GAUGE = 'gauge'


class JMXQuery:
    """
    A JMX Query which is used to fetch specific MBean
    attributes/values from the JVM. The object_name
    can support wildcards to pull multiple metrics
    at once, for example '*:*' will bring back all
    MBeans and attributes in the JVM with their values.

    You can set a metric name if you want to override
    the generated metric name created from the MBean path.
    """

    def __init__(self,
        mBeanName=None,
        attribute=None,
        attributeKey=None,
        value=None,
        value_type=None,
        metric_name=None,
        metric_labels=None):

        self.mBeanName = mBeanName
        self.attribute = attribute
        self.attributeKey = attributeKey
        self.value = value
        self.value_type = value_type
        self.metric_name = metric_name
        self.metric_labels = metric_labels or {}

    def to_query_string(self):
        """
        Build a query string to pass via command line to JMXQuery Jar
        :return:    The query string to find the MBean in format:
            
                        {mBeanName}/{attribute}/{attributeKey}
            
            Example: java.lang:type=Memory/HeapMemoryUsage/init
        """
        query = []
        if self.metric_name:
            query.append(self.metric_name)
            if self.metric_labels:
                query.append("<")
                total_count = len(self.metric_labels)
                for count, (key, value) in enumerate(
                    self.metric_labels.items(), 
                    1
                    ):
                    query.append("{} = {}".format(
                        key, 
                        value)
                        )
                    if count < total_count:
                        query.append(",")
                query.append(">")
            query.append("==")

        query.append(self.mBeanName)
        if self.attribute:
            query.append("/%s" % self.attribute)
        if self.attributeKey:
            query.append("/%s" % self.attributeKey)

        return ''.join(query)

    def to_string(self):
        string = []
        if self.metric_name:
            string.append(self.metric_name)
            if self.metric_labels:
                string.append(" {")
                total_count = len(self.metric_labels)
                for count, (key, value) in enumerate(
                    self.metric_labels.items(), 
                    1
                    ):
                    string.append("{} = {}".format(key, value))
                    if count < total_count:
                        string.append(",")
                string.append("}")
        else:
            string.append(self.mBeanName)
            if self.attribute:
                string.append("/%s" % self.attribute)
            if self.attributeKey:
                string.append("/%s" % self.attributeKey)

        string.append(" = ")
        string.append("%s  ( %s )" % (
            self.value,
            self.value_type
        ))

        return ''.join(string)

class JMXConnection(object):
    """
    The main class that connects to the JMX endpoint via a local JAR 
    to run queries
    """
    def __init__(self, 
        uri=None,
        user=None,
        passwd=None,
        jpath=DEFAULT_JAVA_PATH):
        """
        Creates instance of JMXQuery set to a specific connection uri
        for the JMX endpoint.

        :param uri:  The JMX connection URL. E.g.

            service:jmx:rmi:///jndi/rmi://localhost:7199/jmxrmi

        :param user:    (Optional) Username if JMX endpoint is secured
        :param passwd:  (Optional) Password if JMX endpoint is secured
        :param jpath:    (Optional) Java path.  Default is 'java'
        """

        self.connection_uri = uri
        self.jmx_username = user
        self.jmx_password = passwd
        self.java_path = jpath

    def run_jar(self, queries, timeout):
        """
        Run the JAR and return the results

        :param query:  The query
        :return:       The full command array to run via subprocess
        """

        command =  [
            self.java_path,
            '-jar',
            JAR_PATH,
            '-url',
            self.connection_uri,
            "-json"]

        if self.jmx_username:
            command.extend([
                "-u",
                self.jmx_username,
                "-p",
                self.jmx_password])

        queryString = []
        for query in queries:
            queryString.append("%s;" % query.to_query_string())

        command.extend(["-q", ''.join(queryString)])

        jsonOutput = "[]"
        output = subprocess.run(command,
                                stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE,
                                timeout=timeout,
                                check=True)

        jsonOutput = output.stdout.decode('utf-8')

        metrics = self.load_from_json(jsonOutput)

        return metrics

    def load_from_json(self, jsonOutput):
        """
        Loads the list of returned metrics from JSON response

        :param jsonOutput:  The JSON Array returned from the command line
        :return:            An array of JMXQuerys
        """
        jsonMetrics = json.loads(jsonOutput)
        metrics = []

        for jm in jsonMetrics:
            mBeanName = jm['mBeanName']
            attribute = jm['attribute']
            attributeType = jm['attributeType']
            metric_name = None
            if 'metricName' in jm:
                metric_name = jm['metricName']
            metric_labels = None
            if 'metricLabels' in jm:
                metric_labels = jm['metricLabels']
            attributeKey = None
            if 'attributeKey' in jm:
                attributeKey = jm['attributeKey']
            value = None
            if 'value' in jm:
                value = jm['value']

            metrics.append(JMXQuery(
                mBeanName,
                attribute,
                attributeKey,
                value,
                attributeType,
                metric_name,
                metric_labels))

        return metrics

    def query(self, queries, timeout=DEFAULT_JAR_TIMEOUT):
        """
        Run a list of JMX Queries against the JVM and get the results

        :param queries: A list of JMXQuerys to query the JVM for
        :return: list of query results with their current values
        """

        return self.run_jar(queries, timeout)
