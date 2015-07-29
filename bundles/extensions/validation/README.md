# Apache Sling Validation
Look at the [Sling Wiki](http://sling.apache.org/documentation/bundles/validation.html) for documentation.

## Testing the implementation
    mvn clean install

This will install all the artifacts in your local repository. During the install phase of the `it-http` module a Sling Launchpad instance
will be automatically turned on and all the tests from the `it-http` module will be executed.
