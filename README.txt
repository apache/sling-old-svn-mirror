Apache Sling Slingshot Sample

This bundle provides a sample application for Apache Sling.
Slingshot provides a basic gallery application presenting folders as albums
and files as photos.

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
  
  
Deploy the Demo
===============

Once Sling is running you can access its Web Console from 
  http://localhost:8080/system/console
The default user name and password are: admin/admin.

Install and start the built bundle from the OSGi->Bundles page.

When deployed the application can be accessed from:
  http://localhost:8080/slingshot/albums.html
  

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/samples/slingshot

See the Subversion documentation for other source control features.
