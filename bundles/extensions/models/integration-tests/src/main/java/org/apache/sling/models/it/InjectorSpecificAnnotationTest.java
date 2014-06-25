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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.sling.models.it.models.SlingPropertyAnnotationTestModel;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SlingAnnotationsTestRunner.class)
public class InjectorSpecificAnnotationTest {

    @TestReference
    private ResourceResolverFactory rrFactory;

    @TestReference
    private AdapterManager adapterManager;

    @Test
    public void test() throws Exception {
    String value = RandomStringUtils.randomAlphanumeric(10);

    ResourceResolver resolver = null;
    Node createdNode = null;
    try {
        resolver = rrFactory.getAdministrativeResourceResolver(null);
        Session session = resolver.adaptTo(Session.class);
        Node rootNode = session.getRootNode();
        createdNode = rootNode.addNode("test_" + RandomStringUtils.randomAlphanumeric(10));
        createdNode.setProperty("testProperty", value);
        session.save();

        Resource resource = resolver.getResource(createdNode.getPath());

        SlingPropertyAnnotationTestModel model = resource.adaptTo(SlingPropertyAnnotationTestModel.class);

        assertNotNull("Model is null", model);
        assertEquals("Test Property is not set correctly", value, model.getTestProperty());
    } finally {
        if (createdNode != null) {
        createdNode.remove();
        }
        if (resolver != null) {
        resolver.close();
        }
    }
    }
}
