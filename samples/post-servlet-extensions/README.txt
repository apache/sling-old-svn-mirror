Apache Sling Sample Post Servlet Extensions

This bundle provides sample OSGI services which extend the sling post 
servlet behaviour.

The default POST behavior of Sling can be changed by registering
custom servlets that process POST requests for specific paths and/or
node types.

Another, often simpler, way of customizing the POST behavior is to
register OSGi services for the two extension points that the
SlingPostServlet provides: the SlingPostOperation and the
SlingPostProcessor.

The SlingPostOperation interface is meant to create new operations in
addition to the standard (:operation=delete, :operation=move. etc.)
ones.

The SlingPostProcessor is a called by the SlingPostServlet after the
(standard or custom) SlingPostOperation selected by the request is
called.

These two extension points allow the POST behavior to be customized
easily, with small amounts of code and while reusing most of the
standard behavior.


Getting Started
===============

This component uses a Maven 3 (http://maven.apache.org/) build
environment. It requires a Java 5 JDK (or higher) and Maven (http://maven.apache.org/)
3.0.5 or later. We recommend to use the latest Maven version.

If you have Maven 3 installed, you can compile and
package the jar using the following command:

    mvn package

See the Maven 3 documentation for other build features.

Launching Sling
===============

The Sling Container can be launched by running the following command in the 
launchpad/builder/target directory:
  java -jar org.apache.sling.launchpad-<version>-standalone.jar
so if the current version is 7, the command should be:
  java -jar org.apache.sling.launchpad-7-standalone.jar


The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/samples/post-servlet-extensions

See the Subversion documentation for other source control features.