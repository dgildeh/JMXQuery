JMX Query
=========

This is a simple jar that can be run from command line to connect to a JVM's JMX port
(either locally or remote) and query what metrics are available and also get the values 
of those metrics in bulk.

It is intended for use with Nagios check scripts, such as those run in Dataloop.IO, as a
bridge between the script and JMX to pull out the necessary metrics. This will enable you
to pull out all the metrics for anything that runs on the JVM as long as you know the 
domain/metrics you want (a handy -list option is provided to query the JVM so you can 
see what metrics are available while you develop your script) so you can monitor services
such as Tomcat, Cassandra and Kafka on your servers from a Nagios check script.

The final Jar should be compatible with Java 1.5 and above. If you want to use the "-proc"
or "-list jvms" options you will need to have the JDK installed on your machine and set
the classpath to your "classes.jar" or "tools.jar" in newer versions so it can use the 
JDK libraries to list all the local JVMs.

If not, you should be able to use it with the standard Java runtime on your machine as long
as you don't call these options when you run the jar from command line.

Usage
------

> java -jar jmxquery.jar [-url] [-proc] [-username] [-password] [-metrics] [-list] [-incjvm] [-json] [-help]
>
> options are:
>
>> -help 
>>> Prints help
>>
>> -url 
>>> (Required) JMX URL, for example: "service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi"
>>
>> -proc
>>> (Required if -url isn't provided) Local JVM Process name to connect to via JMX, for example: "org.netbean.Main"
>>> **(NOTE: This requires classpath to JDK tools.jar set when running)**
>>
>> -username
>>> (Optional) jmx username if authentication is enabled
>>
>> -password 
>>> (Optional) jmx password if authentication is enabled
>>
>> -metrics
>>> List of metrics to fetch in following format: {metric}={mBeanName}/{attribute}/{attributeKey};
>>> For example: "jvm.memory.heap.used=java.lang:type=Memory/HeapMemoryUsage/used"
>>> {attributeKey} is optional and only used for Composite metric types. Use semi-colon to separate metrics.
>>> If a token is used in the {metric} it will be replaced on output with the MBean property. See usage example below.
>>
>> -list [listType] [query]
>>> Will list out the following depending on listType given:
>>>>
>>>> * jvms: Will list all local JVMs with their JMX connection URLs or null if none **(NOTE: This requires classpath to JDK tools.jar set when running)**
>>>> * mbeans: Will show full JMX tree with all mbeans and their attributes. This option requires the [query] parameter to filter. Use '*:*' to list everything
>>
>> -incjvm
>>> (Optional) Will add all standard JVM metrics to the -metrics query if used under java.lang domain. 
Useful utility function to add JVM metrics quickly and also for testing connections if used by itself
>>
>> -json
>>> (Optional) Will output everything in JSON format, otherwise will be human readable text. Useful for passing output to scripts.

Example Usage
-------------

The jmxquery.py provided in the root directory contains an example script with some examples of using a script to get and read JMX values.

### Listing available metrics

To browse what metrics are available when writing a script you can use JMX MBean queries to list metrics available. For example the following 
command will list every single metric available via JMX (warning this could be really long on some JVMs!):

> java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -list mbeans '*:*'

To filter on a particular domain so you only see the JMX metrics available under that (i.e. java.lang) you can use the following command:

> java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -list mbeans java.lang:*

If you want to filter on attribute name you could use the following query:

> java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -list mbeans '*:*/HeapMemoryUsage'

This will list any MBean attributes that have that attribue name in the JVM.

### Reading metric values (Simple Example)

To get the Java classcount metrics you would use the following command:

> java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -metrics "jvm.classloading.loadedclasscount=java.lang:type=ClassLoading/LoadedClassCount;
> jvm.classloading.unloadedclasscount=java.lang:type=ClassLoading/UnloadedClassCount;jvm.classloading.totalloadedclasscount=java.lang:type=ClassLoading/TotalLoadedClassCount;"

This will return the values in the format below:

> jvm.classloading.loadedclasscount=2100
> jvm.classloading.unloadedclasscount=201
> jvm.classloading.totalloadedclasscount=2201

**If you were to use the -json option you could output these values as a JSON array which can then be read via a Nagios script easily.**

### Reading metric values (Complex Example)

Note the same queries use for filtering searches when listing metrics can also be used for fetching metric values to get lots of values in one search. For example there
are several Garbage Collector processes with attributes you can read. Instead of needing to know the exact process names, you can do a query and use a [token] in the name
which will be replaced by the MBean property.

I.e. jvm.gc.[name].collectiontime for the MBean properly java.lang:type=GarbageCollector,name=*ConcurrentMarkSweep* will become jvm.gc.ConcurrentMarkSweep.collectiontime
when the values out outputted by the tool.

This example will list all the collectiontime and collectioncount attribute values for all the GarbageCollector processes running in the JVM:

> java -jar jmxquery.jar service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -metrics "jvm.gc.[name].collectiontime=java.lang:type=GarbageCollector,*/CollectionTime;
> jvm.gc.[name].collectioncount=java.lang:type=GarbageCollector,*/CollectionCount"
   
This will output the following:

> jvm.gc.ConcurrentMarkSweep.collectiontime=123
> jvm.gc.ParNew.collectiontime=131
> jvm.gc.ConcurrentMarkSweep.collectioncount=1
> jvm.gc.ParNew.collectioncount=12

Building the Jar
----------------

Simply run the ./build.sh, modifying the build parameters for your environment in the script. This will compile the code for Java 1.5 and build the Jar ready to run.