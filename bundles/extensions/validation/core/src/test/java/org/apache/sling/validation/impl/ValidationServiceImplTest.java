/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at
 *
http://www.apache.org/licenses/LICENSE-2.0
 *
Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package org.apache.sling.validation.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.Predicate;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.Validator;
import org.apache.sling.validation.exceptions.SlingValidationException;
import org.apache.sling.validation.impl.model.ChildResourceImpl;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.impl.util.examplevalidators.DateValidator;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ValidationServiceImplTest {

    /**
     * Assume the validation models are stored under (/libs|/apps) + / + VALIDATION_MODELS_RELATIVE_PATH.
     */
    private ValidationServiceImpl validationService;

    private ValidationModelBuilder modelBuilder;

    private ResourcePropertyBuilder propertyBuilder;

    @Rule
    public SlingContext context = new SlingContext();

    @Before
    public void setUp() throws LoginException, PersistenceException, RepositoryException {
        validationService = new ValidationServiceImpl();
        validationService.searchPaths = Arrays.asList(context.resourceResolver().getSearchPath());
        modelBuilder = new ValidationModelBuilder();
        propertyBuilder = new ResourcePropertyBuilder();
    }

    @Test
    public void testGetValidationModel() throws Exception {

    }

    @Test()
    public void testValueMapWithWrongDataType() throws Exception {
        propertyBuilder.validator(new DateValidator());
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "1");
        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);
        Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
        expectedFailureMessages
                .put("field1",
                        Arrays.asList("Property was expected to be of type 'class java.util.Date' but cannot be converted to that type."));
        Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
    }

    @Test
    public void testValidateNeverCalledWithNullValues() throws Exception {
        Validator<String> myValidator = new Validator<String>() {
            @Override
            public String validate(@Nonnull String data, @Nonnull ValueMap valueMap, @Nonnull ValueMap arguments)
                    throws SlingValidationException {
                Assert.assertNotNull("data parameter for validate should never be null", data);
                Assert.assertNotNull("valueMap parameter for validate should never be null", valueMap);
                Assert.assertNotNull("arguments parameter for validate should never be null", arguments);
                return null;
            }
        };
        propertyBuilder.validator(myValidator);
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "1");
        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);
        Assert.assertTrue(vr.isValid());
    }

    @Test()
    public void testValueMapWithMissingField() throws Exception {
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        modelBuilder.resourceProperty(propertyBuilder.build("field2"));
        modelBuilder.resourceProperty(propertyBuilder.build("field3"));
        modelBuilder.resourceProperty(propertyBuilder.build("field4"));
        ValidationModel vm = modelBuilder.build("sling/validation/test");

        // this should not be detected as missing property
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", new String[] {});
        hashMap.put("field2", new String[] { "null" });
        hashMap.put("field3", "");

        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);
        Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
        expectedFailureMessages.put("field4", Arrays.asList("Missing required property."));
        Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
    }

    @Test()
    public void testValueMapWithMissingOptionalValue() throws Exception {
        modelBuilder.resourceProperty(propertyBuilder.optional().build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field2", "1");
        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);
        Assert.assertTrue(vr.isValid());
    }

    @Test()
    public void testValueMapWithEmptyOptionalValue() throws Exception {
        propertyBuilder.optional();
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "abc");
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "");
        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);

        Assert.assertFalse(vr.isValid()); // check for correct error message Map<String, List<String>>
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("field1", Arrays.asList("Property does not match the pattern abc")));
    }

    @Test
    public void testValueMapWithCorrectDataType() throws Exception {
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "abc");
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        propertyBuilder = new ResourcePropertyBuilder();
        final String TEST_REGEX = "^test$";
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, TEST_REGEX);
        modelBuilder.resourceProperty(propertyBuilder.build("field2"));
        ValidationModel vm = modelBuilder.build("sling/validation/test");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "HelloWorld");
        hashMap.put("field2", "HelloWorld");

        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);

        Assert.assertFalse(vr.isValid()); // check for correct error message Map<String, List<String>>
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("field2", Arrays.asList("Property does not match the pattern " + TEST_REGEX)));
    }

    @Test
    public void testResourceWithMissingGrandChildProperty() throws Exception {
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");
        modelBuilder.resourceProperty(property);

        ChildResource modelGrandChild = new ChildResourceImpl("grandchild", null, true,
                Collections.singletonList(property), Collections.<ChildResource> emptyList());
        ChildResource modelChild = new ChildResourceImpl("child", null, true, Collections.singletonList(property),
                Collections.singletonList(modelGrandChild));
        modelBuilder.childResource(modelChild);

        ValidationModel vm = modelBuilder.build("sometype");

        // create a resource
        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);
        ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
        mvm.put("field1", "1");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("field1", "1");
        Resource resourceChild = rr.create(testResource, "child", properties);
        // resourceGrandChild is missing the mandatory field1 property
        rr.create(resourceChild, "grandchild", null);

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("child/grandchild/field1", Arrays.asList("Missing required property.")));
        Assert.assertThat(vr.getFailureMessages().keySet(), Matchers.hasSize(1));
    }

    @Test
    public void testResourceWithMissingOptionalChildResource() throws Exception {
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");

        ChildResource child = new ChildResourceImpl("child", null, false, Collections.singletonList(property),
                Collections.<ChildResource> emptyList());
        modelBuilder.childResource(child);
        ValidationModel vm = modelBuilder.build("type");

        // create a resource (lacking the optional "child" sub resource)
        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertTrue(vr.isValid());
    }

    @Test
    public void testResourceWithNestedChildren() throws Exception {
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");

        ChildResource modelGrandChild = new ChildResourceImpl("grandchild", null, true,
                Collections.singletonList(property), Collections.<ChildResource> emptyList());
        ChildResource modelChild = new ChildResourceImpl("child", null, true, Collections.singletonList(property),
                Collections.singletonList(modelGrandChild));
        modelBuilder.childResource(modelChild);
        ValidationModel vm = modelBuilder.build("sometype");

        // create a resource
        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("field1", "1");
        Resource resourceChild = rr.create(testResource, "child", properties);
        rr.create(resourceChild, "grandchild", properties);

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertTrue(vr.isValid());
    }

    @Test
    public void testResourceWithNestedChildrenAndPatternMatching() throws Exception {
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");

        ChildResource modelGrandChild = new ChildResourceImpl("grandchild", "grandchild.*", true,
                Collections.singletonList(property), Collections.<ChildResource> emptyList());
        ChildResource modelChild = new ChildResourceImpl("child", "child.*", true, Collections.singletonList(property),
                Collections.singletonList(modelGrandChild));
        ChildResource siblingChild = new ChildResourceImpl("siblingchild", "siblingchild.*", true,
                Collections.singletonList(property), Collections.singletonList(modelGrandChild));

        modelBuilder.childResource(modelChild);
        modelBuilder.childResource(siblingChild);
        ValidationModel vm = modelBuilder.build("sometype");

        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/apps/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("field1", "1");
        Resource resourceChild = rr.create(testResource, "child1", properties);
        rr.create(resourceChild, "grandchild1", properties);
        // child2 is lacking its mandatory sub resource
        rr.create(testResource, "child2", properties);
        rr.create(testResource, "child3", null);
        // siblingchild is not there at all (although mandatory)

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("child2/grandchild.*", Arrays.asList("Missing required child resource.")));
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("child3/grandchild.*", Arrays.asList("Missing required child resource.")));
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("child3/field1", Arrays.asList("Missing required property.")));
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("siblingchild.*", Arrays.asList("Missing required child resource.")));
        Assert.assertThat(vr.getFailureMessages().keySet(), Matchers.hasSize(4));
    }

    @Test
    public void testResourceWithPropertyPatternMatching() throws Exception {
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        propertyBuilder.nameRegex("field.*");
        modelBuilder.resourceProperty(propertyBuilder.build("field"));
        propertyBuilder.nameRegex("otherfield.*");
        modelBuilder.resourceProperty(propertyBuilder.build("otherfield"));
        propertyBuilder.nameRegex("optionalfield.*").optional();
        modelBuilder.resourceProperty(propertyBuilder.build("optionalfield"));
        ValidationModel vm = modelBuilder.build("type");

        // create a resource
        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);
        ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
        mvm.put("field1", "1");
        mvm.put("field2", "1");
        mvm.put("field3", "abc"); // does not validate
        // otherfield.* property is missing

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("field3", Arrays.asList("Property does not match the pattern \\d")));
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("otherfield.*", Arrays.asList("Missing required property.")));
        Assert.assertThat(vr.getFailureMessages().keySet(), Matchers.hasSize(2));
    }

    @Test
    public void testResourceWithMultivalueProperties() throws Exception {
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        propertyBuilder.multiple();
        modelBuilder.resourceProperty(propertyBuilder.build("field"));
        ValidationModel vm = modelBuilder.build("type");

        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);
        ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
        mvm.put("field", new String[] { "1", "abc", "2" });

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("field[1]", Arrays.asList("Property does not match the pattern \\d")));
        Assert.assertThat(vr.getFailureMessages().keySet(), Matchers.hasSize(1));
    }

    @Test()
    public void testValidateResourceRecursively() throws Exception {
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        final ValidationModel vm1 = modelBuilder.build("resourcetype1");
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.resourceProperty(propertyBuilder.build("field2"));
        final ValidationModel vm2 = modelBuilder.build("resourcetype2");

        // set model retriever
        validationService.modelRetriever = new ValidationModelRetriever() {

            @Override
            public @CheckForNull ValidationModel getModel(@Nonnull String resourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
                if (resourceType.equals("resourcetype1")) {
                    return vm1;
                } else if (resourceType.equals("resourcetype2")) {
                    return vm2;
                } else {
                    return null;
                }
            }
        };

        ResourceResolver rr = context.resourceResolver();
        // resource is lacking the required field (is invalid)
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource", "resourcetype1",
                JcrConstants.NT_UNSTRUCTURED, true);

        // child1 is valid
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "resourcetype2");
        properties.put("field2", "test");
        rr.create(testResource, "child1", properties);

        // child2 is invalid
        properties.clear();
        properties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "resourcetype2");
        rr.create(testResource, "child2", properties);

        // child3 has no model (but its resource type is ignored)
        properties.clear();
        properties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "resourcetype3");
        rr.create(testResource, "child3", properties);

        Predicate ignoreResourceType3Filter = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                Resource resource = (Resource) object;
                if ("resourcetype3".equals(resource.getResourceType())) {
                    return false;
                }
                return true;
            }
        };
        
        ValidationResult vr = validationService.validateResourceRecursively(testResource, true, ignoreResourceType3Filter, false);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("field1", Arrays.asList("Missing required property.")));
        Assert.assertThat(vr.getFailureMessages(),
                Matchers.hasEntry("child2/field2", Arrays.asList("Missing required property.")));
        Assert.assertThat(vr.getFailureMessages().keySet(), Matchers.hasSize(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateResourceRecursivelyWithMissingValidationModel() throws Exception {
        // set model retriever which never retrieves anything
        validationService.modelRetriever = new ValidationModelRetriever() {
            @Override
            public @CheckForNull ValidationModel getModel(@Nonnull String resourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
                return null;
            }
        };

        ResourceResolver rr = context.resourceResolver();
        // resource is having no connected validation model
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource", "resourcetype1",
                JcrConstants.NT_UNSTRUCTURED, true);

        ValidationResult vr = validationService.validateResourceRecursively(testResource, true, null, false);
    }

    @Test()
    public void testValidateResourceRecursivelyWithMissingValidatorAndNoEnforcement() throws Exception {
        // set model retriever which never retrieves anything
        validationService.modelRetriever = new ValidationModelRetriever() {
            @Override
            public @CheckForNull ValidationModel getModel(@Nonnull String resourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
                return null;
            }
        };

        ResourceResolver rr = context.resourceResolver();
        // resource is having no connected validation model
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource", "resourcetype1",
                JcrConstants.NT_UNSTRUCTURED, true);

        ValidationResult vr = validationService.validateResourceRecursively(testResource, false, null, false);
        Assert.assertTrue(vr.isValid());
    }

    @Test
    public void testGetRelativeResourcePath() { 
        // return relative paths unmodified
        Assert.assertThat(validationService.getRelativeResourceType("relative/path"), Matchers.equalTo("relative/path"));
        Assert.assertThat(validationService.getRelativeResourceType("/apps/relative/path"),
                Matchers.equalTo("relative/path"));
        Assert.assertThat(validationService.getRelativeResourceType("/libs/relative/path"),
                Matchers.equalTo("relative/path"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRelativeResourcePathWithAbsolutePathOutsideOfTheSearchPaths() {
        validationService.getRelativeResourceType("/apps2/relative/path");
    }

}
