Apache Sling Launchpad Builder

The Launchpad Builder project produces both a Standalone Java Application which
contains everything needed to run the Launchpad in a single JAR file and a Web
Application. It also produces a feature descriptor for Apache Felix Karaf and
a bundle list file which can be used by other projects wishing to build
customized Launchpad packages.

Getting Started
===============

This component uses a Maven 2 (http://maven.apache.org/) build environment. 
It requires a Java 5 JDK (or higher) and Maven (http://maven.apache.org/)
2.2.1 or later. We recommend to use the latest Maven version.

If you have Maven 2 installed, you can compile and
package the jar using the following command:

    mvn package

See the Maven 2 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/launchpad/builder

See the Subversion documentation for other source control features.


How to run the Sling launchpad/builder module in Standalone mode
----------------------------------------

  NOTE: "mvn clean" deletes the "sling" work directory in the project base
        directory. It is advisable to use a work directory outside of the
        project directory.

1) Build Sling using 

	mvn clean install
	
in the top-level directory of the Sling source code.

2) Start the generated jar with

	 java -jar target/org.apache.sling.launchpad-6-standalone.jar 
	 
Use the correct version number instead of 6, if needed.

3) Browse Sling in:

        http://localhost:8080

How to run the Sling launchpad/builder module in webapp mode
----------------------------------------

1) Build Sling using 

	mvn clean install
	
in the top-level directory of the Sling source code.

2) Start the generated war with

	 mvn jetty:run

3) Browse Sling in:

        http://localhost:8080
        
  OR
  
   Deploy target/org.apache.sling.launchpad-6.war to your favorite application
   server or servlet container.
