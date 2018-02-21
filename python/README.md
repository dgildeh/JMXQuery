# JMXQuery Python Module

Provides a Python module to easily run queries and collect metrics from a Java Virtual Machine via JMX.

In order to use this module, provide a list of queries, and the module will return all of the values it 
finds matching the query. Please note that the interfact to the JMX uses a small jar file contained in 
this module, so you will need to have java installed on the machine you're running this module on.

## Usage

This example query for a Kafka server will get all cluster partition metrics:

```
jmxConnection = JMXConnection("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi")
jmxQuery = [JMXQuery("kafka.cluster:type=*,name=*,topic=*,partition=*",
                         metric_name="kafka_cluster_{type}_{name}",
                         metric_labels={"topic" : "{topic}", "partition" : "{partition}"})]
metrics = jmxConnection.query(jmxQuery)
for metric in metrics:
    print(f"{metric.metric_name}<{metric.metric_labels}> == {metric.value}")
```

This will return the following:

```
kafka_cluster_Partition_UnderReplicated<{'partition': '0', 'topic': 'test'}> == 0
kafka_cluster_Partition_UnderMinIsr<{'partition': '0', 'topic': 'test'}> == 0
kafka_cluster_Partition_InSyncReplicasCount<{'partition': '0', 'topic': 'test'}> == 1
kafka_cluster_Partition_ReplicasCount<{'partition': '0', 'topic': 'test'}> == 1
kafka_cluster_Partition_LastStableOffsetLag<{'partition': '0', 'topic': 'test'}> == 0
```

As you will notice you can optionally send a metric_name and metric_labels with {} tokens in them. These
tokens are replaced at runtime by the jar so you can easily build metric names with associated labels using
the MBean properties of the values your query pulls back. 

You can also use the module to pull back a list of all the MBean values available in the JVM too:

```
jmxConnection = JMXConnection("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi")
jmxQuery = [JMXQuery("*:*")]
metrics = jmxConnection.query(jmxQuery)
for metric in metrics:
    print(f"{metric.to_query_string()} ({metric.value_type}) = {metric.value}")
```

## Installation

Just use pip to install the module in your Python environment:

```
pip install jmxquery
```