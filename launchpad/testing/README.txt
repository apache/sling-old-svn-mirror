Apache Sling Launchpad Testing module

This module builds a Sling webapp using bundles from the trunk, and
runs integration tests (that were previously in the launchpad/webapp
module) on them.  


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

  cd launchpad/testing
  mvn clean package jetty:run
  
Once the webapp starts, http://localhost:8888/system/console should display the Felix
OSGi console.

4) Test node creation and display
To create a node with curl:

	 curl -D - -Ftitle=something http://admin:admin@localhost:8888/testing/this
	 
Then, http://admin:admin@localhost:8888/testing/this should display a default HTML
representation, including the value of the "title" property.

Add a txt or json extension to see other output formats.

Integration tests
-----------------
This module provides a number of integration tests, that run automatically when
doing a full build, and test Sling via its client HTTP interfaces.

These tests can also be run against another instance of Sling, for example to
test it in another web container than the embedded Jetty that is used during the
Maven build.

See pom.xml for the parameters that control these integration tests. Here's an
example of running them against a Sling instance running on host xyzzy, port 1234,
with the Sling webapp mounted under /foo:

   mvn -o -s /dev/null test \
    -Dhttp.port=1234 \
    -Dtest.host=xyzzy \
    -Dhttp.base.path=foo \
    -Dwebdav.workspace.path=foo/dav/default \
    -Dtest=**/integrationtest/**/*Test.java

The  -s /dev/null parameter disables all your local Maven settings, to make sure
they don't interfere. Feel free to remove that if you know what you're doing.

To run a single test, other values can be used for the "-Dtest" parameter.

This is very useful during development: you can leave a Sling webapp instance
running, update bundles in it using the mvn autoInstallBundle profile, and run
specific integration tests quickly from another terminal.

JCR Install Integration tests
-----------------------------

This module also contains integration tests for the JCR Install module. These
tests are disabled by default, since they may not be of general interest. To
include the JCR Install tests in the integration tests run enable the
jcrinstall-tests profile such as in

   mvn -P jcrinstall-tests integration-test
   
*** WARNING: the jcrinstall-tests do not work in revision 741168, due to changes
in the start levels. They can be run according to the scenario below, if you 
change the default bundles start level to 30, after starting the Sling instance
with mvn jetty:run, from http://localhost:8888/system/console/vmstat. ***     

To run the jcrinstall integration tests quickly, without having to start the
webapp first:

1. Run mvn clean package jetty:run -P jcrinstall-tests in a first console

(1.5 see WARNING above - adjust the default bundles start level)

2. In another console run

  mvn test -Pjcrinstall-tests  -Dtest=**/integrationtest/jcrinstall/**/*Test.java
  
       
