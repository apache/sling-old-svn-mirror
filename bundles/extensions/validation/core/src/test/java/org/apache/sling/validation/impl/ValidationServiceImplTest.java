/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.validation.SlingValidationException;
import org.apache.sling.validation.ValidationFailure;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.impl.model.ChildResourceImpl;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.impl.util.examplevalidators.DateValidator;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.spi.ValidationModelRetriever;
import org.apache.sling.validation.spi.ValidatorContext;
import org.apache.sling.validation.spi.Validator;
import org.apache.sling.validation.spi.support.DefaultValidationFailure;
import org.apache.sling.validation.spi.support.DefaultValidationResult;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class ValidationServiceImplTest {

    /**
     * Assume the validation models are stored under (/libs|/apps) + / + VALIDATION_MODELS_RELATIVE_PATH.
     */
    private ValidationServiceImpl validationService;

    private ValidationModelBuilder modelBuilder;

    private ResourcePropertyBuilder propertyBuilder;
    
    @Mock
    ValidationServiceConfiguration configuration;

    @Rule
    public SlingContext context = new SlingContext();
    
    @Mock
    private ResourceBundle defaultResourceBundle;
    
    @Mock
    private ResourceBundleProvider resourceBundleProvider;
    
    @Mock
    private ValidationModelRetriever modelRetriever;
    private Validator<Date> dateValidator;
    
    @Mock
    private ServiceReference<Validator<?>> validatorServiceReference;
    @Mock
    private ServiceReference<Validator<?>> newValidatorServiceReference;
    @Mock
    private Bundle providingBundle;
    
    private static final String DATE_VALIDATOR_ID = "DateValidator";
    private static final String REGEX_VALIDATOR_ID = "RegexValidator";
    
    @Before
    public void setUp() throws LoginException, PersistenceException, RepositoryException {
        validationService = new ValidationServiceImpl();
        validationService.searchPaths = Arrays.asList(context.resourceResolver().getSearchPath());
        validationService.configuration = configuration;
        Mockito.doReturn(20).when(configuration).defaultSeverity();
        validationService.resourceBundleProviders = Collections.singletonList(resourceBundleProvider);
        Mockito.doReturn(defaultResourceBundle).when(resourceBundleProvider).getResourceBundle(Mockito.anyObject());
        modelBuilder = new ValidationModelBuilder();
        propertyBuilder = new ResourcePropertyBuilder();
        dateValidator =  new DateValidator();
        Mockito.doReturn(1l).when(providingBundle).getBundleId();
        Mockito.doReturn(providingBundle).when(validatorServiceReference).getBundle();
        Mockito.doReturn(providingBundle).when(newValidatorServiceReference).getBundle();
        validationService.validatorMap.put(DATE_VALIDATOR_ID, dateValidator, validatorServiceReference, 10);
        validationService.validatorMap.put(REGEX_VALIDATOR_ID, new RegexValidator(), validatorServiceReference, 10);
        validationService.modelRetriever = modelRetriever;
    }

    @Test
    public void testGetValidationModelWithAbsolutePath() throws Exception {
        // check conversion to relative resource type
        validationService.getValidationModel("/libs/some/type", "some path", true);
        Mockito.verify(modelRetriever).getValidationModel("some/type", "some path", true);
    }

    @Test
    public void testGetValidationModelWithRelativePath() throws Exception {
        // check conversion to relative resource type
        validationService.getValidationModel("some/type", "some path", true);
        Mockito.verify(modelRetriever).getValidationModel("some/type", "some path", true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testGetValidationModelWithAbsolutePathOutsideSearchPath() throws Exception {
        // check conversion to relative resource type
        validationService.getValidationModel("/content/some/type", "some path", true);
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateWithInvalidValidatorId() throws Exception {
        propertyBuilder.validator("invalidid", 10);
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test", "some source");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "1");
        validationService.validate(new ValueMapDecorator(hashMap), vm);
    }

    @Test()
    public void testValueMapWithWrongDataType() throws Exception {
        propertyBuilder.validator(DATE_VALIDATOR_ID, 10);
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test", "some source");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "1");
        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>contains(new DefaultValidationFailure("field1", 10, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_WRONG_PROPERTY_TYPE, Date.class)));
    }

    @Test
    public void testValidateNeverCalledWithNullValues() throws Exception {
        Validator<String> myValidator = new Validator<String>() {
            @Override
            public @Nonnull ValidationResult validate(@Nonnull String data, @Nonnull ValidatorContext context, @Nonnull ValueMap arguments)
                    throws SlingValidationException {
                Assert.assertNotNull("data parameter for validate should never be null", data);
                Assert.assertNotNull("location of context parameter for validate should never be null", context.getLocation());
                Assert.assertNotNull("valueMap of context parameter for validate should never be null", context.getValueMap());
                Assert.assertNull("resource of context parameter for validate cannot be set if validate was called only with a value map", context.getResource());
                Assert.assertNotNull("arguments parameter for validate should never be null", arguments);
                return DefaultValidationResult.VALID;
            }
        };
        validationService.validatorMap.put("someId", myValidator, validatorServiceReference, 10);
        propertyBuilder.validator("someId", 20);
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test", "some source");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "1");
        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);
        Assert.assertThat(vr.getFailures(), Matchers.hasSize(0));
        Assert.assertTrue(vr.isValid());
    }

    @Test()
    public void testValueMapWithMissingField() throws Exception {
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        modelBuilder.resourceProperty(propertyBuilder.build("field2"));
        modelBuilder.resourceProperty(propertyBuilder.build("field3"));
        modelBuilder.resourceProperty(propertyBuilder.build("field4"));
        ValidationModel vm = modelBuilder.build("sling/validation/test", "some source");

        // this should not be detected as missing property
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", new String[] {});
        hashMap.put("field2", new String[] { "null" });
        hashMap.put("field3", "");

        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>contains(new DefaultValidationFailure("", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, "field4")));
    }

    @Test()
    public void testValueMapWithMissingOptionalValue() throws Exception {
        modelBuilder.resourceProperty(propertyBuilder.optional().build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test", "some source");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field2", "1");
        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);
        Assert.assertThat(vr.getFailures(), Matchers.hasSize(0));
        Assert.assertTrue(vr.isValid());
    }

    @Test()
    public void testValueMapWithEmptyOptionalValue() throws Exception {
        propertyBuilder.optional();
        propertyBuilder.validator(REGEX_VALIDATOR_ID, null, RegexValidator.REGEX_PARAM, "abc");
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        ValidationModel vm = modelBuilder.build("sling/validation/test", "some source");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "");
        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);

        Assert.assertFalse(vr.isValid());
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>contains(new DefaultValidationFailure("field1", 10, defaultResourceBundle, RegexValidator.I18N_KEY_PATTERN_DOES_NOT_MATCH, "abc")));
    }

    @Test
    public void testValueMapWithCorrectDataType() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, "abc");
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        propertyBuilder = new ResourcePropertyBuilder();
        final String TEST_REGEX = "^test$";
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, TEST_REGEX);
        modelBuilder.resourceProperty(propertyBuilder.build("field2"));
        ValidationModel vm = modelBuilder.build("sling/validation/test", "some source");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("field1", "HelloWorld");
        hashMap.put("field2", "HelloWorld");

        ValidationResult vr = validationService.validate(new ValueMapDecorator(hashMap), vm);

        Assert.assertFalse(vr.isValid());
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure> hasItem(new DefaultValidationFailure("field2", 0, defaultResourceBundle, RegexValidator.I18N_KEY_PATTERN_DOES_NOT_MATCH, TEST_REGEX)));
    }

    // see https://issues.apache.org/jira/browse/SLING-5674
    @Test
    public void testNonExistingResource() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");
        modelBuilder.resourceProperty(property);
        
        ChildResource modelChild = new ChildResourceImpl("child", null, true, Collections.singletonList(property), Collections.emptyList());
        modelBuilder.childResource(modelChild);
        
        modelChild = new ChildResourceImpl("optionalChild", null, false, Collections.singletonList(property), Collections.emptyList());
        modelBuilder.childResource(modelChild);
        
        ValidationModel vm = modelBuilder.build("sometype", "some source");
        ResourceResolver rr = context.resourceResolver();
        Resource nonExistingResource = new NonExistingResource(rr, "non-existing-resource");
        ValidationResult vr = validationService.validate(nonExistingResource, vm);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>containsInAnyOrder(
                new DefaultValidationFailure("", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, "field1"),
                new DefaultValidationFailure("", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_WITH_NAME, "child")
                ));
    }

    // see https://issues.apache.org/jira/browse/SLING-5749
    @Test
    public void testSyntheticResource() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");
        modelBuilder.resourceProperty(property);
        
        ChildResource modelChild = new ChildResourceImpl("child", null, true, Collections.singletonList(property), Collections.emptyList());
        modelBuilder.childResource(modelChild);
        
        modelChild = new ChildResourceImpl("optionalChild", null, false, Collections.singletonList(property), Collections.emptyList());
        modelBuilder.childResource(modelChild);
        
        ValidationModel vm = modelBuilder.build("sometype", "some source");
        ResourceResolver rr = context.resourceResolver();
        Resource nonExistingResource = new SyntheticResource(rr, "someresource", "resourceType");
        ValidationResult vr = validationService.validate(nonExistingResource, vm);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>containsInAnyOrder(
                new DefaultValidationFailure("", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, "field1"),
                new DefaultValidationFailure("", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_WITH_NAME, "child")
                ));
    }

    @Test
    public void testResourceWithMissingGrandChildProperty() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");
        modelBuilder.resourceProperty(property);

        ChildResource modelGrandChild = new ChildResourceImpl("grandchild", null, true,
                Collections.singletonList(property), Collections.<ChildResource> emptyList());
        ChildResource modelChild = new ChildResourceImpl("child", null, true, Collections.singletonList(property),
                Collections.singletonList(modelGrandChild));
        modelBuilder.childResource(modelChild);

        ValidationModel vm = modelBuilder.build("sometype", "some source");

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
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>contains(new DefaultValidationFailure("child/grandchild", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, "field1")));
    }

    @Test
    public void testResourceWithMissingOptionalChildResource() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");

        ChildResource child = new ChildResourceImpl("child", null, false, Collections.singletonList(property),
                Collections.<ChildResource> emptyList());
        modelBuilder.childResource(child);
        ValidationModel vm = modelBuilder.build("type", "some source");

        // create a resource (lacking the optional "child" sub resource)
        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertThat(vr.getFailures(), Matchers.hasSize(0));
        Assert.assertTrue(vr.isValid());
    }

    @Test
    public void testResourceWithNestedChildren() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");

        ChildResource modelGrandChild = new ChildResourceImpl("grandchild", null, true,
                Collections.singletonList(property), Collections.<ChildResource> emptyList());
        ChildResource modelChild = new ChildResourceImpl("child", null, true, Collections.singletonList(property),
                Collections.singletonList(modelGrandChild));
        modelBuilder.childResource(modelChild);
        ValidationModel vm = modelBuilder.build("sometype", "some source");

        // create a resource
        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("field1", "1");
        Resource resourceChild = rr.create(testResource, "child", properties);
        rr.create(resourceChild, "grandchild", properties);

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertThat(vr.getFailures(), Matchers.hasSize(0));
        Assert.assertTrue(vr.isValid());
    }

    @Test
    public void testResourceWithValidatorLeveragingTheResource() throws Exception {
        Validator<String> extendedValidator = new Validator<String>() {
            @Override
            @Nonnull
            public ValidationResult validate(@Nonnull String data, @Nonnull ValidatorContext context, @Nonnull ValueMap arguments)
                    throws SlingValidationException {
                Resource resource = context.getResource();
                if (resource == null) {
                    Assert.fail("Resource must not be null");
                } else {
                    Assert.assertThat(resource.getPath(), Matchers.equalTo("/content/validation/1/resource"));
                }
                return DefaultValidationResult.VALID;
            }
            
        };
        // register validator
        validationService.validatorMap.put("myid", extendedValidator, newValidatorServiceReference, null);
        propertyBuilder.validator("myid", null); // accept any digits
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        ValidationModel vm = modelBuilder.build("sometype", "some source");

        // create a resource
        ResourceResolver rr = context.resourceResolver();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("field1", "1");
        Resource testResource = ResourceUtil.getOrCreateResource(rr,
                "/content/validation/1/resource", properties, JcrConstants.NT_UNSTRUCTURED, true);
        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertTrue(vr.isValid());
    }

    @Test
    public void testResourceWithNestedChildrenAndPatternMatching() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        ResourceProperty property = propertyBuilder.build("field1");

        ChildResource modelGrandChild = new ChildResourceImpl("grandchild", "grandchild.*", true,
                Collections.singletonList(property), Collections.<ChildResource> emptyList());
        ChildResource modelChild = new ChildResourceImpl("child", "child.*", true, Collections.singletonList(property),
                Collections.singletonList(modelGrandChild));
        ChildResource siblingChild = new ChildResourceImpl("siblingchild", "siblingchild.*", true,
                Collections.singletonList(property), Collections.singletonList(modelGrandChild));

        modelBuilder.childResource(modelChild);
        modelBuilder.childResource(siblingChild);
        ValidationModel vm = modelBuilder.build("sometype", "some source");

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
        // sibling child is not there at all (although mandatory)

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>containsInAnyOrder(
                new DefaultValidationFailure("child2", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_MATCHING_PATTERN, "grandchild.*"),
                new DefaultValidationFailure("child3", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_MATCHING_PATTERN, "grandchild.*"),
                new DefaultValidationFailure("child3", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, "field1"),
                new DefaultValidationFailure("", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_CHILD_RESOURCE_MATCHING_PATTERN, "siblingchild.*")));
    }

    @Test
    public void testResourceWithPropertyPatternMatching() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 1, RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        propertyBuilder.nameRegex("field.*");
        modelBuilder.resourceProperty(propertyBuilder.build("field"));
        propertyBuilder.nameRegex("otherfield.*");
        modelBuilder.resourceProperty(propertyBuilder.build("otherfield"));
        propertyBuilder.nameRegex("optionalfield.*").optional();
        modelBuilder.resourceProperty(propertyBuilder.build("optionalfield"));
        ValidationModel vm = modelBuilder.build("type", "some source");

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
        
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>contains(
                new DefaultValidationFailure("field3", 1, defaultResourceBundle, RegexValidator.I18N_KEY_PATTERN_DOES_NOT_MATCH, "\\d"),
                new DefaultValidationFailure("", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_PROPERTY_MATCHING_PATTERN, "otherfield.*")));
    }

    @Test
    public void testResourceWithMultivalueProperties() throws Exception {
        propertyBuilder.validator(REGEX_VALIDATOR_ID, 0, RegexValidator.REGEX_PARAM, "\\d"); // accept any digits
        propertyBuilder.multiple();
        modelBuilder.resourceProperty(propertyBuilder.build("field"));
        ValidationModel vm = modelBuilder.build("type", "some source");

        ResourceResolver rr = context.resourceResolver();
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource",
                JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, true);
        ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
        mvm.put("field", new String[] { "1", "abc", "2" });

        ValidationResult vr = validationService.validate(testResource, vm);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>contains(new DefaultValidationFailure("field[1]", 0, defaultResourceBundle, RegexValidator.I18N_KEY_PATTERN_DOES_NOT_MATCH, "\\d")));
    }

    @Test()
    public void testValidateResourceRecursively() throws Exception {
        modelBuilder.resourceProperty(propertyBuilder.build("field1"));
        final ValidationModel vm1 = modelBuilder.build("resourcetype1", "some source");
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.resourceProperty(propertyBuilder.build("field2"));
        final ValidationModel vm2 = modelBuilder.build("resourcetype2", "some source");

        // set model retriever
        validationService.modelRetriever = new ValidationModelRetriever() {

            @Override
            public @CheckForNull ValidationModel getValidationModel(@Nonnull String resourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
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

        final Predicate<Resource> ignoreResourceType3Filter = new Predicate<Resource>() {
            @Override
            public boolean test(final Resource resource) {
                return !"resourcetype3".equals(resource.getResourceType());
            }
        };
        
        ValidationResult vr = validationService.validateResourceRecursively(testResource, true, ignoreResourceType3Filter, false);
        Assert.assertFalse("resource should have been considered invalid", vr.isValid());
        Assert.assertThat(vr.getFailures(), Matchers.<ValidationFailure>contains(
                new DefaultValidationFailure("", 20, defaultResourceBundle,  ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, "field1"),
                new DefaultValidationFailure("child2", 20, defaultResourceBundle, ValidationServiceImpl.I18N_KEY_MISSING_REQUIRED_PROPERTY_WITH_NAME, "field2")));
    }

    // see https://issues.apache.org/jira/browse/SLING-5674
    @Test
    public void testValidateResourceRecursivelyOnNonExistingResource() throws Exception {
        ResourceResolver rr = context.resourceResolver();
        Resource nonExistingResource = new NonExistingResource(rr, "non-existing-resource");
        
        ValidationResult vr = validationService.validateResourceRecursively(nonExistingResource, true, null, true);
        Assert.assertTrue("resource should have been considered valid", vr.isValid());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateResourceRecursivelyWithMissingValidationModel() throws Exception {
        // set model retriever which never retrieves anything
        validationService.modelRetriever = new ValidationModelRetriever() {
            @Override
            public @CheckForNull ValidationModel getValidationModel(@Nonnull String resourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
                return null;
            }
        };

        ResourceResolver rr = context.resourceResolver();
        // resource is having no connected validation model
        Resource testResource = ResourceUtil.getOrCreateResource(rr, "/content/validation/1/resource", "resourcetype1",
                JcrConstants.NT_UNSTRUCTURED, true);

        validationService.validateResourceRecursively(testResource, true, null, false);
    }

    @Test()
    public void testValidateResourceRecursivelyWithMissingValidatorAndNoEnforcement() throws Exception {
        // set model retriever which never retrieves anything
        validationService.modelRetriever = new ValidationModelRetriever() {
            @Override
            public @CheckForNull ValidationModel getValidationModel(@Nonnull String resourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
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
