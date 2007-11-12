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

- Install a JDK 1.5 and Maven 2.0.7.

- Get and build the Jackrabbit trunk from

  http://svn.apache.org/repos/asf/jackrabbit/trunk

  I'm currently using revision 583722 for my tests.

- Build the api and sling-json modules

  cd sling/api
  mvn clean install

  cd sling/commons/json
  mvn clean install
  
- Build the script modules used by microsling

  cd sling/scripting/freemarker
  mvn clean install

  cd sling/scripting/javascript
  mvn clean install

  cd sling/scripting/ruby
  mvn clean install

  cd sling/scripting/velocity
  mvn clean install
  
- Run the microsling tests:

  cd sling/microsling/microsling-core
  mvn clean install
  
  Builds microsling and runs the unit and integration tests.  
  
- Build and run this webapp:

  mvn clean jetty:run
  
  Which should say "Started SelectChannelConnector@0.0.0.0:8080" once
  the build is done.  
  
- Connect to http://localhost:8080/ which should return a page
  saying "Microsling homepage". That page contains instructions for
  playing with Microsling.   