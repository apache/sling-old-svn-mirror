Apache Sling Webloader Sample Service UI

---------------------------------------
Sling Webloader sample - user interface
---------------------------------------
This bundle provides a sample web user interface for the Webloader Service,
which id provided by the sibling "webloader.service" bundle.

Launching Sling
===============

The Sling Container can be launched by running the following command in the 
launchpad/builder/target directory:
  java -jar org.apache.sling.launchpad-<version>-standalone.jar
so if the current version is 7, the command should be:
  java -jar org.apache.sling.launchpad-7-standalone.jar

Getting Started
===============

This component uses a Maven 3 (http://maven.apache.org/) build
environment. It requires a Java 5 JDK (or higher) and Maven (http://maven.apache.org/)
3.0.5 or later. We recommend to use the latest Maven version.

The demo bundles can be deployed through Maven using the following commands:

Deploy the Webloader Service bundle running the following command in 
the samples/webloader/service directory:
  mvn install -P autoInstallBundle
then deploy the Webloader UI bundle by running the following command in 
the samples/webloader/ui directory:
  mvn install -P autoInstallBundle

When deployed, the webloader can be accessed by navigating to:
  http://localhost:8080/bin/sling/webloader.html 
This should display the "Sling Webloader" page, that gives access to 
the Webloader service.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/samples/webloader/ui

See the Subversion documentation for other source control features.