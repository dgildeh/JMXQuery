JMX Query
=========

A simple jar to query JMX data from a JVM and return in a format that can easily be used in Nagios check scripts.

Requires Java 1.5 or above.


Usage
------

```
jmxquery [-url] [-username,u] [-password,p] [-query,q] [-incjvm] [-json] [-help]
```

options are:

-help, h
	Prints help page
	
-url 
	JMX URL, for example: "service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi"
	
-username, u
	jmx username if required
	
-password, p
	jmx password if required

-query, q
        List of metrics to fetch in following format: {mBeanName}/{attribute}/{attributeKey};
        For example: "java.lang:type=Memory/HeapMemoryUsage/used"
        {attributeKey} is optional and only used for Composite metric types. 
        Use semi-colon to separate metrics.

-incjvm
        Will add all standard JVM metrics to the -metrics query if used under java.lang domain
        Useful utility function to add JVM metrics quickly and also for testing connections if
        used by itself

-json
        Will output everything in JSON format, otherwise will be human readable text. Useful
        for passing output to scripts.

Example Usage
-------------

### Listing available metrics

List all metrics:

```
java -jar jmxquery.jar -url service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -q "*:*"
```

To filter on a particular domain so you only see the JMX metrics available under that (i.e. java.lang) you can use the following command:

```
java -jar jmxquery.jar -url service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -q "java.lang:*"
```

If you want to filter on attribute name you could use the following query:

```
java -jar jmxquery.jar -url service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -q "*:*/HeapMemoryUsage"
```

This will list any MBean attributes that have that attribue name in the JVM.

### Get a metric value

```
java -jar JMXQuery.jar -url service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -q "java.lang:type=ClassLoading/LoadedClassCount"
```

You can get multiple values by joining the mbeans together with semi colons.

```
java -jar JMXQuery.jar -url service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi -q "java.lang:type=ClassLoading/LoadedClassCount;java.lang:type=ClassLoading/UnloadedClassCount"
```

Building the Jar
----------------

Simply run the ./build.sh, modifying the build parameters for your environment in the script. This will compile the code for Java 1.5 and build the Jar ready to run.

License & Credits
-----------------

This tool was inspired by https://code.google.com/p/jmxquery/ but has been completely rewritten by David Gildeh from Outlyer (www.outlyer.com).

It is licensed under the MIT License (https://opensource.org/licenses/MIT)
