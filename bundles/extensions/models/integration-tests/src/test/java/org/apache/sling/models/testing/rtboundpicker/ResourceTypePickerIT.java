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
package org.apache.sling.models.testing.rtboundpicker;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.it.rtboundpicker.BaseComponent;
import org.apache.sling.models.it.rtboundpicker.SubRTComponent;
import org.apache.sling.models.it.rtboundpicker.TestComponent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ResourceTypePickerIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "SM_Teleporter");

    private ResourceResolverFactory rrFactory;

    private final String baseComponentPath = "/content/rtpicker/baseComponent";
    private final String childComponentPath = "/content/rtpicker/childComponent";

    @Before
    public void setup() throws LoginException, PersistenceException {
        rrFactory = teleporter.getService(ResourceResolverFactory.class);
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrFactory.getAdministrativeResourceResolver(null);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/rtpicker/base");
            ResourceUtil.getOrCreateResource(adminResolver, baseComponentPath, properties, null, false);
            properties.clear();

            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/rtpicker/sub");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE,
                    "sling/rtpicker/base");
            ResourceUtil.getOrCreateResource(adminResolver, childComponentPath, properties, null, false);
            properties.clear();

            adminResolver.commit();
        } finally {
            if (adminResolver != null && adminResolver.isLive()) {
                adminResolver.close();
            }
        }
    }

    @Test
    public void testClientModelCreateFromResource() throws LoginException {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            final Resource baseComponentResource = resolver.getResource(baseComponentPath);
            Assert.assertNotNull(baseComponentResource);
            TestComponent baseModel = baseComponentResource.adaptTo(TestComponent.class);
            Assert.assertNotNull("Model should not be null", baseModel);
            Assert.assertTrue("Model should be a BaseComponent", baseModel instanceof BaseComponent);

            final Resource childComponentResource = resolver.getResource(childComponentPath);
            Assert.assertNotNull(childComponentResource);
            baseModel = childComponentResource.adaptTo(TestComponent.class);
            Assert.assertNotNull("Model should not be null", baseModel);
            Assert.assertTrue("Model should be a SubRTComponent", baseModel instanceof SubRTComponent);
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }
}
