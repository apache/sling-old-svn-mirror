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

JUnit OakOnly and JackrabbitOnly categories are used to select some tests
which are specific to one of these implementations. By default, OakOnly tests
are excluded, to switch to them use -Dsling.run.modes=oak

To run the tests against the same instance that is used in the full build,
start an instance by running

  mvn launchpad:run

in the launchpad/testing folder, optionally using -Dsling.run.modes=oak to
use Oak instead of Jackrabbit.

Note that, for all tests to pass, the Sling instance under test needs the 
org.apache.sling.launchpad.test-services bundle, and the war file of the
launchpad/test-services-war project which should be copied to the
sling/startup/0 folder before starting Sling.
