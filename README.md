# JMXQuery

This project provides a command line tool written in Java and packaged as a Jar to allow you to connect to and query a JMX endpoint on a Java Virtual Machine.

The command line tool can be used standalone, or with the Python module also included in this project, if you want to provide a way to query JMX from Python.

This project was originally written in December 2014 and has been used in the Outlyer monitoring agent since to provide monitoring for JVM applications via Nagios plugins.

Outlyer plugins use the Jar via the Python module to query JVM metrics via JMX and provide those for dashboards and alerts on Outlyer.

However, this module can also be used standalone with any other monitoring tool that can run shell commands or include the Python module.

There are two folders under this repo:

## Java 
This contains all the source to build and compile the Jar command line tool that connects to the JVM JMX endpoint.

## Python
This contains all the source to connect to the Jar with a simple to use Python class that handles all the communication via the command line.

## Usage
Full instructions on how to run the JAR directly on the command line are provided under the Java folder. To use the Python module just use the pip installer:

```
pip install jmxquery
```

__Note: The Python module only supports Python 3 at this time__

## Contributing

This project is released under the MIT License so you are free to use it in your own projects. However contributions are welcome and can be made via a fork/PR for review.

Issues can also be raised in this repo if you find any, so please report them here for our attention.

