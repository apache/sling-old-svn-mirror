------------------------------------------------------------
Microsling: the Sling request processing, reduced to the max
------------------------------------------------------------

The goal of this prototype is to demonstrate the Sling HTTP
request processing in the simplest possible way, to help the
community converge on the goals and architecture of this 
module.

How to build and run this
------------------------
Currently this depends on the Jackrabbit 1.4-SNAPSHOT, mostly
because I had a project skeleton around that takes advantage of
JCR-955 to reuse some Jackrabbit servlets.

The sling-api module must also be available in your local
Maven repository.

To build and run:

1) Install a JDK 1.5 and Maven 2.0.7.

2) Get and build the Jackrabbit trunk from

  http://svn.apache.org/repos/asf/jackrabbit/trunk

I'm currently using revision 583722 for my tests.

3) Build the sling-api and sling-json modules

  cd sling/sling-api
  mvn clean install

  cd sling/json
  mvn clean install
  
4) Run the microsling tests:

  cd sling/microsling/microsling-core
  mvn clean install
  
Builds microsling and runs the unit and integration tests.  
  
5) Build and run this webapp:

  mvn clean jetty:run
  
Which should say "Started SelectChannelConnector@0.0.0.0:8080" once
the build is done.  
  
6) Connect to http://localhost:8080/ which should return a page
saying "Microsling homepage". That page contains instructions for
playing with Microsling.   