/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.it.models.ConstructorInjectionTestModel;
import org.apache.sling.models.it.models.FieldInjectionTestModel;
import org.apache.sling.models.it.models.implextend.InvalidImplementsInterfacePropertyModel;
import org.apache.sling.models.it.models.implextend.SampleServiceInterface;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ModelFactorySimpleIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "SM_Teleporter");

    private ModelFactory modelFactory;

    private String value;
    private ResourceResolver resolver;
    private Resource resource;
    private Node createdNode;

    @Before
    public void setUp() throws Exception {
        ResourceResolverFactory rrFactory = teleporter.getService(ResourceResolverFactory.class);
        modelFactory = teleporter.getService(ModelFactory.class);
        value = RandomStringUtils.randomAlphanumeric(10);

        resolver = rrFactory.getAdministrativeResourceResolver(null);
        Session session = resolver.adaptTo(Session.class);
        Node rootNode = session.getRootNode();
        createdNode = rootNode.addNode("test_" + RandomStringUtils.randomAlphanumeric(10));
        createdNode.setProperty("testProperty", value);
        session.save();

        resource = resolver.getResource(createdNode.getPath());
    }

    @After
    public void tearDown() throws Exception {
        if (createdNode != null) {
            createdNode.remove();
        }
        if (resolver != null) {
            resolver.close();
        }
    }

    @Test
    public void testCreateModel() {
        FieldInjectionTestModel model = modelFactory.createModel(resource, FieldInjectionTestModel.class);

        assertNotNull("Model is null", model);
        assertEquals("Test Property is not set correctly", value, model.getTestProperty());
        assertNotNull("Filters is null", model.getFilters());
        assertSame("Adaptable is not injected", resource, model.getResource());
    }

    private static final class DummyClass {
    }

    @Test
    public void testIsModelClass() {
        assertTrue("Model is not detected as such", modelFactory.isModelClass(ConstructorInjectionTestModel.class));
        assertFalse("Dummy class incorrectly detected as model class", modelFactory.isModelClass(DummyClass.class));
        assertFalse("Model with invalid adaptable incorrectly detected as model class" , modelFactory.isModelClass(InvalidImplementsInterfacePropertyModel.class));
        assertTrue("Model is not detected as such", modelFactory.isModelClass(SampleServiceInterface.class)); // being provided by two adapters
    }

    @Test
    public void testCanCreateFromAdaptable() {
        assertTrue("Model is not detected as such", modelFactory.canCreateFromAdaptable(resource, ConstructorInjectionTestModel.class));
        assertTrue("Model is not detected as such", modelFactory.canCreateFromAdaptable(resource, SampleServiceInterface.class));
        assertFalse("Model is incorrectly detected", modelFactory.canCreateFromAdaptable(new String(), ConstructorInjectionTestModel.class)); // invalid adaptable
    }
    
    @Test()
    public void testCanCreateFromAdaptableWithModelExceptin() {
        assertFalse("Model is incorrectly detected", modelFactory.canCreateFromAdaptable(resource, DummyClass.class)); // no model class
    }
}
