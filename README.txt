Apache Sling API

The Sling API defines an extension to the Servlet API 2.4 to
provide access to content and unified access to request
parameters hiding the differences between the different methods
of transferring parameters from client to server. Note that the
Sling API bundle does not include the Servlet API but instead
requires the API to be provided by the Servlet container in
which the Sling framework is running or by another bundle.

Getting Started
===============

This component uses an Apache Maven (http://maven.apache.org/) build
environment. It requires a Java 6 JDK (or higher) and Maven (http://maven.apache.org/)
3.0.5 or later. We recommend to use the latest Maven version.

If you have Maven installed, you can compile and
package the jar using the following command:

    mvn package

See the Maven documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/api

See the Subversion documentation for other source control features.

