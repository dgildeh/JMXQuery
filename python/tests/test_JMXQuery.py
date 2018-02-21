"""
Use docker compose file in this directory to spin up the test Kafka/Zookeeper cluster
when running these tests:

    docker-compose -f docker-compose-kafka.yaml up

"""
import logging, sys
from nose.tools import assert_greater_equal

from jmxquery import JMXConnection, JMXQuery, MetricType

logging.basicConfig(format='%(asctime)s %(levelname)s %(name)s - %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S',
                    stream=sys.stdout,
                    level=logging.DEBUG)

CONNECTION_URL = "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi"
jmxConnection = JMXConnection(CONNECTION_URL)

# def test_wildcard_query():
#
#     jmxQuery = [JMXQuery("*:*")]
#     metrics = jmxConnection.query(jmxQuery)
#     printMetrics(metrics)
#     assert_greater_equal(len(metrics), 4699)

def test_specific_queries():

    jmxQuery = [JMXQuery("kafka.*:type=*,name=*PerSec", None, None, )]
    metrics = jmxConnection.query(jmxQuery)
    printMetrics(metrics)

def printMetrics(metrics):
    for metric in metrics:
        metricName = metric.to_query_string()
        print(f"{metricName} ({metric.value_type}) = {metric.value}")
    print("===================\nTotal Metrics: " + str(len(metrics)))