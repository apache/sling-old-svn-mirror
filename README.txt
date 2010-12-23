Apache Sling integration-tests module
-------------------------------------
This module contains test classes used by the launchpad/testing module,
separated in their own jar to be reusable.

To run a single test or a specific set of tests against a running Sling
instance, use for example:

    mvn test -Dtest=UploadFileTest
    
Where UploadFileTest is the test to run. Wildcards are allowed, and test
classes are found in the src/main folder (not a typo - that's not src/test
as we want to pack the tests in the jar file that we build).

See the <properties> section in pom.xml for additional parameters that the
tests use.

Here's another example, running the tests against a Sling instance running 
on host xyzzy, port 1234, with the Sling main servlet mounted under /foo:

   mvn -o -s /dev/null test \
    -Dhttp.port=1234 \
    -Dtest.host=xyzzy \
    -Dhttp.base.path=foo \
    -Dwebdav.workspace.path=foo \
    -Dtest=**/integrationtest/**/*Test.java

Note that, for all tests to pass, the Sling instance under test needs the 
org.apache.sling.launchpad.test-services bundle.

