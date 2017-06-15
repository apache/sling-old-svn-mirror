# Job IT tests launcher.

This project runs a test launcher that creates an OSGi instance with only what is required to run Server Side tests.
Unfurtunately, since OSGi is a multi classloader environment its not possible to perform tests in this bundle, except
tests over http as any references made to APIs will get resolved with the wrong classloader and so wont be able to 
interact with OSGi. Hence the tests are in a separate bundle that is loaded by the launchpad.