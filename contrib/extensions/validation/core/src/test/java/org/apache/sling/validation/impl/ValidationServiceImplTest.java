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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.ValidationResult;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.setup.MockedResourceResolver;
import org.apache.sling.validation.impl.util.examplevalidators.DateValidator;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

public class ValidationServiceImplTest {

    /**
     * Assume the validation models are stored under (/libs|/apps) + / + VALIDATION_MODELS_RELATIVE_PATH.
     */
    private static final String VALIDATION_MODELS_RELATIVE_PATH = "sling/validation/models";
    private static final String APPS = "/apps";
    private static final String LIBS = "/libs";
    private static ResourceResolverFactory rrf;
    private static Resource appsValidatorsRoot;
    private static Resource libsValidatorsRoot;
    private ValidationServiceImpl validationService;

    @BeforeClass
    public static void init() throws Exception {
        rrf = mock(ResourceResolverFactory.class);
        when(rrf.getAdministrativeResourceResolver(null)).thenAnswer(new Answer<ResourceResolver>() {
            public ResourceResolver answer(InvocationOnMock invocation) throws Throwable {
                return new MockedResourceResolver();
            }
        });
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        if (rr != null) {
            appsValidatorsRoot = ResourceUtil.getOrCreateResource(rr, APPS + "/" + VALIDATION_MODELS_RELATIVE_PATH, (Map<String, Object>) null,
                    "sling:Folder", true);
            libsValidatorsRoot = ResourceUtil.getOrCreateResource(rr, LIBS + "/" + VALIDATION_MODELS_RELATIVE_PATH, (Map<String, Object>) null,
                    "sling:Folder", true);
            rr.close();
        }
    }

    @AfterClass
    public static void beNiceAndClean() throws Exception {
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        if (rr != null) {
            if (appsValidatorsRoot != null) {
                rr.delete(appsValidatorsRoot);
            }
            if (libsValidatorsRoot != null) {
                rr.delete(libsValidatorsRoot);
            }
            rr.commit();
            rr.close();
        }
    }

    @Before
    public void setUp() {
        validationService = new ValidationServiceImpl();
        validationService.validators = new HashMap<String, Validator<?>>();
        Whitebox.setInternalState(validationService, "rrf", rrf);
       
    }
    
    @Test
    public void testGetValidationModel() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());
        
        TestProperty property = new TestProperty("field1");
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null, model2 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
                model2 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel2", "sling/validation/test",
                        new String[]{"/apps/validation/1",
                        "/apps/validation/2"}, property);
            }

            // BEST MATCHING PATH = /apps/validation/1; assume the applicable paths contain /apps/validation/2
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            assertTrue(arrayContainsString(vm.getApplicablePaths(), "/apps/validation/2"));

            // BEST MATCHING PATH = /apps/validation; assume the applicable paths contain /apps/validation but not /apps/validation/1
            vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/resource");
            assertTrue(arrayContainsString(vm.getApplicablePaths(), "/apps/validation"));
            assertTrue(!arrayContainsString(vm.getApplicablePaths(), "/apps/validation/1"));
            if (model1 != null) {
                rr.delete(model1);
            }
            if (model2 != null) {
                rr.delete(model2);
            }
        } finally {
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }

    @Test
    public void testGetValidationModelWithOverlay() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty field = new TestProperty("field1");
        field.addValidator("org.apache.sling.validation.impl.validators.RegexValidator");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null, model2 = null, model3 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation/1"}, field);
                model2 = createValidationModelResource(rr, appsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation/1",
                                "/apps/validation/2"}, field);
                model3 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel2", "sling/validation/test",
                        new String[]{"/apps/validation/3"}, field);
            }

            // BEST MATCHING PATH = /apps/validation/1; assume the applicable paths contain /apps/validation/2
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            assertTrue(arrayContainsString(vm.getApplicablePaths(), "/apps/validation/2"));

            vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/3/resource");
            assertTrue(arrayContainsString(vm.getApplicablePaths(), "/apps/validation/3"));

            if (model1 != null) {
                rr.delete(model1);
            }
            if (model2 != null) {
                rr.delete(model2);
            }
            if (model3 != null) {
                rr.delete(model3);
            }
        } finally {
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }

    @Test(expected=IllegalStateException.class)
    public void testGetValidationModelWithInvalidValidator() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty field = new TestProperty("field1");
        // invalid validator name
        field.addValidator("org.apache.sling.validation.impl.validators1.RegexValidator");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model = null;
        try {
            if (rr != null) {
                model = createValidationModelResource(rr, appsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation/1",
                                "/apps/validation/2"}, field);
            }

            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
        } finally {
            if (rr != null) {
                if (model != null) {
                    rr.delete(model);
                }
                rr.commit();
                rr.close();
            }
        }
    }

    @Test(expected=IllegalStateException.class)
    public void testGetValidationModelWithMissingChildrenAndProperties() throws Exception {
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model = null;
        try {
            if (rr != null) {
                model = createValidationModelResource(rr, appsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation/1",
                                "/apps/validation/2"});
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
        } finally {
            if (rr != null) {
                if (model != null) {
                    rr.delete(model);
                }
                rr.commit();
                rr.close();
            }
        }
    }

    @Test()
    public void testValueMapWithWrongDataType() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());
        validationService.validators.put("org.apache.sling.validation.impl.util.examplevalidators.DateValidator",new DateValidator());

        TestProperty property = new TestProperty("field1");
        property.addValidator("org.apache.sling.validation.impl.util.examplevalidators.DateValidator");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("field1", "1");
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            
            Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
            expectedFailureMessages.put("field1", Arrays.asList("Property was expected to be of type 'class java.util.Date' but cannot be converted to that type."));
            Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
        } finally {
            if (model1 != null) {
                rr.delete(model1);
            }
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }
    
    @Test()
    public void testValueMapWithMissingField() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex=.*");
        TestProperty property2 = new TestProperty("field2");
        property2.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex=.*");
        TestProperty property3 = new TestProperty("field3");
        property3.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex=.*");
        TestProperty property4 = new TestProperty("field4");
        property3.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex=.*");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property, property2, property3, property4);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            // this should not be detected as missing property
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("field1", new String[]{});
                put("field2", new String[]{"null"});
                put("field3", "");
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
            expectedFailureMessages.put("field4", Arrays.asList("Missing required property."));
            Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
        } finally {
            if (model1 != null) {
                rr.delete(model1);
            }
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }
    
    @Test()
    public void testValueMapWithMissingOptionalValue() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        property.optional = true;
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "");
        
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("field2", "1");
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            Assert.assertTrue(vr.isValid());
        } finally {
            if (model1 != null) {
                rr.delete(model1);
            }
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }
    
    @Test()
    public void testValueMapWithEmptyOptionalValue() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        property.optional = true;
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex=abc");
        
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("field1", "");
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            Assert.assertFalse(vr.isValid());
            // check for correct error message
            Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
            expectedFailureMessages.put("field1", Arrays.asList("Property does not match the pattern abc"));
            Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
        } finally {
            if (model1 != null) {
                rr.delete(model1);
            }
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }

    @Test
    public void testValueMapWithCorrectDataType() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty field1 = new TestProperty("field1");
        field1.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex=^\\p{L}+$");
        TestProperty field2 = new TestProperty("field2");
        final String TEST_REGEX = "^test$";
        field2.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex="+TEST_REGEX);
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, field1, field2);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("field1", "HelloWorld");
                put("field2", "HelloWorld");
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            assertFalse(vr.isValid());
            // check for correct error message
            Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
            expectedFailureMessages.put("field2", Arrays.asList("Property does not match the pattern " + TEST_REGEX));
            Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
            if (model1 != null) {
                rr.delete(model1);
            }
        } finally {
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }
    

    @Test
    public void testResourceWithMissingChildProperty() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", RegexValidator.REGEX_PARAM + "=" + "\\d");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        Resource testResource = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
                
                Resource child = createValidationModelChildResource(model1, "child1", null, false,  new TestProperty("hello"));
                createValidationModelChildResource(child, "grandChild1", null, false, new TestProperty("hello"));

                testResource = ResourceUtil.getOrCreateResource(rr, "/apps/validation/1/resource", JcrConstants.NT_UNSTRUCTURED,
                        JcrConstants.NT_UNSTRUCTURED, true);
                ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
                mvm.put("field1", "1");
                
                Resource childResource = rr.create(testResource, "child1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                
                Resource resourceChild = rr.create(testResource, "child1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                mvm = resourceChild.adaptTo(ModifiableValueMap.class);
                mvm.put("hello", "1");

                // /apps/validation/1/resource/child1/grandChild1 will miss its mandatory "hello" property
                Resource resourceGrandChild = rr.create(resourceChild, "grandChild1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                rr.commit();
                
                mvm = resourceGrandChild.adaptTo(ModifiableValueMap.class);
                mvm.put("field1", "1");
                rr.commit();
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            ValidationResult vr = validationService.validate(testResource, vm);
            assertFalse(vr.isValid());
            assertThat(vr.getFailureMessages(), Matchers.hasKey("child1/grandChild1/hello"));
            assertThat(vr.getFailureMessages().keySet(), Matchers.hasSize(1));
        } finally {
            if (rr != null) {
                rr.delete(model1);
            }
            if (testResource != null) {
                 rr.delete(testResource);
            }
            rr.commit();
            rr.close();
        }
    }
    
    @Test
    public void testResourceWithMissingOptionalChildProperty() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", RegexValidator.REGEX_PARAM + "=" + "\\d");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        Resource testResource = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
                
                createValidationModelChildResource(model1, "child1", null, true, new TestProperty("hello"));

                testResource = ResourceUtil.getOrCreateResource(rr, "/apps/validation/1/resource", JcrConstants.NT_UNSTRUCTURED,
                        JcrConstants.NT_UNSTRUCTURED, true);
                ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
                mvm.put("field1", "1");
                
                rr.create(testResource, "child2", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                rr.commit();
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            ValidationResult vr = validationService.validate(testResource, vm);
            assertTrue(vr.isValid());
        } finally {
            if (rr != null) {
                rr.delete(model1);
            }
            if (testResource != null) {
                 rr.delete(testResource);
            }
            rr.commit();
            rr.close();
        }
    }
    
    @Test
    public void testResourceWithNestedChildren() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", RegexValidator.REGEX_PARAM + "=" + "\\d");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        Resource testResource = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
                
                Resource child = createValidationModelChildResource(model1, "child1", null, false, new TestProperty("hello"));
                createValidationModelChildResource(child, "grandChild1", null, false, new TestProperty("hello"));

                testResource = ResourceUtil.getOrCreateResource(rr, "/apps/validation/1/resource", JcrConstants.NT_UNSTRUCTURED,
                        JcrConstants.NT_UNSTRUCTURED, true);
                Resource childResource = rr.create(testResource, "child1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});

                ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
                mvm.put("field1", "1");

                Resource resourceChild = rr.create(testResource, "child1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                mvm = resourceChild.adaptTo(ModifiableValueMap.class);
                mvm.put("hello", "test");

                Resource resourceGrandChild = rr.create(resourceChild, "grandChild1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                mvm = resourceGrandChild.adaptTo(ModifiableValueMap.class);
                mvm.put("hello", "test");
                rr.commit();
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            ValidationResult vr = validationService.validate(testResource, vm);
            assertTrue(vr.isValid());
        } finally {
            if (rr != null) {
                if (model1 != null) {
                    rr.delete(model1);
                }
                if (testResource != null) {
                    rr.delete(testResource);
                }
                rr.commit();
                rr.close();
            }
        }
    }
    
    @Test
    public void testResourceWithNestedChildrenAndPatternMatching() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", RegexValidator.REGEX_PARAM + "=" + "\\d");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        Resource testResource = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
                Resource child = createValidationModelChildResource(model1, "child1", "child.*", false, new TestProperty("hello"));
                createValidationModelChildResource(child, "grandChild", "grandChild.*", false, new TestProperty("hello"));
                rr.commit();
                
                testResource = ResourceUtil.getOrCreateResource(rr, "/apps/validation/1/resource", JcrConstants.NT_UNSTRUCTURED,
                        JcrConstants.NT_UNSTRUCTURED, true);
                ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
                mvm.put("field1", "1");
                
                Resource childResource = rr.create(testResource, "child1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});

                mvm = childResource.adaptTo(ModifiableValueMap.class);
                mvm.put("hello", "test");

                Resource resourceChild = rr.create(testResource, "child2", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                mvm = resourceChild.adaptTo(ModifiableValueMap.class);
                mvm.put("hello2", "test");

                Resource resourceGrandChild = rr.create(resourceChild, "grandChild1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                mvm = resourceGrandChild.adaptTo(ModifiableValueMap.class);
                mvm.put("hello", "test");
                rr.commit();
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            ValidationResult vr = validationService.validate(testResource, vm);
            assertFalse(vr.isValid());
            // check for correct error message
            Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
            expectedFailureMessages.put("child2/hello", Arrays.asList("Missing required property."));
            expectedFailureMessages.put("child1/grandChild.*", Arrays.asList("Missing required child resource."));
            Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
        } finally {
            if (rr != null) {
                if (model1 != null) {
                    rr.delete(model1);
                }
                if (testResource != null) {
                    rr.delete(testResource);
                }
                rr.commit();
                rr.close();
            }
        }
    }
    
    @Test
    public void testResourceWithPropertyPatternMatching() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        final String TEST_REGEX = "^testvalue.*$";
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex="+TEST_REGEX);
        property.setNameRegex("property[1-4]");
        
        TestProperty otherProperty = new TestProperty("field2");
        otherProperty.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex="+TEST_REGEX);
        otherProperty.setNameRegex("otherproperty[1-4]");
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property, otherProperty);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("property1", "testvalue1"); 
                put("property2", "test1value1"); // does not match validator pattern
                put("property3", "testvalue1");
                put("property4", "1testvalue1"); // does not match validator pattern
                put("property5", "invalid");     // does not match property name pattern
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            assertFalse(vr.isValid());
            // check for correct error message
            Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
            expectedFailureMessages.put("property2", Arrays.asList("Property does not match the pattern " + TEST_REGEX));
            expectedFailureMessages.put("property4", Arrays.asList("Property does not match the pattern " + TEST_REGEX));
            expectedFailureMessages.put("otherproperty[1-4]", Arrays.asList("Missing required property."));
            Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
        } finally {
            if (model1 != null) {
                rr.delete(model1);
            }
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }
    
    @Test
    public void testResourceWithMultivalueProperties() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        TestProperty property = new TestProperty("field1");
        final String TEST_REGEX = "^testvalue.*$";
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator", "regex="+TEST_REGEX);
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, property);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("field1", new String[] {"testvalue1", "test2value", "testvalue3"});
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            assertFalse(vr.isValid());
            // check for correct error message
            Map<String, List<String>> expectedFailureMessages = new HashMap<String, List<String>>();
            expectedFailureMessages.put("field1[1]", Arrays.asList("Property does not match the pattern " + TEST_REGEX));
            Assert.assertThat(vr.getFailureMessages().entrySet(), Matchers.equalTo(expectedFailureMessages.entrySet()));
        } finally {
            if (model1 != null) {
                rr.delete(model1);
            }
            if (rr != null) {
                rr.commit();
                rr.close();
            }
        }
    }

    private Resource createValidationModelResource(ResourceResolver rr, String root, String name, String validatedResourceType,
                                               String[] applicableResourcePaths, TestProperty... properties) throws Exception {
        Map<String, Object> modelProperties = new HashMap<String, Object>();
        modelProperties.put(Constants.VALIDATED_RESOURCE_TYPE, validatedResourceType);
        modelProperties.put(Constants.APPLICABLE_PATHS, applicableResourcePaths);
        modelProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, Constants.VALIDATION_MODEL_RESOURCE_TYPE);
        modelProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        Resource model = ResourceUtil.getOrCreateResource(rr, root + "/" + name, modelProperties, JcrResourceConstants.NT_SLING_FOLDER, true);
        if (model != null) {
            createValidationModelProperties(model, properties);
        }
        return model;
    }
    
    private void createValidationModelProperties(Resource model, TestProperty... properties) throws PersistenceException {
        ResourceResolver rr = model.getResourceResolver();
        if (properties.length == 0) {
            return;
        }
        Resource propertiesResource = ResourceUtil.getOrCreateResource(rr, model.getPath() + "/" + Constants
                .PROPERTIES, JcrConstants.NT_UNSTRUCTURED, null, true);
        if (propertiesResource != null) {
            for (TestProperty property : properties) {
                Map<String, Object> modelPropertyJCRProperties = new HashMap<String, Object>();
                modelPropertyJCRProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                Resource propertyResource = ResourceUtil.getOrCreateResource(rr, propertiesResource.getPath() + "/" + property.name,
                        modelPropertyJCRProperties, null, true);
                if (propertyResource != null) {
                    ModifiableValueMap values = propertyResource.adaptTo(ModifiableValueMap.class);
                    if (property.nameRegex != null) {
                        values.put(Constants.NAME_REGEX, property.nameRegex);
                    }
                    values.put(Constants.PROPERTY_MULTIPLE, property.multiple);
                    values.put(Constants.OPTIONAL, property.optional);
                    Resource validators = ResourceUtil.getOrCreateResource(rr,
                            propertyResource.getPath() + "/" + Constants.VALIDATORS,
                            JcrConstants.NT_UNSTRUCTURED, null, true);
                    if (validators != null) {
                        for (Map.Entry<String, String[]> v : property.validators.entrySet()) {
                            Map<String, Object> validatorProperties = new HashMap<String, Object>();
                            validatorProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                            if (v.getValue() != null) {
                                validatorProperties.put(Constants.VALIDATOR_ARGUMENTS, v.getValue());
                            }
                             ResourceUtil.getOrCreateResource(rr, validators.getPath() + "/" + v.getKey(), validatorProperties, null,
                                    true);
                        }
                    }
                }
            }
        }
    }
    
    private Resource createValidationModelChildResource(Resource parentResource, String name, String nameRegex, boolean isOptional, TestProperty... properties) throws PersistenceException {
        ResourceResolver rr = parentResource.getResourceResolver();
        Resource modelChildren = rr.create(parentResource, Constants.CHILDREN, new HashMap<String, Object>(){{
            put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        }});
        Resource child = rr.create(modelChildren, name, new HashMap<String, Object>(){{
            put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        }});
        ModifiableValueMap mvm = child.adaptTo(ModifiableValueMap.class);
        if (nameRegex != null) {
            mvm.put(Constants.NAME_REGEX, nameRegex);
        }
        mvm.put(Constants.OPTIONAL, isOptional);
        createValidationModelProperties(child, properties);
        return child;
    }

    private boolean arrayContainsString(String[] array, String string) {
        boolean result = false;
        if (array != null && string != null) {
            for (String s : array) {
                if (string.equals(s)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private class TestProperty {
        public boolean optional;
        public boolean multiple;
        final String name;
        String nameRegex;
        final Map<String, String[]> validators;

        TestProperty(String name) {
            validators = new HashMap<String, String[]>();
            this.name = name;
            this.nameRegex = null;
            this.optional = false;
            this.multiple = false;
        }
        
        TestProperty setNameRegex(String nameRegex) {
            this.nameRegex = nameRegex;
            return this;
        }
        
        TestProperty addValidator(String name, String... parameters) {
            validators.put(name, parameters);
            return this;
        }
    }

}
