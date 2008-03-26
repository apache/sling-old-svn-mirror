This webapp contains the necessary bundles to run launchpad.

See also the "Discover Sling in 15 minutes" page at
http://incubator.apache.org/sling/site/discover-sling-in-15-minutes.html

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
  
Once the webapp starts, http://localhost:8888/sling should display the Sling 
web console.

4) Test node creation and display
To create a node with curl:

	 curl -D - -Ftitle=something http://admin:admin@localhost:8888/testing/this
	 
Then, http://admin:admin@localhost:8888/testing/this should display a default HTML
representation, including the value of the "title" property.

Add a txt or json extension to see other output formats.

Integration tests
-----------------
This module provides a number of integration tests, that run automatically when
doing a full build, and test Sling via its client HTTP interfaces.

These tests can also be run against another instance of Sling, for example to
test it in another web container than the embedded Jetty that is used during the
Maven build.

See pom.xml for the parameters that control these integration tests. Here's an
example of running them against a Sling instance running on host xyzzy, port 1234,
with the Sling webapp mounted under /foo:

   mvn -o -s /dev/null test \
    -Dhttp.port=1234 \
    -Dtest.host=xyzzy \
    -Dhttp.base.path=foo \
    -Dwebdav.workspace.path=foo/dav/default \
    -Dtest=**/integrationtest/**/*Test.java

The  -s /dev/null parameter disables all your local Maven settings, to make sure
they don't interfere. Feel free to remove that if you know what you're doing.

To run a single test, other values can be used for the "-Dtest" parameter.
