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

    svn checkout http://svn.apache.org/repos/asf/incubator/sling/trunk/samples/post-servlet-extensions

See the Subversion documentation for other source control features.