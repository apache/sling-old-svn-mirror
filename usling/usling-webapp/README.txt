This webapp contains the necessary bundles to run usling.

How to run this
---------------

1) Build all Sling bundles

  cd <top of the Sling source code tree>
  mvn clean install
  
2) Build the usling servlets

  cd usling/usling-servlets
  mvn clean install
  cd -
  
3) Build and run this

  cd usling
  mvn clean package jetty:run
  
Once the webapp starts, http://localhost:8080/sling should display the Sling 
web console.

KNOWN PROBLEM: currently the org.apache.sling.jcr.jackrabbit.server bundle jar
must be installed manually before running the tests shown below. For some reason 
the bundle cannot be loaded automatically, see pom.xml

Use the "install or update" button of http://localhost:8080/sling/list to load
the bundle, and start it from that page.

If the bundle is missing you'll get an error saying
"No Repository available to SlingAuthenticator, cannot authenticate".

4) Test node creation and display
To create a node with curl:

	 curl -D - -Ftitle=something http://admin:admin@localhost:8080/testing/this
	 
	 
Then, http://admin:admin@localhost:8080/testing/this should display a default HTML
representation, including the value of the "title" property.

Add a txt or json extension to see other output formats.

  	 	      
