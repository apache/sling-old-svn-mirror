Scala for Sling Demo Application - Forum

Getting Started
===============

This component uses a Maven 2 (http://maven.apache.org/) build
environment. It requires a Java 5 JDK (or higher) and Maven 
(http://maven.apache.org/) 2.2.1 or later. We recommend to use 
the latest Maven version.

If you have Maven 2 installed, you can compile and
package the jar using the following command:

    mvn package
    
To Install the package in a running Sling instance use:

    mvn sling:install

See the Maven 2 documentation for other build features.


Before running the example make sure to install the Scala scripting
engine for Sling and the path-based-rtp bundle from the samples directory 
of the Sling distribution. 

After installing these bundles the forum application is available at:

    http://localhost:8080/content/forum.html?sling:authRequestLogin=true


Known issue:
- SLING-1895: sun.reflect.MethodAccessorImpl not correctly boot delegated
  https://issues.apache.org/jira/browse/SLING-1895
