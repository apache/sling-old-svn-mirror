Apache Sling Atom Tag Library

Core Tag Library for Apache Sling Atom support

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

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/contrib/scripting/jsp-taglib-atom

See the Subversion documentation for other source control features.

Dependencies
============

This bundle depends on the Apache Abdera libraries being available in the OSGi
framework. This may easiest be done by installing the Apache Abdera libraries
as bundles. To do this you must grab a SNAPSHOT which includes the fixes for
ABDERA-236.