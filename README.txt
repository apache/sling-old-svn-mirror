Apache Sling
============

Bringing Back the Fun!

Apache Sling (currently in incubation) is a web framework that uses a Java
Content Repository, such as Apache Jackrabbit, to store and manage content.

Sling applications use either scripts or Java servlets, selected based on
simple name conventions, to process HTTP requests in a RESTful way.

The embedded Apache Felix OSGi framework and console provide a dynamic
runtime environment, where  code and content bundles can be loaded, unloaded
and reconfigured at runtime.

As the first web framework dedicated to JSR-170 Java Content Repositories,
Sling makes it very simple to implement simple applications, while providing
an enterprise-level framework for more complex applications.

See http://incubator.apache.org/sling for more information.

Getting started
---------------

Requires a Java 5 JDK and Maven (http://maven.apache.org/) 2.0.7 or later.

To build Sling, run

  mvn clean install

in this directory. This will build, test and install the Sling modules
in your local Maven repository.

Some modules might not be listed in the pom.xml found in this directory,
those won't be built by the above command. If you need one of these 
modules, run "mvn clean install" in the directory that contains its
pom.xml file.

To get started with Sling, start the launchpad/launchpad-webapp module,
see README.txt in that module's directory.

Disclaimer
==========

Apache Sling is an effort undergoing incubation at The Apache Software
Foundation (ASF), sponsored by the Apache Jackrabbit PMC. Incubation is
required of all newly accepted projects until a further review indicates that
the infrastructure, communications, and decision making process have
stabilized in a manner consistent with other successful ASF projects. While
incubation status is not necessarily a reflection of the completeness or
stability of the code, it does indicate that the project has yet to be fully
endorsed by the ASF.
