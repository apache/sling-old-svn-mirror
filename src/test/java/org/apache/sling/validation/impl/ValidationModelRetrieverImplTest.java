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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.impl.util.ResourcePropertyNameMatcher;
import org.apache.sling.validation.impl.util.examplevalidators.DateValidator;
import org.apache.sling.validation.impl.util.examplevalidators.StringValidator;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.ValidatorAndSeverity;
import org.apache.sling.validation.model.spi.ValidationModelProvider;
import org.apache.sling.validation.spi.Validator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class ValidationModelRetrieverImplTest {

    private ValidationModelRetrieverImpl validationModelRetriever;
    private Validator<Date> dateValidator;
    private MultiValuedMap<String, String> applicablePathPerResourceType;
    private TestModelProvider modelProvider;
    
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private ResourceResolverFactory resourceResolverFactory;
    @Mock
    private ServiceReference<Validator<?>> validatorServiceReference;
    @Mock
    private ServiceReference<Validator<?>> newValidatorServiceReference;
    @Mock
    private Bundle providingBundle;
    
    private static final String DATE_VALIDATOR_ID = "DateValidator";

    /**
     * Test model provider which only provides models for all resource types in map applicablePathPerResourceType with their according applicablePath!
     * In addition those models have an (empty) resource property with a name equal to validated resource type.
     *
     */
    class TestModelProvider implements ValidationModelProvider {
        private @Nonnull final String source;
        public TestModelProvider(@Nonnull String source) {
            this.source = source;
        }
        
        @Override
        public @Nonnull List<ValidationModel> getModels(@Nonnull String relativeResourceType,
                @Nonnull Map<String, ValidatorAndSeverity<?>> validatorsMap) {
            // make sure the date validator is passed along
            Assert.assertThat(validatorsMap,
                    Matchers.<String, ValidatorAndSeverity<?>> hasEntry(DATE_VALIDATOR_ID, new ValidatorAndSeverity<Date>(dateValidator, 1)));

            List<ValidationModel> models = new ArrayList<ValidationModel>();
            Collection<String> applicablePaths = applicablePathPerResourceType.get(relativeResourceType);
            if (applicablePaths != null) {
                for (String applicablePath : applicablePaths) {
                    ValidationModelBuilder modelBuilder = new ValidationModelBuilder();
                    if (applicablePath != null) {
                        modelBuilder.addApplicablePath(applicablePath);
                    }
                    modelBuilder.resourceProperty(new ResourcePropertyBuilder().build(relativeResourceType));
                    models.add(modelBuilder.build(relativeResourceType, source));
                }
            }
            return models;
        }
    }

    @Before
    public void setup() throws LoginException {
        dateValidator =  new DateValidator();
        applicablePathPerResourceType = new ArrayListValuedHashMap<>();
        validationModelRetriever = new ValidationModelRetrieverImpl();
        modelProvider = new TestModelProvider("source1");
        validationModelRetriever.modelProviders = new ArrayList<>();
        validationModelRetriever.modelProviders.add(modelProvider);
        Mockito.doReturn(1l).when(providingBundle).getBundleId();
        Mockito.doReturn(providingBundle).when(validatorServiceReference).getBundle();
        Mockito.doReturn(providingBundle).when(newValidatorServiceReference).getBundle();
        Map<String, Object> validatorProperties = new HashMap<>();
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_ID, DATE_VALIDATOR_ID);
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_SEVERITY, 1);
        validationModelRetriever.addValidator(dateValidator, validatorProperties, validatorServiceReference);
        validationModelRetriever.resourceResolverFactory = resourceResolverFactory;
        Mockito.when(resourceResolverFactory.getServiceResourceResolver(Mockito.anyObject())).thenReturn(resourceResolver);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testAddValidatorWithoutValidatorIdProperty() {
        Map<String, Object> validatorProperties = new HashMap<>();
        validationModelRetriever.addValidator(dateValidator, validatorProperties, validatorServiceReference);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testAddValidatorWithWronglyTypedValidatorId() {
        Map<String, Object> validatorProperties = new HashMap<>();
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_ID, new String[]{"some", "value"});
        validationModelRetriever.addValidator(dateValidator, validatorProperties, validatorServiceReference);
    }

    @Test
    public void testAddOverloadingValidatorWithSameValidatorIdAndHigherRanking() {
        Map<String, Object> validatorProperties = new HashMap<>();
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_ID, DATE_VALIDATOR_ID);
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_SEVERITY, 2);
        Mockito.doReturn(1).when(newValidatorServiceReference).compareTo(Mockito.anyObject());
        Validator<String> stringValidator = new StringValidator();
        validationModelRetriever.addValidator(stringValidator, validatorProperties, newValidatorServiceReference);
        Assert.assertEquals(new ValidatorAndSeverity<>(stringValidator, 2), validationModelRetriever.validators.get(DATE_VALIDATOR_ID));
        Assert.assertEquals(newValidatorServiceReference, validationModelRetriever.validatorServiceReferences.get(DATE_VALIDATOR_ID));
    }
    
    @Test
    public void testAddOverloadingValidatorWithSameValidatorIdAndLowerRanking() {
        Map<String, Object> validatorProperties = new HashMap<>();
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_ID, DATE_VALIDATOR_ID);
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_SEVERITY, 2);
        Mockito.doReturn(-1).when(newValidatorServiceReference).compareTo(Mockito.anyObject());
        Validator<String> stringValidator = new StringValidator();
        validationModelRetriever.addValidator(stringValidator, validatorProperties, newValidatorServiceReference);
        Assert.assertEquals(new ValidatorAndSeverity<>(dateValidator, 1), validationModelRetriever.validators.get(DATE_VALIDATOR_ID));
        Assert.assertEquals(validatorServiceReference, validationModelRetriever.validatorServiceReferences.get(DATE_VALIDATOR_ID));
    }

    @Test
    public void testGetModel() {
        applicablePathPerResourceType.put("test/type", "/content/site1");
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode/test");
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode");
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode/test/somepage/within");
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode/test/testoutside");

        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1/subnode/test/somepage", false);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getApplicablePaths(), Matchers.contains("/content/site1/subnode/test"));
    }
    
    @Test
    public void testGetModelWithExactlyMatchingApplicablePath() {
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode/test/somepage");
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode/test/somepage/");

        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1/subnode/test/somepage", false);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getApplicablePaths(), Matchers.contains("/content/site1/subnode/test/somepage"));
    }

    @Test
    public void testGetModelWithNullApplicablePathPath() {
        applicablePathPerResourceType.put("test/type", "/content/site1");
        applicablePathPerResourceType.put("test/type", "");
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode");

        ValidationModel model = validationModelRetriever.getModel("test/type", null, false);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getApplicablePaths(), Matchers.contains(""));
    }

    @Test
    public void testGetModelWithResourceInheritance() {
        // in case no super type is known, just return model
        applicablePathPerResourceType.put("test/type", "/content/site1");
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1", true);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.contains(new ResourcePropertyNameMatcher("test/type")));
        // in case there is one super type make sure the merged model is returned!
        Mockito.when(resourceResolver.getParentResourceType("test/type")).thenReturn("test/supertype");
        applicablePathPerResourceType.put("test/supertype", "/content/site1");
        model = validationModelRetriever.getModel("test/type", "/content/site1", true);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.<ResourceProperty>containsInAnyOrder(Arrays.asList(new ResourcePropertyNameMatcher("test/type"), new ResourcePropertyNameMatcher("test/supertype"))));
    }
    
    @Test
    public void testGetModelWithResourceInheritanceAndNoSuitableBaseModelFound() {
        // no model found for base type and no resource super type set
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1", true);
        Assert.assertNull("Found model although no model has been specified", model);
        
        // set super super type
        Mockito.when(resourceResolver.getParentResourceType("test/type")).thenReturn("test/supertype");
        // no model found at all (neither base nor super type)
        model = validationModelRetriever.getModel("test/type", "/content/site1", true);
        Assert.assertNull("Found model although no model has been specified (neither in base nor in super type)", model);
        
        // only supertype has model being set
        applicablePathPerResourceType.put("test/supertype", "/content/site1");
        model = validationModelRetriever.getModel("test/type", "/content/site1", true);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.contains(new ResourcePropertyNameMatcher("test/supertype")));
    }
    
    @Test
    public void testGetModelWithResourceInheritanceAndNoModelForSuperTypeFound() {
        applicablePathPerResourceType.put("test/type", "/content/site1");
        Mockito.when(resourceResolver.getParentResourceType("test/type")).thenReturn("test/supertype");
        Mockito.when(resourceResolver.getParentResourceType("test/supertype")).thenReturn("test/supersupertype");
        
        // only model found for base type
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1", true);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.contains(new ResourcePropertyNameMatcher("test/type")));
    }
    
    @Test
    public void testGetModelWithMultipleProvidersHigherRanking() {
        ValidationModelProvider modelProvider2 = new TestModelProvider("source2");
        validationModelRetriever.modelProviders.clear();
        validationModelRetriever.modelProviders.add(modelProvider);
        validationModelRetriever.modelProviders.add(modelProvider2);
        // each provider must return the same applicable path but different
        applicablePathPerResourceType.put("test/type", "/content/site1");

        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1/somepage", false);
        Assert.assertNotNull(model);
        Assert.assertEquals("source2", model.getSource());
        
    }
    
    @Test
    public void testGetModelWithMultipleProvidersLowerRanking() {
        ValidationModelProvider modelProvider2 = new TestModelProvider("source2");
        validationModelRetriever.modelProviders.clear();
        validationModelRetriever.modelProviders.add(modelProvider2);
        validationModelRetriever.modelProviders.add(modelProvider);
        // each provider must return the same applicable path but different
        applicablePathPerResourceType.put("test/type", "/content/site1");

        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1/somepage", false);
        Assert.assertNotNull(model);
        Assert.assertEquals("source1", model.getSource());
        
    }
}
