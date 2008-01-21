This webapp contains the necessary bundles to run launchpad.

How to run this
---------------

1) Build all Sling bundles

  cd <top of the Sling source code tree>
  mvn clean install
  
2) Build the launchpad servlets

  cd launchpad/launchpad-servlets
  mvn clean install
  cd -
  
3) Build and run this

  cd launchpad/launchpad-webapp
  mvn clean package jetty:run
  
Once the webapp starts, http://localhost:8080/sling should display the Sling 
web console.

4) Test node creation and display
To create a node with curl:

	 curl -D - -Ftitle=something http://admin:admin@localhost:8080/testing/this
	 
Then, http://admin:admin@localhost:8080/testing/this should display a default HTML
representation, including the value of the "title" property.

Add a txt or json extension to see other output formats.

We hope to have a "Getting started with the Launchpad" document on the Sling website
(http://incubator.apache.org/sling/) soon - but it's not there yet. 

  	 	      
