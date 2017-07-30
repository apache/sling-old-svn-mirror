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
package org.apache.sling.models.testing.rtbound;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.it.rtbound.BaseComponent;
import org.apache.sling.models.it.rtbound.ExtendedComponent;
import org.apache.sling.models.it.rtbound.FromRequestComponent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourceTypeBindingIT {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "SM_Teleporter");

    private ResourceResolverFactory rrFactory;

    private ModelFactory modelFactory;

    private final String baseComponentPath = "/content/rt/baseComponent";
    private final String childComponentPath = "/content/rt/childComponent";
    private final String child2ComponentPath = "/content/rt/child2Component";
    private final String extendedComponentPath = "/content/rt/extendedComponent";
    private final String fromRequestComponentPath = "/content/rt/fromRequest";

    @Before
    public void setup() throws LoginException, PersistenceException {
        rrFactory = teleporter.getService(ResourceResolverFactory.class);
        modelFactory = teleporter.getService(ModelFactory.class);

        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrFactory.getAdministrativeResourceResolver(null);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("sampleValue", "baseTESTValue");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/rt/base");
            ResourceUtil.getOrCreateResource(adminResolver, baseComponentPath, properties, null, false);
            properties.clear();

            properties.put("sampleValue", "childTESTValue");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/rt/child");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE,
                    "sling/rt/base");
            ResourceUtil.getOrCreateResource(adminResolver, childComponentPath, properties, null, false);
            properties.clear();

            properties.put("sampleValue", "childTESTValue2");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/rt/child");
            ResourceUtil.getOrCreateResource(adminResolver, child2ComponentPath, properties, null, false);
            properties.clear();

            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE,
                    "sling/rt/base");
            ResourceUtil.getOrCreateResource(adminResolver, "/apps/sling/rt/child", properties, null, false);
            properties.clear();

            properties.put("sampleValue", "extendedTESTValue");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/rt/extended");
            ResourceUtil.getOrCreateResource(adminResolver, extendedComponentPath, properties, null, false);

            properties.clear();
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/rt/fromRequest");
            ResourceUtil.getOrCreateResource(adminResolver, fromRequestComponentPath, properties, null, false);

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
            final Object baseModel = modelFactory.getModelFromResource(baseComponentResource);
            Assert.assertNotNull("Model should not be null", baseModel);
            Assert.assertTrue("Model should be a BaseComponent", baseModel instanceof BaseComponent);

            final Resource childComponentResource = resolver.getResource(childComponentPath);
            Assert.assertNotNull(childComponentResource);
            final Object childModel = modelFactory.getModelFromResource(childComponentResource);
            Assert.assertNotNull("Model should not be null", childModel);
            Assert.assertTrue("Model should be a BaseComponent", childModel instanceof BaseComponent);

            final Resource child2ComponentResource = resolver.getResource(child2ComponentPath);
            Assert.assertNotNull(child2ComponentResource);
            final Object child2Model = modelFactory.getModelFromResource(child2ComponentResource);
            Assert.assertNotNull("Model should not be null", child2Model);
            Assert.assertTrue("Model should be a BaseComponent", child2Model instanceof BaseComponent);

            final Resource extendedComponentResource = resolver.getResource(extendedComponentPath);
            Assert.assertNotNull(extendedComponentResource);
            final Object extendedModel = modelFactory.getModelFromResource(extendedComponentResource);
            Assert.assertNotNull("Model should not be null", extendedModel);
            Assert.assertTrue("Model should be a BaseComponent", extendedModel instanceof BaseComponent);
            Assert.assertTrue("Model should be an ExtendedComponent", extendedModel instanceof ExtendedComponent);
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    @Test
    public void testClientModelCreateFromRequest() throws LoginException {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            final Resource baseComponentResource = resolver.getResource(fromRequestComponentPath);
            Assert.assertNotNull(baseComponentResource);
            final Object baseModel = modelFactory.getModelFromRequest(new FakeRequest(baseComponentResource));
            Assert.assertNotNull("Model should not be null", baseModel);
            Assert.assertTrue("Model should be a FromRequestComponent", baseModel instanceof FromRequestComponent);

        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

}
