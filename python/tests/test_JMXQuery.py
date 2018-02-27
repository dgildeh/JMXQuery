"""
Use docker compose file in this directory to spin up the test Kafka/Zookeeper cluster
when running these tests:

    docker-compose -f docker-compose-kafka.yaml up

"""
import logging, sys
import threading
from nose.tools import assert_greater_equal

from jmxquery import JMXConnection, JMXQuery, MetricType

logging.basicConfig(format='%(asctime)s %(levelname)s %(name)s - %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S',
                    stream=sys.stdout,
                    level=logging.DEBUG)

CONNECTION_URL = "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi"

def test_wildcard_query():

    jmxConnection = JMXConnection(CONNECTION_URL)
    jmxQuery = [JMXQuery("*:*")]
    metrics = jmxConnection.query(jmxQuery)
    printMetrics(metrics)
    assert_greater_equal(len(metrics), 4699)

def test_kafka_plugin():

    jmxConnection = JMXConnection(CONNECTION_URL)
    jmxQuery = [
        # UnderReplicatedPartitions
        JMXQuery("kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions/Value",
                 metric_name="kafka_server_ReplicaManager_UnderReplicatedPartitions"),

        # OfflinePartitionsCount
        JMXQuery("kafka.controller:type=KafkaController,name=OfflinePartitionsCount/Value",
                 metric_name="kafka_controller_KafkaController_OfflinePartitionsCount"),

        # ActiveControllerCount
        JMXQuery("kafka.controller:type=KafkaController,name=ActiveControllerCount/Value",
                 metric_name="kafka_controller_KafkaController_ActiveControllerCount"),

        # MessagesInPerSec
        JMXQuery("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec/Count",
                 metric_name="kafka_server_BrokerTopicMetrics_MessagesInPerSec_Count"),

        # BytesInPerSec
        JMXQuery("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec/Count",
                 metric_name="kafka_server_BrokerTopicMetrics_BytesInPerSec_Count"),

        # BytesOutPerSec
        JMXQuery("kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec/Count",
                 metric_name="kafka_server_BrokerTopicMetrics_BytesOutPerSec_Count"),

        # RequestsPerSec
        JMXQuery("kafka.network:type=RequestMetrics,name=RequestsPerSec,request=*/Count",
                 metric_name="kafka_network_RequestMetrics_RequestsPerSec_Count",
                 metric_labels={"request": "{request}"}),

        # TotalTimeMs
        JMXQuery("kafka.network:type=RequestMetrics,name=TotalTimeMs,request=*",
                 metric_name="kafka_network_RequestMetrics_TotalTimeMs_{attribute}",
                 metric_labels={"request": "{request}"}),

        # LeaderElectionsPerSec
        JMXQuery("kafka.controller:type=ControllerStats,name=LeaderElectionRateAndTimeMs/Count",
                 metric_name="kafka_cluster_ControllerStats_LeaderElectionRateAndTimeMs_Count"),

        # UncleanLeaderElectionsPerSec
        JMXQuery("kafka.controller:type=ControllerStats,name=UncleanLeaderElectionsPerSec/Count",
                 metric_name="kafka_cluster_ControllerStats_UncleanLeaderElectionsPerSec_Count"),

        # PartitionCount
        JMXQuery("kafka.server:type=ReplicaManager,name=PartitionCount/Value",
                 metric_name="kafka_server_ReplicaManager_PartitionCount"),

        # ISRShrinkRate
        JMXQuery("kafka.server:type=ReplicaManager,name=IsrShrinksPerSec",
                 metric_name="kafka_server_ReplicaManager_IsrShrinksPerSec_{attribute}"),

        # ISRExpandRate
        JMXQuery("kafka.server:type=ReplicaManager,name=IsrExpandsPerSec",
                 metric_name="kafka_server_ReplicaManager_IsrExpandsPerSec_{attribute}"),

        # NetworkProcessorAvgIdlePercent
        JMXQuery("kafka.network:type=SocketServer,name=NetworkProcessorAvgIdlePercent/Value",
                 metric_name="kafka_network_SocketServer_NetworkProcessorAvgIdlePercent"),

        # RequestHandlerAvgIdlePercent
        JMXQuery("kafka.server:type=KafkaRequestHandlerPool,name=RequestHandlerAvgIdlePercent",
                 metric_name="kafka_server_KafkaRequestHandlerPool_RequestHandlerAvgIdlePercent_{attribute}"),

        # ZooKeeperDisconnectsPerSec
        JMXQuery("kafka.server:type=SessionExpireListener,name=ZooKeeperDisconnectsPerSec",
                 metric_name="kafka_server_SessionExpireListener_ZooKeeperDisconnectsPerSec_{attribute}"),

        # ZooKeeperExpiresPerSec
        JMXQuery("kafka.server:type=SessionExpireListener,name=ZooKeeperExpiresPerSec",
                 metric_name="kafka_server_SessionExpireListener_ZooKeeperExpiresPerSec_{attribute}"),

    ]

    metrics = jmxConnection.query(jmxQuery)
    printMetrics(metrics)
    assert_greater_equal(len(metrics), 525)

def test_threading():
    """
    This test is to check that we don't get any threading issues as the module will be
    run in concurrent plugin threads
    """
    list_threads = []
    for x in range(0, 10):
        t = threading.Thread(target=test_kafka_plugin)
        t.daemon = True
        list_threads.append(t)
        t.start()
    sys.stdout.write("All threads started.\n")

    for t in list_threads:
        t.join()  # Wait until thread terminates its task
    sys.stdout.write("All threads completed.\n")

def printMetrics(metrics):
    for metric in metrics:
        if metric.metric_name:
            print(f"{metric.metric_name}<{metric.metric_labels}> == {metric.value}")
        else:
            print(f"{metric.to_query_string()} ({metric.value_type}) = {metric.value}")

    print("===================\nTotal Metrics: " + str(len(metrics)))