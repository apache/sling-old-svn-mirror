Apache Sling Launchpad Testing module
=====================================
This module builds a Sling instance using bundles from the trunk, and
runs integration tests against it via HTTP.

Default build with integration tests
------------------------------------
The integration tests are provided by the sibling integration-tests 
module. By default the Sling instance to test is started, including a
few test-specific bundles, the integration tests are executed and 
the instance is stopped.

Executing individual tests
--------------------------
To run individual tests against this instance, with the exact same setup used
in the full build, use

  mvn clean install -Dlaunchpad.keep.running=true -Dhttp.port=8080 -Ddebug

The -Ddebug option enables server-side debugging of the instance under test, 
on port 8000. It can be omitted, of course.

Use CTRL-C to stop that instance.

See the README.txt in the integration-tests module for how to run specific 
tests against that instance.
