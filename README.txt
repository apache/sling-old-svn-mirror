Apache Sling System Bundle Extension: Activation API

Adds the Activation API package to the system bundle exports. The list of packages
is derived from the packages available in the Java 6 platform. To use more
recent Activation API either modify this bundle to also export those package from
the platform or install respective API bundles.

Getting Started
===============

This component uses a Maven 2 (http://maven.apache.org/) build
environment. It requires a Java 5 JDK (or higher) and Maven (http://maven.apache.org/)
2.2.1 or later. We recommend to use the latest Maven version.

If you have Maven 2 installed, you can compile and
package the jar using the following command:

    mvn package

See the Maven 2 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout https://svn.apache.org/repos/asf/sling/trunk/bundles/extensions/framework-extension-activation

See the Subversion documentation for other source control features.
