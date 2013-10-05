Apache Sling Launchpad Karaf

A repository of features and a startup handler that allow an easy deployment
of Apache Sling on Apache Karaf [1]. See [2] for details.

[1] http://karaf.apache.org
[2] http://karaf.apache.org/manual/latest/users-guide/provisioning.html

Getting Started
===============

This component uses a Maven 3 (http://maven.apache.org/) build
environment. It requires a Java 6 JDK (or higher) and Maven (http://maven.apache.org/)
3.0.4 or later. We recommend to use the latest Maven version.

If you have Maven 3 installed, you can install the features repository
and startup handler using the following command:

    mvn clean install

See the Maven 3 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.apache.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/contrib/launchpad/karaf

See the Subversion documentation for other source control features.


How to deploy this
------------------

1) Start Apache Karaf. See details in:

	http://karaf.apache.org/manual/latest/quick-start.html

2) Add the Apache Sling features repository and install:

  karaf@root()> feature:repo-add mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.1-SNAPSHOT/xml/features
  karaf@root()> feature:install sling-launchpad-jackrabbit

3) Install Launchpad content and Explorer:

  karaf@root()> feature:install sling-launchpad-content
  karaf@root()> feature:install sling-extension-explorer

4) Browse to:

	http://localhost:8181/
