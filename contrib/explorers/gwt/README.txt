Apache Sling Explorer

This bundle provides the Sling Explorer Application to manage all Sling resources. 


Getting Started
===============

1/ GWT 

We are using Google Web Toolkit for building this web app (see on http://code.google.com/intl/fr-FR/webtoolkit/). 
For testing the application, it is not necessary to redeploy all the time the application . You can test it directly 
from the project directory thanks to the GWT hosted mode and the mountByFS Sling option. 

Before executing the following maven commands, check if the Sling Bundle "Filesystem Resource Provider" is installed. 
If this bundle is not installed, this is not possible to test the application from the project folder without redeployment. 



2/ Using Maven 

This application uses a Maven 2 (http://maven.apache.org/) build
environment. It requires a Java 5 JDK (or higher) and Maven (http://maven.apache.org/)
2.0.7 or later. We recommend to use the latest Maven version.

You can use the following maven commands :

	1/ mvn install -P dev  : Initialize the local Sling server. This command will mount your project folder into the Sling local repository. 
	You have to call this command only once time. After that, you are ready to test & dedug the application from your project folder. 
	
	2/ mvn gwt:run : run the application in the hosted mode.
	
    3/ mvn gwt:debug : start the application in hosted/debug mode. By default, you can start the debugger from your IDE on the port 8888.
    
    4/ mvn gwt:compile : compile the GWT application.
    
    5/ mvn package : build the OSGI bundle 
    
     
After installing the GWT application on the Sling server, you can also access to it from http://localhost:8080/apps/explorer/index.html but it is faster to use the GWT hosted mode.
  
   
See the Maven 2 documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/contrib/explorers/gwt

See the Subversion documentation for other source control features.

Running the Explorer
====================
After installing the Explorer, http://localhost:8080/apps/explorer/index.html should display
its home page, which says "Sling Explorer" at the top and displays a tree with a Resources
root.
