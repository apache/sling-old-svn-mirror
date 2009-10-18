Apache Sling Launchpad Karaf

Builds a repository of features that allows an easy deployment of Apache Sling
on Apache Felix Karaf [1]. See [2] for details.

[1] http://felix.apache.org/site/apache-felix-karaf.html
[2] http://felix.apache.org/site/46-provisioning.html

Getting Started
===============

This component uses a Maven 2 (http://maven.apache.org/) build
environment. It requires a Java 5 JDK (or higher) and Maven (http://maven.apache.org/)
2.0.7 or later. We recommend to use the latest Maven version.

If you have Maven 2 installed, you can install locally
the features repository using the following command:

    mvn clean install

See the Maven 2 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/contrib/launchpad/karaf

See the Subversion documentation for other source control features.


How to deploy this
-------------------

1) Install Apache Felix Karaf. See details in:

	http://felix.apache.org/site/3-installation.html
	
2) Deploy your favourite JTA bundle specification. Apache Karaf recommends
   geronimo-jta_1.1_spec-1.1.1. From smx console:
	
	karaf@root:/> osgi:install mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1

   Start the bundle just installed:
	
	karaf@root:/> osgi:start <bundle-pid>
   
3) Install the http feature in order to get an HTTP OSGi service available:
   
	karaf@root:/> features:install http

4) Add the Sling features reposity and install:

	karaf@root:/> features:addUrl mvn:org.apache.sling/org.apache.sling.launchpad.karaf/1.0.0-SNAPSHOT/xml/features 
	karaf@root:/> features:install sling
	
5) Browse Sling in:

	http://localhost:8181/