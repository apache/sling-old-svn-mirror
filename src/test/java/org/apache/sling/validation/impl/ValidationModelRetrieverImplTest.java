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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.validation.Validator;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.impl.util.examplevalidators.DateValidator;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.spi.ValidationModelProvider;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;

public class ValidationModelRetrieverImplTest {

    private ValidationModelRetrieverImpl validationModelRetriever;
    private Validator<?> dateValidator;
    private Map<String, String> propertyNamePerApplicablePath;
    private TestModelProvider modelProvider;

    /**
     * Test model provider which only provides models for resource types starting with "test"
     *
     */
    class TestModelProvider implements ValidationModelProvider {
        int counter = 0;

        @Override
        public @Nonnull Collection<ValidationModel> getModel(@Nonnull String relativeResourceType, @Nonnull Map<String, Validator<?>> validatorsMap) {
            // make sure the date validator is passed along
            Assert.assertThat(validatorsMap, Matchers.<String, Validator<?>>hasEntry(DateValidator.class.getName(), dateValidator));

            Collection<ValidationModel> models = new ArrayList<ValidationModel>();
            if (relativeResourceType.startsWith("test")) {
                for (Map.Entry<String, String> entry : propertyNamePerApplicablePath.entrySet()) {
                    ValidationModelBuilder modelBuilder = new ValidationModelBuilder();
                    ResourcePropertyBuilder propertyBuilder = new ResourcePropertyBuilder();
                    modelBuilder.resourceProperty(propertyBuilder.build(entry.getValue()));
                    String applicablePath = entry.getKey();
                    if (applicablePath != null) {
                        modelBuilder.addApplicablePath(applicablePath);
                    }
                    models.add(modelBuilder.build(relativeResourceType));
                }
            }
            counter++;
            return models;
        }
    }

    @Before
    public void setup() {
        dateValidator = new DateValidator();
        propertyNamePerApplicablePath = new HashMap<String, String>();
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
        propertyNamePerApplicablePath.put("/content/site1", "somename");
        propertyNamePerApplicablePath.put("/content/site1/subnode/test", "somename2");
        propertyNamePerApplicablePath.put("/content/site1/subnode", "somename3");
        
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1/subnode/test/somepage");
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.hasSize(1));
        ResourceProperty resourceProperty = model.getResourceProperties().get(0);
        Assert.assertEquals("somename2", resourceProperty.getName());
    }

    @Test
    public void testGetModelWithNoResourcePath() {
        propertyNamePerApplicablePath.put("/content/site1", "somename");
        propertyNamePerApplicablePath.put(null, "somename2");
        propertyNamePerApplicablePath.put("/content/site1/subnode", "somename3");
        
        ValidationModel model = validationModelRetriever.getModel("test/type", null);
        Assert.assertNotNull(model);
        Assert.assertThat(model.getResourceProperties(), Matchers.hasSize(1));
        ResourceProperty resourceProperty = model.getResourceProperties().get(0);
        Assert.assertEquals("somename2", resourceProperty.getName());
    }

    @Test
    public void testGetCachedModel() {
        propertyNamePerApplicablePath.put("/content/site1", "somename");
        // call two times, the second time the counter must be the same (because provider is not called)
        ValidationModel model = validationModelRetriever.getModel("test/type", "/content/site1");
        Assert.assertNotNull(model);
        Assert.assertEquals(1, modelProvider.counter);
        model = validationModelRetriever.getModel("test/type", "/content/site1");
        Assert.assertNotNull(model);
        Assert.assertEquals(1, modelProvider.counter);
        
        model = validationModelRetriever.getModel("invalid/type", "/content/site1");
        Assert.assertNull(model);
        Assert.assertEquals(2, modelProvider.counter);
        model = validationModelRetriever.getModel("invalid/type", "/content/site1");
        Assert.assertNull(model);
        Assert.assertEquals(2, modelProvider.counter);
    }

    @Test
    public void testGetCachedInvalidation() {
        propertyNamePerApplicablePath.put("/content/site1", "somename");
        validationModelRetriever.getModel("test/type", "/content/site1");
        Assert.assertEquals(1, modelProvider.counter);
        validationModelRetriever.handleEvent(new Event(ValidationModelRetrieverImpl.CACHE_INVALIDATION_EVENT_TOPIC, null));
        // after cache invalidation the provider is called again
        validationModelRetriever.getModel("test/type", "/content/site1");
        Assert.assertEquals(2, modelProvider.counter);
        
    }
}
