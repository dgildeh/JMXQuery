#!/bin/bash

# Compiles and packages JMXQuery into jmxquery.jar
# Will compile to ensure compatibility with Java 1.5 and above
# Assumes JDK 1.6 is installed on MacOS

# Declare classpath to classes.jar (tools.jar in newer JDKs)
JDK_CLASSPATH="/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes/classes.jar"
# Declare path to JDK 1.6 javac
JDK_HOME="/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/bin/javac"

# Build Options
JAR_DESTINATION="./dist/jmxquery.jar"
BUILD_DIR="./buildjar"

# Compile classes
echo ** COMPILING CLASSES
mkdir $BUILD_DIR
find ./src -name "*.java" -print | xargs $JDK_HOME -target 1.5 -g -classpath $JDK_CLASSPATH -d $BUILD_DIR
cp ./src/io/dataloop/jmx/jmxquery/HELP $BUILD_DIR/io/dataloop/jmx/jmxquery/HELP

# Build jmxquery.jar
echo ** BUILDING JAR: jar cvfm $JAR_DESTINATION MANIFEST.MF -C $BUILD_DIR .
jar cvfm $JAR_DESTINATION MANIFEST.MF -C $BUILD_DIR .

# Delete build directory
rm -r $BUILD_DIR