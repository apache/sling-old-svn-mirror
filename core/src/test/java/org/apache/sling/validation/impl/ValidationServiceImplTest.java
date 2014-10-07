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

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.validation.api.Type;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.ValidationResult;
import org.apache.sling.validation.api.ValidationService;
import org.apache.sling.validation.api.ValidatorLookupService;
import org.apache.sling.validation.impl.setup.MockedResourceResolver;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private ValidationService validationService;
    private ValidatorLookupService validatorLookupService;

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
            appsValidatorsRoot = ResourceUtil.getOrCreateResource(rr, APPS + "/" + VALIDATION_MODELS_RELATIVE_PATH, (Map) null,
                    "sling:Folder", true);
            libsValidatorsRoot = ResourceUtil.getOrCreateResource(rr, LIBS + "/" + VALIDATION_MODELS_RELATIVE_PATH, (Map) null,
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
        Whitebox.setInternalState(validationService, "rrf", rrf);
        validatorLookupService = mock(ValidatorLookupService.class);
    }

    @Test
    public void testGetValidationModel() throws Exception {
        when(validatorLookupService.getValidator("org.apache.sling.validation.impl.validators.RegexValidator")).thenReturn(new
                RegexValidator());
        Whitebox.setInternalState(validationService, "validatorLookupService", validatorLookupService);

        List<TestProperty> properties = new ArrayList<TestProperty>();
        TestProperty property = new TestProperty();
        property.name = "field1";
        property.type = Type.DATE;
        property.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", null);
        properties.add(property);
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null, model2 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, properties);
                model2 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel2", "sling/validation/test",
                        new String[]{"/apps/validation/1",
                        "/apps/validation/2"}, properties);
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
        when(validatorLookupService.getValidator("org.apache.sling.validation.impl.validators.RegexValidator")).thenReturn(new
                RegexValidator());
        Whitebox.setInternalState(validationService, "validatorLookupService", validatorLookupService);

        List<TestProperty> fields = new ArrayList<TestProperty>();
        TestProperty field = new TestProperty();
        field.name = "field1";
        field.type = Type.DATE;
        field.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", null);
        fields.add(field);
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null, model2 = null, model3 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation/1"}, fields);
                model2 = createValidationModelResource(rr, appsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation/1",
                                "/apps/validation/2"}, fields);
                model3 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel2", "sling/validation/test",
                        new String[]{"/apps/validation/3"}, fields);
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

    @Test
    public void testValueMapWithWrongDataType() throws Exception {
        when(validatorLookupService.getValidator("org.apache.sling.validation.impl.validators.RegexValidator")).thenReturn(new
                RegexValidator());
        Whitebox.setInternalState(validationService, "validatorLookupService", validatorLookupService);

        List<TestProperty> properties = new ArrayList<TestProperty>();
        TestProperty property = new TestProperty();
        property.name = "field1";
        property.type = Type.DATE;
        property.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", null);
        properties.add(property);
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, properties);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("field1", "1");
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            assertFalse(vr.isValid());
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
    public void testValueMapWithCorrectDataType() throws Exception {
        when(validatorLookupService.getValidator("org.apache.sling.validation.impl.validators.RegexValidator")).thenReturn(new
                RegexValidator());
        Whitebox.setInternalState(validationService, "validatorLookupService", validatorLookupService);

        List<TestProperty> fields = new ArrayList<TestProperty>();
        TestProperty field = new TestProperty();
        field.name = "field1";
        field.type = Type.STRING;
        field.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new String[] {"regex=^\\p{L}+$"});
        fields.add(field);
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, fields);
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
                put("field1", "HelloWorld");
            }};
            ValueMap map = new ValueMapDecorator(hashMap);
            ValidationResult vr = validationService.validate(map, vm);
            assertTrue(vr.isValid());
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
        when(validatorLookupService.getValidator("org.apache.sling.validation.impl.validators.RegexValidator")).thenReturn(new
                RegexValidator());
        Whitebox.setInternalState(validationService, "validatorLookupService", validatorLookupService);

        List<TestProperty> fields = new ArrayList<TestProperty>();
        TestProperty property = new TestProperty();
        property.name = "field1";
        property.type = Type.INT;
        property.validators.put("org.apache.sling.validation.impl.validators.RegexValidator", new String[] {RegexValidator.REGEX_PARAM + "=" + "\\d"});
        fields.add(property);
        ResourceResolver rr = rrf.getAdministrativeResourceResolver(null);
        Resource model1 = null;
        Resource testResource = null;
        try {
            if (rr != null) {
                model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", "sling/validation/test",
                        new String[]{"/apps/validation"}, fields);
                Resource modelChildren = rr.create(model1, "children", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                Resource child = rr.create(modelChildren, "child1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                Resource childProperties = rr.create(child, "properties", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                Resource childProperty = rr.create(childProperties, "hello", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                    put(Constants.PROPERTY_TYPE, "string");
                }});
                Resource grandChildren = rr.create(child, "children", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                Resource grandChild = rr.create(grandChildren, "grandChild1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});

                testResource = ResourceUtil.getOrCreateResource(rr, "/apps/validation/1/resource", JcrConstants.NT_UNSTRUCTURED,
                        JcrConstants.NT_UNSTRUCTURED, true);
                Resource childResource = rr.create(testResource, "child1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                rr.commit();

                ModifiableValueMap mvm = testResource.adaptTo(ModifiableValueMap.class);
                mvm.put("field1", "1");
                rr.commit();

                // /apps/validation/1/resource/child1 will miss its mandatory "hello" property
                Resource resourceChild = rr.create(testResource, "child1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});

                Resource resourceGrandChild = rr.create(resourceChild, "grandChild1", new HashMap<String, Object>(){{
                    put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                }});
                rr.commit();
            }
            ValidationModel vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
            ValidationResult vr = validationService.validate(testResource, vm);
            assertFalse(vr.isValid());
            assertTrue(vr.getFailureMessages().containsKey("child1/hello"));
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

    private Resource createValidationModelResource(ResourceResolver rr, String root, String name, String validatedResourceType,
                                               String[] applicableResourcePaths, List<TestProperty> properties) throws Exception {
        Map<String, Object> modelProperties = new HashMap<String, Object>();
        modelProperties.put(Constants.VALIDATED_RESOURCE_TYPE, validatedResourceType);
        modelProperties.put(Constants.APPLICABLE_PATHS, applicableResourcePaths);
        modelProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, Constants.VALIDATION_MODEL_RESOURCE_TYPE);
        modelProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        Resource model = ResourceUtil.getOrCreateResource(rr, root + "/" + name, modelProperties, JcrResourceConstants.NT_SLING_FOLDER, true);
        if (model != null) {
            Resource propertiesResource = ResourceUtil.getOrCreateResource(rr, model.getPath() + "/" + Constants
                    .PROPERTIES, JcrConstants.NT_UNSTRUCTURED, null, true);
            if (propertiesResource != null) {
                for (TestProperty property : properties) {
                    Map<String, Object> modelPropertyJCRProperties = new HashMap<String, Object>();
                    modelPropertyJCRProperties.put(Constants.PROPERTY_TYPE, property.type.getName());
                    modelPropertyJCRProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                    Resource propertyResource = ResourceUtil.getOrCreateResource(rr, propertiesResource.getPath() + "/" + property.name,
                            modelPropertyJCRProperties, null, true);
                    if (propertyResource != null) {
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
        return model;
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
        String name;
        Type type;
        Map<String, String[]> validators;

        TestProperty() {
            validators = new HashMap<String, String[]>();
        }
    }

    private class TestChild {

    }

}
