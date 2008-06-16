Apache Sling Launchpad App

Standalone Launchpad Java Application. Everything needed to run
the Launchpad is included in a single JAR file.

Disclaimer
==========
Apache Sling is an effort undergoing incubation at The Apache Software Foundation (ASF),
sponsored by the Apache Jackrabbit PMC. Incubation is required of all newly accepted
projects until a further review indicates that the infrastructure, communications,
and decision making process have stabilized in a manner consistent with other
successful ASF projects. While incubation status is not necessarily a reflection of
the completeness or stability of the code, it does indicate that the project has yet
to be fully endorsed by the ASF.

Getting Started
===============

This component uses a Maven 2 (http://maven.apache.org/) build
environment. It requires a Java 5 JDK (or higher) and Maven (http://maven.apache.org/)
2.0.7 or later. We recommend to use the latest Maven version.

If you have Maven 2 installed, you can compile and
package the jar using the following command:

    mvn package

See the Maven 2 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/incubator/sling/trunk/launchpad/app

See the Subversion documentation for other source control features.


How to run the Sling launcher/app module
----------------------------------------

1) Build Sling using 

	mvn clean install
	
in the top-level directory of the Sling source code.

2) Come back to this directory and build using 

	mvn -P full clean install

3) Start the generated jar with

	 java -jar target/org.apache.sling.launcher.app-2.0.0-incubator-SNAPSHOT-full.jar 
	 
Use the correct version number instead of 2.0.0-incubator-SNAPSHOT, if needed.