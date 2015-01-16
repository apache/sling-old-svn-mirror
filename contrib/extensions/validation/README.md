# Apache Sling Validation Framework Prototype
This prototype proposes a new validation framework API for Apache Sling and also provides a default implementation, together with a testing
services bundle.

The core service of the validation framework is represented by the `ValidationService` interface, which provides the following methods:
```java
/**
     * Tries to obtain a {@link ValidationModel} that is able to validate a {@code Resource} of type {@code validatedResourceType}.
     *
     * @param validatedResourceType the type of {@code Resources} the model validates
     * @param applicablePath        the model's applicable path (the path of the validated resource)
     * @return a {@code ValidationModel} if one is found, {@code null} otherwise
     */
    ValidationModel getValidationModel(String validatedResourceType, String applicablePath);

    /**
     * Tries to obtain a {@link ValidationModel} that is able to validate the given {@code resource}.
     *
     * @param resource the resource for which to obtain a validation model
     * @return a {@code ValidationModel} if one is found, {@code null} otherwise
     */
    ValidationModel getValidationModel(Resource resource);

    /**
     * Validates a {@link Resource} using a specific {@link ValidationModel}. If the {@code model} describes a resource tree,
     * the {@link ResourceResolver} associated with the {@code resource} will be used for retrieving information about the {@code
     * resource}'s descendants.
     *
     * @param resource the resource to validate
     * @param model    the model with which to perform the validation
     * @return a {@link ValidationResult} that provides the necessary information
     */
    ValidationResult validate(Resource resource, ValidationModel model);

    /**
     * Validates a {@link ValueMap} or any object adaptable to a {@code ValueMap} using a specific {@link ValidationModel}. Since the
     * {@code valueMap} only contains the direct properties of the adapted object, the {@code model}'s descendants description should not
     * be queried for this validation operation.
     *
     * @param valueMap the map to validate
     * @return a {@link ValidationResult} that provides the necessary information
     */
    ValidationResult validate(ValueMap valueMap, ValidationModel model);
```

The prototype allows validating resource trees or any other object that is adaptable to a `ValueMap`. For example a `SlingHttpServletRequest` can be validated in the default implementation with the help of an `AdapterFactory` that extracts the request's parameters into a `ValueMap`.

The default implementation is also able to automatically create `ValidationModel` objects from JCR content structures of the following form:
<pre>
validationModel
    @validatedResourceType
    @applicablePaths = [path1,path2,...] (optional)
    @sling:resourceType = sling/validation/model
    properties
        property1
            @propertyType = <string value>
            @propertyMultiple = <boolean value> (optional)
            validators
                validator1
                    @validatorArguments = [key=value,key=value...] (optional)
                validatorN
                    @validatorArguments = [key=value,key=value...] (optional)
        propertyN
            @propertyType = <string value>
            @propertyMultiple = <boolean value> (optional)
            validators
                validator1
                    @validatorArguments = [key=value,key=value...] (optional)
    children
        child1
            properties
                property1
                    @propertyType = <string value>
                    @propertyMultiple = <boolean value> (optional)
                    validators
                        validator1
                            @validatorArguments = [key=value,key=value...] (optional)
                        validatorN
                            @validatorArguments = [key=value,key=value...] (optional)
</pre>
where all nodes can be of type `nt:unstructured`.

## Testing the default implementation
Clone this repository and install the bundles (api, core test-services, it-http)

    git clone https://github.com/raducotescu/org.apache.sling.validation.git
    cd org.apache.sling.validation/
    mvn clean install

This will install all the artifacts in your local repository. During the install phase of the `it-http` module a Sling Launchpad instance
will be automatically turned on and all the tests from the `it-http` module will be executed.
