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
package org.apache.sling.models.it;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.it.models.SelfModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jcr.Node;
import javax.jcr.Session;

import static org.junit.Assert.*;

@RunWith(SlingAnnotationsTestRunner.class)
public class DecoratedTest {

    @TestReference
    private ResourceResolverFactory rrFactory;

    @TestReference
    private ModelFactory modelFactory;
    
    private String value;
    private ResourceResolver resolver;
    private Resource resource;
    private Node createdNode;
    
    @Before
    public void setUp() throws Exception {
        value = RandomStringUtils.randomAlphanumeric(10);

        resolver = rrFactory.getAdministrativeResourceResolver(null);
        Session session = resolver.adaptTo(Session.class);
        Node rootNode = session.getRootNode();
        createdNode = rootNode.addNode("test_" + RandomStringUtils.randomAlphanumeric(10));
        createdNode.setProperty("testProperty", value);
        createdNode.setProperty("decorate", true);
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
    public void testInjectDecoratedResource() {
        SelfModel model = modelFactory.createModel(resource, SelfModel.class);
        assertTrue("Resource is not wrapped", resource instanceof ResourceWrapper);
    
        assertNotNull("Model is null", model);
        assertTrue("Model is not wrapped", model.getResource() instanceof ResourceWrapper);
    }

}
