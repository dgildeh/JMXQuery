#!/usr/bin/env python3

"""
    Python interface to JMX. Uses local jar to pass commands to JMX and read JSON
    results returned.
"""

import subprocess
import os
import json
from typing import List
from enum import Enum
import logging
import base64

# Full Path to Jar
JAR_PATH = os.path.dirname(os.path.realpath(__file__)) + '/JMXQuery-0.1.8.jar'
# Default Java path
DEFAULT_JAVA_PATH = 'java'
# Default timeout for running jar in seconds
DEFAULT_JAR_TIMEOUT = 10

logger = logging.getLogger(__name__)

class MetricType(Enum):
    COUNTER = 'counter'
    GAUGE = 'gauge'

class JMXQuery:
    """
    A JMX Query which is used to fetch specific MBean attributes/values from the JVM. The object_name can support wildcards
    to pull multiple metrics at once, for example '*:*' will bring back all MBeans and attributes in the JVM with their values.

    You can set a metric name if you want to override the generated metric name created from the MBean path
    """

    def __init__(self,
                 mBeanName: str,
                 attribute: str = None,
                 attributeKey: str = None,
                 value: object = None,
                 value_type: str = None,
                 metric_name: str = None,
                 metric_labels: dict = None):

        self.mBeanName = mBeanName
        self.attribute = attribute
        self.attributeKey = attributeKey
        self.value = value
        self.value_type = value_type
        self.metric_name = metric_name
        self.metric_labels = metric_labels

    def to_query_string(self) -> str:
        """
        Build a query string to pass via command line to JMXQuery Jar

        :return:    The query string to find the MBean in format:

                        {mBeanName}/{attribute}/{attributeKey}

                    Example: java.lang:type=Memory/HeapMemoryUsage/init
        """
        query = ""
        if self.metric_name:
            query += self.metric_name

            if ((self.metric_labels != None) and (len(self.metric_labels) > 0)):
                query += "<"
                keyCount = 0
                for key, value in self.metric_labels.items():
                    query += key + "=" + value
                    keyCount += 1
                    if keyCount < len(self.metric_labels):
                        query += ","
                query += ">"
            query += "=="

        query += self.mBeanName
        if self.attribute:
            query += "/" + self.attribute
        if self.attributeKey:
            query += "/" + self.attributeKey

        return query

    def to_string(self):

        string = ""
        if self.metric_name:
            string += self.metric_name

            if ((self.metric_labels != None) and (len(self.metric_labels) > 0)):
                string += " {"
                keyCount = 0
                for key, value in self.metric_labels.items():
                    string += key + "=" + value
                    keyCount += 1
                    if keyCount < len(self.metric_labels):
                        string += ","
                string += "}"
        else:
            string += self.mBeanName
            if self.attribute:
                string += "/" + self.attribute
            if self.attributeKey:
                string += "/" + self.attributeKey

        string += " = "
        string += str(self.value) + " (" + self.value_type + ")"

        return string

class JMXParam:
    """
    A JMX Parameter for JMXMethod.
    """

    def __init__(self,
                 value: str="",
                 value_type: str = "String"):
        """
        This method creates a JMXMethod param
        :param value:
        :param value_type:
        """
        self.value = value
        self.value_type = value_type

class JMXMethod:
    """
    A JMX Method which is used to invoke a specific MBean method from the JVM.
    """

    def __init__(self,
                 m_bean_name: str,
                 method_name: str = None,
                 params: List[JMXParam] = []):
        """
         Creates instance of JMXMethod

         :param m_bean_name:  The JMX MBean name. E.g.
         :param method_name:  JMX method name
         :param params:    a list of JMXParam parameters
         """
        self.mBeanName = m_bean_name
        self.methodName = method_name
        self.params = params

    def json(self):
        """
        this method return the json serialization of this object
        :return: json string
        """
        json_param = ",".join(["{\"value\":\""+param.value+"\", type:\""+param.value_type+"\"}" for param in self.params])
        return "{\"objectName\":\""+self.mBeanName+"\",\"name\":\""+self.methodName+"\",\"params\":["+json_param+"]}"

    def base64json(self):
        """
        return the base64 encoded string of the json serialization of this object
        :return: base64 json serialization of JMXMethod
        """
        json_result = self.json()
        json_result = json_result.encode('ascii')
        return base64.b64encode(json_result).decode("utf-8")

class JMXConnection(object):
    """
    The main class that connects to the JMX endpoint via a local JAR to run queries
    """

    def __init__(self, connection_uri: str, jmx_username: str = None, jmx_password: str = None, java_path: str = DEFAULT_JAVA_PATH):
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
        self.java_path = java_path

    def __run_query(self, queries: List[JMXQuery], timeout) -> List[JMXQuery]:
        """
        Run the JAR and return the results

        :param query:   The query
        :return:        The full command array to run via subprocess
        """

        command =  [self.java_path, '-jar', JAR_PATH, '-url', self.connection_uri, "-json"]
        if (self.jmx_username):
            command.extend(["-u", self.jmx_username, "-p", self.jmx_password])

        queryString = ""
        for query in queries:
            queryString += query.to_query_string() + ";"

        command.extend(["-q", queryString])
        logger.debug("Running command: " + str(command))

        jsonOutput = "[]"
        try:
            output = subprocess.run(command,
                                    stdout=subprocess.PIPE,
                                    stderr=subprocess.PIPE,
                                    timeout=timeout,
                                    check=True)

            jsonOutput = output.stdout.decode('utf-8')
        except subprocess.TimeoutExpired as err:
            logger.error("Error calling JMX, Timeout of " + str(err.timeout) + " Expired: " + err.output.decode('utf-8'))
        except subprocess.CalledProcessError as err:
            logger.error("Error calling JMX: " + err.output.decode('utf-8'))
            raise err

        logger.debug("JSON Output Received: " + jsonOutput)
        metrics = self.__load_from_json(jsonOutput)
        return metrics

    def __run_method(self, method: List[JMXMethod], timeout) -> str:
        """
        Run the JAR and return the results

        :param query:   The query
        :return:        The full command array to run via subprocess
        """

        command = [self.java_path, '-jar', JAR_PATH, '-url', self.connection_uri]
        if self.jmx_username:
            command.extend(["-u", self.jmx_username, "-p", self.jmx_password])

        command.extend(["-c", method.base64json()])
        logger.debug("Running command: " + str(command))

        output = ""
        try:
            output = subprocess.run(command,
                                    stdout=subprocess.PIPE,
                                    stderr=subprocess.PIPE,
                                    timeout=timeout,
                                    check=True)

            output = output.stdout.decode('utf-8')
        except subprocess.TimeoutExpired as err:
            logger.error("Error calling JMX, Timeout of " + str(err.timeout) + " Expired: " + err.output.decode('utf-8'))
        except subprocess.CalledProcessError as err:
            logger.error("Error calling JMX: " + err.output.decode('utf-8'))
            raise err

        logger.debug("Output received: " + output)
        return output
		
    def __load_from_json(self, jsonOutput: str) -> List[JMXQuery]:
        """
        Loads the list of returned metrics from JSON response

        :param jsonOutput:  The JSON Array returned from the command line
        :return:            An array of JMXQuerys
        """
        jsonMetrics = json.loads(jsonOutput)
        metrics = []
        for jsonMetric in jsonMetrics:
            mBeanName = jsonMetric['mBeanName']
            attribute = jsonMetric['attribute']
            attributeType = jsonMetric['attributeType']
            metric_name = None
            if 'metricName' in jsonMetric:
                metric_name = jsonMetric['metricName']
            metric_labels = None
            if 'metricLabels' in jsonMetric:
                metric_labels = jsonMetric['metricLabels']
            attributeKey = None
            if 'attributeKey' in jsonMetric:
                attributeKey = jsonMetric['attributeKey']
            value = None
            if 'value' in jsonMetric:
                value = jsonMetric['value']

            metrics.append(JMXQuery(mBeanName, attribute, attributeKey, value, attributeType, metric_name, metric_labels))
        return metrics

    def query(self, queries: List[JMXQuery], timeout=DEFAULT_JAR_TIMEOUT) -> List[JMXQuery]:
        """
        Run a list of JMX Queries against the JVM and get the results

        :param queries:     A list of JMXQuerys to query the JVM for
        :return:            A list of JMXQuerys found in the JVM with their current values
        """
        return self.__run_query(queries, timeout)

    def call(self, method: JMXMethod, timeout=DEFAULT_JAR_TIMEOUT) -> str:
        """
        Run a list of JMX Queries against the JVM and get the results

        :param method:     A JMXMethod to be executed on the JVM
        :return:           A string represents the result of the method
        """
        return self.__run_method(method, timeout)