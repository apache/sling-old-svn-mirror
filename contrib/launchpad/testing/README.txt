Apache Sling Launchpad Testing module

This module builds a Sling webapp using bundles from the trunk, and
runs integration tests (that were previously in the launchpad/webapp
module) on them.  

How to run these tests
----------------------

1) Build all Sling bundles

  cd <top of the Sling source code tree>
  mvn clean install
  
2) Build and run this

  cd contrib/launchpad/testing
  mvn clean install
  
This might required building a few other Sling bundles, from the contrib
and samples code trees. We'll need to improve this, but right now there's no
common pom to build all what's required for these contrib integration tests.

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

This is very useful during development: you can leave a Sling webapp instance
running, update bundles in it using the mvn autoInstallBundle profile, and run
specific integration tests quickly from another terminal.

JCR Install Integration tests
-----------------------------

This module also contains integration tests for the JCR Install module. These
tests are disabled by default, since they may not be of general interest. To
include the JCR Install tests in the integration tests run enable the
jcrinstall-tests profile such as in

   mvn -P jcrinstall-tests integration-test
   
*** WARNING: the jcrinstall-tests do not work in revision 741168, due to changes
in the start levels. They can be run according to the scenario below, if you 
change the default bundles start level to 30, after starting the Sling instance
with mvn jetty:run, from http://localhost:8080/system/console/vmstat. ***     

To run the jcrinstall integration tests quickly, without having to start the
webapp first:

1. Run mvn clean package jetty:run -P jcrinstall-tests in a first console

(1.5 see WARNING above - adjust the default bundles start level)

2. In another console run

  mvn test -Pjcrinstall-tests  -Dtest=**/integrationtest/jcrinstall/**/*Test.java
  
       
