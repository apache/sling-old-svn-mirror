Apache Sling
============

Bringing Back the Fun!

Apache Sling is a web framework that uses a Java
Content Repository, such as Apache Jackrabbit, to store and manage content.

Sling applications use either scripts or Java servlets, selected based on
simple name conventions, to process HTTP requests in a RESTful way.

The embedded Apache Felix OSGi framework and console provide a dynamic
runtime environment, where  code and content bundles can be loaded, unloaded
and reconfigured at runtime.

As the first web framework dedicated to JSR-170 Java Content Repositories,
Sling makes it very simple to implement simple applications, while providing
an enterprise-level framework for more complex applications.

See http://sling.apache.org for more information.

Getting started
---------------

You need a Java 6 (or higher) JDK and Maven 3 (http://maven.apache.org/,
version 3.0.4 or higher) to build Sling.

Once you have everything in place, run

    export MAVEN_OPTS="-Xmx256M -XX:MaxPermSize=256M" 
    mvn clean install

in this directory (on 64bit platforms you might want to use 512M instead
of 256M). This will build, test and install the Sling modules in your local
Maven repository.

Some modules might not be listed in the pom.xml found in this directory,
those won't be built by the above command. If you need one of these 
modules, run "mvn clean install" in the directory that contains its
pom.xml file.

To get started with Sling, start the launchpad/builder module,
see README.txt in that module's directory.

