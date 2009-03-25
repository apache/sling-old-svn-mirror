Apache Sling Launchpad ServiceMix Kernel

Builds a repository of features that allows an easy deployment of Apache Sling
on Apache ServiceMix Kernel. See [1] for details.

[1] http://servicemix.apache.org/SMX4KNL/46-provisioning.html

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

If you have Maven 2 installed, you can install locally
the features repository using the following command:

    mvn clean install

See the Maven 2 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/incubator/sling/trunk/contrib/launchpad/smx-kernel

See the Subversion documentation for other source control features.


How to deploy this
-------------------

1) Install Apache ServiceMix Kernel. See details in:

	http://servicemix.apache.org/SMX4KNL/3-installation.html
	
2) Deploy your favourite JTA bundle specification. ServiceMix recommends
   geronimo-jta_1.1_spec-1.1.1. From smx console:
	
	smx@root:/> osgi/install mvn:org.apache.geronimo.specs/geronimo-jta_1.1_spec/1.1.1

   Start the bundle just installed:
	
	smx@root:/> osgi/start <bundle-pid>
   
3) Deploy your favourite OSGi HTTP Service implementation. ServiceMix 
   provides OPS4J Pax Web Service as a feature. See details in:
   
	http://servicemix.apache.org/SMX4KNL/66-installing-additional-features.html

4) Add the Sling features reposity and install:

	smx@root:/> features/addUrl mvn:org.apache.sling/org.apache.sling.launchpad.smx-kernel/2.0.0-incubator-SNAPSHOT/xml/features 
	smx@root:/> features/install sling
	
5) Browse Sling in:

	http://localhost:8080/