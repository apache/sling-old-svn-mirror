Apache Sling Launchpad Testing module
=====================================

This module builds a Sling instance using bundles from the trunk, and
runs integration tests against it, via HTTP.

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

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/launchpad/webapp

See the Subversion documentation for other source control features.

Default build with integration tests
------------------------------------
This module runs number of integration tests provided by the sibling 
integration-tests module. By default the instance is started, the integration
tests are executed and the instance is stopped.

Executing individual tests
--------------------------
To run individual tests against this instance, with the exact same setup used
by the full build use

  mvn clean install -Dlaunchpad.keep.running=true -Dhttp.port=4502 -Ddebug

The -Ddebug option enables server-side debugging of the instance under test, 
on port 8000. It can be omitted, of course.

Use CTRL-C to stop that instance.

See the README.txt in the integration-tests module for how to run specific 
tests against that instance.
