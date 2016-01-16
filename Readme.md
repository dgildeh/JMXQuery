JMX Query
=========

A simple jar to query JMX data from a JVM and return in a format that can easily be used in Nagios check scripts.

Requires Java 1.5 or above.


Usage
------

`java -jar jmxquery.jar [-url] [-proc] [-username] [-password] [-metrics] [-list] [-incjvm] [-json] [-help]`

options:

    -help
        Prints help

    -url
        (Required) JMX URL, for example: "service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi"

    -proc
        (Required if -url isn't provided) Local JVM Process name to connect to via JMX, for example: "org.netbean.Main"
        **(NOTE: This requires classpath to JDK tools.jar set when running)**

    -username
        (Optional) jmx username if authentication is enabled

    -password
        (Optional) jmx password if authentication is enabled

    -metrics
        List of metrics to fetch in following format: {metric}={mBeanName}/{attribute}/{attributeKey};
        For example: "jvm.memory.heap.used=java.lang:type=Memory/HeapMemoryUsage/used"
        {attributeKey} is optional and only used for Composite metric types. Use semi-colon to separate metrics.
        If a token is used in the {metric} it will be replaced on output with the MBean property. See usage example below.

    -list [listType] [query]
        Will list out the following depending on listType given:
            * jvms: Will list all local JVMs with their JMX connection URLs or null if none **(NOTE: This requires classpath to JDK tools.jar set when running)**
            * mbeans: Will show full JMX tree with all mbeans and their attributes. This option requires the [query] parameter to filter. Use '*:*' to list everything

    -incjvm
        (Optional) Will add all standard JVM metrics to the -metrics query if used under java.lang domain.
        Useful utility function to add JVM metrics quickly and also for testing connections if used by itself

    -json
        (Optional) Will output everything in JSON format, otherwise will be human readable text. Useful for passing output to scripts.

Example Usage
-------------

### Listing available metrics

List all metrics:

`java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -list mbeans '*:*'`

To filter on a particular domain so you only see the JMX metrics available under that (i.e. java.lang) you can use the following command:

`java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -list mbeans java.lang:*`

If you want to filter on attribute name you could use the following query:

`java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -list mbeans '*:*/HeapMemoryUsage'`

This will list any MBean attributes that have that attribue name in the JVM.

### Get a metric value

`java -jar JMXQuery.jar -url service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -metrics jvm.classloading.loadedclasscount=java.lang:type=ClassLoading/LoadedClassCount`

You can get multiple values by joining the mbeans together with semi colons.

`java -jar JMXQuery.jar -url service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -metrics jvm.classloading.loadedclasscount=java.lang:type=ClassLoading/LoadedClassCount;
jvm.classloading.unloadedclasscount=java.lang:type=ClassLoading/UnloadedClassCount`


### Complex Queries

Note the same queries use for filtering searches when listing metrics can also be used for fetching metric values to get lots of values in one search. For example there
are several Garbage Collector processes with attributes you can read. Instead of needing to know the exact process names, you can do a query and use a [token] in the name
which will be replaced by the MBean property.

I.e. jvm.gc.[name].collectiontime for the MBean properly java.lang:type=GarbageCollector,name=*ConcurrentMarkSweep* will become jvm.gc.ConcurrentMarkSweep.collectiontime
when the values out outputted by the tool.

This example will list all the collectiontime and collectioncount attribute values for all the GarbageCollector processes running in the JVM:

`java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -metrics "jvm.gc.[name].collectiontime=java.lang:type=GarbageCollector,*/CollectionTime;
jvm.gc.[name].collectioncount=java.lang:type=GarbageCollector,*/CollectionCount"`
   
This will output the following:

```
jvm.gc.ConcurrentMarkSweep.collectiontime=123
jvm.gc.ParNew.collectiontime=131
jvm.gc.ConcurrentMarkSweep.collectioncount=1
jvm.gc.ParNew.collectioncount=12
```

Building the Jar
----------------

Simply run the ./build.sh, modifying the build parameters for your environment in the script. This will compile the code for Java 1.5 and build the Jar ready to run.

License & Credits
-----------------

This tool was inspired by https://code.google.com/p/jmxquery/ but has been completely rewritten by David Gildeh (Dataloop.IO).

It is licensed under Apache 2 License (http://www.apache.org/licenses/LICENSE-2.0)