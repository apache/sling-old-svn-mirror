Apache Sling integration-tests module
-------------------------------------
This module contains test classes used by the launchpad/testing module,
separated in their own jar to be reusable.

To run a single test or a specific set of tests against a running Sling
instance, use for example:

    mvn test -Dtest=UploadFileTest -Dhttp.port=1234
    
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

To run the tests against the same instance that is used in the full build,
start an instance by running

   mvn slingstart:start -Dlaunchpad.keep.running=true

in the launchpad/testing folder, optionally using -Dsling.run.modes=oak to
use Oak instead of Jackrabbit. Since that instance is using an arbitrary
http port you have to give exactly that port as parameter if you execute the test.

The standard -Dmaven.surefire.debug option can be used to debug the tests
themselves. Have a look at the README.txt in the launchpad.testing module on how
to debug the server-side Sling code.

Note that, for all tests to pass, the Sling instance under test needs the 
org.apache.sling.launchpad.test-services bundle, and the war file of the
launchpad/test-services-war project which should be copied to the
sling/startup/0 folder before starting Sling.
