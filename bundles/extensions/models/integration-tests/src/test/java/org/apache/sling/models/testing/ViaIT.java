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

import static org.junit.Assert.*;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.it.models.SourceObject;
import org.apache.sling.models.it.models.ViaModel;
import org.junit.Rule;
import org.junit.Test;

public class ViaIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "SM_Teleporter");

    @Test
    public void test() throws Exception {
        ResourceResolverFactory rrFactory = teleporter.getService(ResourceResolverFactory.class);
        AdapterManager adapterManager =  teleporter.getService(AdapterManager.class);

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
            SourceObject obj = new SourceObject(resource);

            ViaModel model = adapterManager.getAdapter(obj, ViaModel.class);

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
