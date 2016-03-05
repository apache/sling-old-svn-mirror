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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.impl.util.examplevalidators.DateValidator;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.spi.ValidationModelProvider;
import org.apache.sling.validation.spi.Validator;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;

@RunWith(MockitoJUnitRunner.class)
public class ValidationModelRetrieverImplTest {

    private ValidationModelRetrieverImpl validationModelRetriever;
    private Validator<?> dateValidator;
    private MultiMap applicablePathPerResourceType;
    private TestModelProvider modelProvider;
    
    @Mock
    private ResourceResolver resourceResolver;

    /**
     * Test model provider which only provides models for all resource types in map applicablePathPerResourceType with their according applicablePath!
     * In addition those models have an (empty) resource property with a name equal to validated resource type.
     *
     */
    class TestModelProvider implements ValidationModelProvider {
        int counter = 0;

        @Override
        public @Nonnull Collection<ValidationModel> getModel(@Nonnull String relativeResourceType,
                @Nonnull Map<String, Validator<?>> validatorsMap, @Nonnull ResourceResolver resourceResolver) {
            // make sure the date validator is passed along
            Assert.assertThat(validatorsMap,
                    Matchers.<String, Validator<?>> hasEntry(DateValidator.class.getName(), dateValidator));

            Collection<ValidationModel> models = new ArrayList<ValidationModel>();
            Collection<String> applicablePaths = (Collection<String>) applicablePathPerResourceType
                    .get(relativeResourceType);
            if (applicablePaths != null) {
                for (String applicablePath : applicablePaths) {
                    ValidationModelBuilder modelBuilder = new ValidationModelBuilder();
                    if (applicablePath != null) {
                        modelBuilder.addApplicablePath(applicablePath);
                    }
                    modelBuilder.resourceProperty(new ResourcePropertyBuilder().build(relativeResourceType));
                    models.add(modelBuilder.build(relativeResourceType));
                }
            }
            counter++;
            return models;
        }
    }

    /**
     * Custom Hamcrest matcher which matches Resource Properties based on the equality only on their name.
     */
    private static final class ResourcePropertyNameMatcher extends TypeSafeMatcher<ResourceProperty> {

        private final String expectedName;

        public ResourcePropertyNameMatcher(String name) {
            expectedName = name;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("ResourceProperty with name=" + expectedName);
        }

        @Override
        protected boolean matchesSafely(ResourceProperty resourceProperty) {
           return expectedName.equals(resourceProperty.getName());
        }
    }

    @Before
    public void setup() {
        dateValidator = new DateValidator();
        applicablePathPerResourceType = new MultiHashMap();
        validationModelRetriever = new ValidationModelRetrieverImpl();
        modelProvider = new TestModelProvider();
        // service id must be set (even if service ranking is not set)
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Constants.SERVICE_ID, 1L);
        validationModelRetriever.bindModelProvider(modelProvider, properties);
        validationModelRetriever.bindValidator(dateValidator);
    }

    @Test
    public void testGetModel() {
        applicablePathPerResourceType.put("test/type", "/content/site1");
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode/test");
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode");

        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1/subnode/test/somepage",
                false, resourceResolver);
        Assert.assertNotNull(model);
        Assert.assertThat(Arrays.asList(model.getApplicablePaths()), Matchers.contains("/content/site1/subnode/test"));
    }

    @Test
    public void testGetModelWithNullApplicablePathPath() {
        applicablePathPerResourceType.put("test/type", "/content/site1");
        applicablePathPerResourceType.put("test/type", null);
        applicablePathPerResourceType.put("test/type", "/content/site1/subnode");

        ValidationModel model = validationModelRetriever.getModel("test/type", null, false, resourceResolver);
        Assert.assertNotNull(model);
        Assert.assertThat(Arrays.asList(model.getApplicablePaths()), Matchers.contains(""));
    }

    @Test
    public void testGetCachedModel() {
        applicablePathPerResourceType.put("test/type", "/content/site1");
        // call two times, the second time the counter must be the same (because provider is not called)
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1", false, resourceResolver);
        Assert.assertNotNull(model);
        Assert.assertEquals(1, modelProvider.counter);
        model = validationModelRetriever.getModel("test/type", "/content/site1", false, resourceResolver);
        Assert.assertNotNull(model);
        Assert.assertEquals(1, modelProvider.counter);

        model = validationModelRetriever.getModel("invalid/type", "/content/site1", false, resourceResolver);
        Assert.assertNull(model);
        Assert.assertEquals(2, modelProvider.counter);
        model = validationModelRetriever.getModel("invalid/type", "/content/site1", false, resourceResolver);
        Assert.assertNull(model);
        Assert.assertEquals(2, modelProvider.counter);
    }

    @Test
    public void testGetCachedInvalidation() {
        applicablePathPerResourceType.put("test/type", "/content/site1");
        validationModelRetriever.getModel("test/type", "/content/site1", false, resourceResolver);
        Assert.assertEquals(1, modelProvider.counter);
        validationModelRetriever.handleEvent(new Event(ValidationModelRetrieverImpl.CACHE_INVALIDATION_EVENT_TOPIC,
                null));
        // after cache invalidation the provider is called again
        validationModelRetriever.getModel("test/type", "/content/site1", false, resourceResolver);
        Assert.assertEquals(2, modelProvider.counter);
    }

    @Test
    public void testGetModelWithResourceInheritance() {
        // in case no super type is known, just return model
        applicablePathPerResourceType.put("test/type", "/content/site1");
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1", true, resourceResolver);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.contains(new ResourcePropertyNameMatcher("test/type")));
        // in case there is one super type make sure the merged model is returned!
        Mockito.when(resourceResolver.getParentResourceType("test/type")).thenReturn("test/supertype");
        applicablePathPerResourceType.put("test/supertype", "/content/site1");
        model = validationModelRetriever.getModel("test/type", "/content/site1", true, resourceResolver);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.containsInAnyOrder(new ResourcePropertyNameMatcher("test/type"), new ResourcePropertyNameMatcher("test/supertype")));
    }
    
    @Test
    public void testGetModelWithResourceInheritanceAndNoSuitableBaseModelFound() {
        // no model found for base type and no resource super type set
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1", true, resourceResolver);
        Assert.assertNull("Found model although no model has been specified", model);
        
        // set super super type
        Mockito.when(resourceResolver.getParentResourceType("test/type")).thenReturn("test/supertype");
        // no model found at all (neither base nor super type)
        model = validationModelRetriever.getModel("test/type", "/content/site1", true, resourceResolver);
        Assert.assertNull("Found model although no model has been specified (neither in base nor in super type)", model);
        
        validationModelRetriever.validationModelsCache.clear();
        
        // only supertype has model being set
        applicablePathPerResourceType.put("test/supertype", "/content/site1");
        model = validationModelRetriever.getModel("test/type", "/content/site1", true, resourceResolver);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.contains(new ResourcePropertyNameMatcher("test/supertype")));
    }
    
    @Test
    public void testGetModelWithResourceInheritanceAndNoModelForSuperTypeFound() {
        applicablePathPerResourceType.put("test/type", "/content/site1");
        Mockito.when(resourceResolver.getParentResourceType("test/type")).thenReturn("test/supertype");
        Mockito.when(resourceResolver.getParentResourceType("test/supertype")).thenReturn("test/supersupertype");
        
        // only model found for base type
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1", true, resourceResolver);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.contains(new ResourcePropertyNameMatcher("test/type")));
    }
}
