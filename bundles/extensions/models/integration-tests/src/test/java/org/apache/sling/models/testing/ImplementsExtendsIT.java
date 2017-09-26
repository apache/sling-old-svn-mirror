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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.it.implpicker.CustomLastImplementationPicker;
import org.apache.sling.models.it.models.implextend.ImplementsInterfacePropertyModel;
import org.apache.sling.models.it.models.implextend.ImplementsInterfacePropertyModel2;
import org.apache.sling.models.it.models.implextend.InvalidSampleServiceInterface;
import org.apache.sling.models.it.models.implextend.SampleServiceInterface;
import org.apache.sling.models.it.models.implextend.SimplePropertyModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ImplementsExtendsIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "SM_Teleporter");

    private AdapterManager adapterManager;

    private String firstValue;
    private String secondValue;
    private String thirdValue;
    private ResourceResolver resolver;
    private Resource resource;
    private Node createdNode;
    
    @Before
    public void setUp() throws Exception {
        ResourceResolverFactory rrFactory = teleporter.getService(ResourceResolverFactory.class);
        adapterManager = teleporter.getService(AdapterManager.class);
        firstValue = RandomStringUtils.randomAlphanumeric(10);
        thirdValue = RandomStringUtils.randomAlphanumeric(10);

        resolver = rrFactory.getAdministrativeResourceResolver(null);     
        Session session = resolver.adaptTo(Session.class);
        Node rootNode = session.getRootNode();
        createdNode = rootNode.addNode("test_" + RandomStringUtils.randomAlphanumeric(10));
        createdNode.setProperty("first", firstValue);
        createdNode.setProperty("third", thirdValue);
        session.save();

        resource = resolver.getResource(createdNode.getPath());
    }

    @After
    public void after() throws Exception {

        if (resolver != null) {
            resolver.close();
        }
    }
    
    /**
     * Try to adapt to interface, with an different implementation class that has the @Model annotation
     */
    @Test
    public void testImplementsInterfaceModel() {
        SampleServiceInterface model = adapterManager.getAdapter(resource, SampleServiceInterface.class);
        assertNotNull(model);
        assertEquals(ImplementsInterfacePropertyModel.class, model.getClass());
        assertEquals(firstValue + "|" + secondValue + "|" + thirdValue, model.getAllProperties());
    }

    /**
     * Ensure that the implementation class itself can be adapted to, even if it is not part of the "adapter" property in the annotation.
     */
    @Test
    public void testImplementsInterfaceModel_ImplClassImplicitlyMapped() {
        ImplementsInterfacePropertyModel model = adapterManager.getAdapter(resource, ImplementsInterfacePropertyModel.class);
        assertNotNull(model);
    }

    /**
     * Test implementation class with a mapping that is not valid (an interface that is not implemented).
     */
    @Test
    public void testInvalidImplementsInterfaceModel() {
        InvalidSampleServiceInterface model = adapterManager.getAdapter(resource, InvalidSampleServiceInterface.class);
        assertNull(model);
    }

    /**
     * Test to adapt to a superclass of the implementation class with the appropriate mapping in the @Model annotation.
     */
    @Test
    public void testExtendsClassModel() {
        SimplePropertyModel model = adapterManager.getAdapter(resource, SimplePropertyModel.class);
        assertNotNull(model);
        assertEquals("!" + firstValue + "|" + secondValue + "|" + thirdValue + "!", model.getAllProperties());
    }
    

    /**
     * Try to adapt to interface, with an different implementation class that has the @Model annotation
     */
    @Test
    public void testImplementsInterfaceModelWithPickLastImplementationPicker() throws RepositoryException {
        
        Session session = resolver.adaptTo(Session.class);
        Node node = resource.adaptTo(Node.class);
        Node childNode = node.addNode(CustomLastImplementationPicker.CUSTOM_NAME);
        childNode.setProperty("first", firstValue);
        childNode.setProperty("third", thirdValue);
        session.save();
        
        Resource childResource = resolver.getResource(childNode.getPath());
        
        SampleServiceInterface model = adapterManager.getAdapter(childResource, SampleServiceInterface.class);
        assertNotNull(model);
        assertEquals(ImplementsInterfacePropertyModel2.class, model.getClass());
        assertEquals(firstValue + "|" + secondValue + "|" + thirdValue, model.getAllProperties());
        
        childNode.remove();
        session.save();
    }

}
