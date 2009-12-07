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

Before running the example make sure to install the path-based-rtp 
bundle from the samples directory of the Sling distribution. 

After installing the package and the path-based-rtp the forum application
is available at the following URL:

    http://localhost:8080/content/forum.html?sling:authRequestLogin=true
    
For more information on Scala for Sling see:

    http://people.apache.org/~mduerig/scala4sling/    
    

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/contrib/scripting/scala/

See the Subversion documentation for other source control features.

