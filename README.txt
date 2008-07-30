Apache Sling Launchpad Webapp

This webapp contains the necessary bundles to run launchpad.

See also the "Discover Sling in 15 minutes" page at
http://incubator.apache.org/sling/site/discover-sling-in-15-minutes.html


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

    svn checkout http://svn.apache.org/repos/asf/incubator/sling/trunk/launchpad/webapp

See the Subversion documentation for other source control features.


How to run this
---------------

1) Build all Sling bundles

  cd <top of the Sling source code tree>
  mvn clean install
  
2) Build and run this

  cd launchpad/webapp
  mvn clean package jetty:run
  
Once the webapp starts, http://localhost:8888/system/console should display the Felix
OSGi console.

3) Test node creation and display
To create a node with curl:

	 curl -D - -Ftitle=something http://admin:admin@localhost:8888/testing/this
	 
Then, http://admin:admin@localhost:8888/testing/this should display a default HTML
representation, including the value of the "title" property.

Add a txt or json extension to see other output formats.

For more info see the Sling website at http://incubator.apache.org/sling, and the
"Sling in 15 minutes" tutorial 
at http://incubator.apache.org/sling/site/discover-sling-in-15-minutes.html