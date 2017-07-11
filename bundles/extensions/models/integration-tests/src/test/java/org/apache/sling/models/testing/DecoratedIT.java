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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.it.models.SelfModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Session;

import static org.junit.Assert.*;

public class DecoratedIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "SM_Teleporter");

    private ModelFactory modelFactory;

    private ResourceResolver resolver;
    private Resource resourceWithDefaultWrapperBehavior;
    private Resource resourceWithCustomAdaptToWrapper;
    
    @Before
    public void setUp() throws Exception {
        ResourceResolverFactory rrFactory = teleporter.getService(ResourceResolverFactory.class);
        modelFactory = teleporter.getService(ModelFactory.class);
        resolver = rrFactory.getAdministrativeResourceResolver(null);
        Session session = resolver.adaptTo(Session.class);
        Node rootNode = session.getRootNode();
        Node createdNode = rootNode.addNode("test_" + RandomStringUtils.randomAlphanumeric(10));
        createdNode.setProperty("decorate", true);
        session.save();

        resourceWithDefaultWrapperBehavior = resolver.getResource(createdNode.getPath());

        createdNode = rootNode.addNode("test_" + RandomStringUtils.randomAlphanumeric(10));
        createdNode.setProperty("decorate", "customAdaptTo");
        session.save();

        resourceWithCustomAdaptToWrapper = resolver.getResource(createdNode.getPath());
    }

    @After
    public void tearDown() throws Exception {
        resolver.delete(resourceWithDefaultWrapperBehavior);
        resolver.delete(resourceWithCustomAdaptToWrapper);
        resolver.close();
    }

    @Test
    public void testInjectDecoratedResourceUsingCreateModel() {
        assertTrue("Resource is not wrapped", resourceWithDefaultWrapperBehavior instanceof ResourceWrapper);
        SelfModel model = modelFactory.createModel(resourceWithDefaultWrapperBehavior, SelfModel.class);

        assertNotNull("Model is null", model);
        assertTrue("Model is not wrapped", model.getResource() instanceof ResourceWrapper);
    }

    @Test
    public void testInjectDecoratedResourceUsingAdaptTo() {
        assertTrue("Resource is not wrapped", resourceWithCustomAdaptToWrapper instanceof ResourceWrapper);
        SelfModel model = resourceWithCustomAdaptToWrapper.adaptTo(SelfModel.class);
    
        assertNotNull("Model is null", model);
        assertTrue("Model is not wrapped", model.getResource() instanceof ResourceWrapper);
    }

}
