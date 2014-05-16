Apache Sling IDE Tools: Eclipse Tests
===

The Apache Sling IDE Tools: Eclipse Tests project executes integration
tests which exercise a live Eclipse instance and a running Sling
launchpad. 

Getting Started
---

This component uses a Maven 3 (http://maven.apache.org/) build environment. 
It requires a Java 6 JDK (or higher) and Maven (http://maven.apache.org/)
3.0.5 or later. We recommend to use the latest Maven version.

Standalone execution is not possible. To run all the tests execute

    mvn clean verify

from the parent directory.

It is recommended to allocate enough memory to the PermGen in order to 
allow the tests to execute.

For instance, on Linux and MacOS X:

	export MAVEN_OPTS="-XX:MaxPermSize=512m"
	
and on Windows

	set MAVEN_OPTS="-XX:MaxPermSize=512m"

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/tooling/ide/eclipse-test

See the Subversion documentation for other source control features.
