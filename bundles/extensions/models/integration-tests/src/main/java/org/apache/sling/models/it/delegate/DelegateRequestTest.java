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
package org.apache.sling.models.it.delegate;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.it.delegate.request.DelegateBaseModel;
import org.apache.sling.models.it.delegate.request.DelegateExtendedModel;
import org.apache.sling.models.it.delegate.request.DelegateInterface;
import org.apache.sling.models.it.rtbound.FakeRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SlingAnnotationsTestRunner.class)
public class DelegateRequestTest {

    @TestReference
    private ResourceResolverFactory rrFactory;

    @TestReference
    private ModelFactory modelFactory;

    private final String baseComponentPath = "/content/delegate/baseComponent";
    private final String extendedComponentPath = "/content/delegate/extendedComponent";

    @Before
    public void setup() throws LoginException, PersistenceException {
        ResourceResolver adminResolver = null;
        try {
            adminResolver = rrFactory.getAdministrativeResourceResolver(null);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("text", "baseTESTValue");
            properties.put("other", "baseOther");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/delegate/base");
            ResourceUtil.getOrCreateResource(adminResolver, baseComponentPath, properties, null, false);
            properties.clear();

            properties.put("text", "extendedTESTValue");
            properties.put("other", "extendedOther");
            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE,
                    "sling/delegate/extended");
            ResourceUtil.getOrCreateResource(adminResolver, extendedComponentPath, properties, null, false);
            properties.clear();

            properties.put(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE,
                    "sling/delegate/base");
            ResourceUtil.getOrCreateResource(adminResolver, "/apps/sling/delegate/extended", properties, null, false);
            properties.clear();

            adminResolver.commit();
        } finally {
            if (adminResolver != null && adminResolver.isLive()) {
                adminResolver.close();
            }
        }
    }

    @Test
    public void testCreateDelegateModel() throws LoginException {
        ResourceResolver resolver = null;
        try {
            resolver = rrFactory.getAdministrativeResourceResolver(null);
            final Resource baseComponentResource = resolver.getResource(baseComponentPath);
            assertNotNull(baseComponentResource);
            final FakeRequest baseRequest = new FakeRequest(baseComponentResource);
            final DelegateInterface modelFromBase = modelFactory.createModel(baseRequest, DelegateInterface.class);
            assertNotNull("Base Model should not be null", modelFromBase);
            assertTrue("Model should be DelegateBaseModel", modelFromBase instanceof DelegateBaseModel);
            assertEquals("baseTESTValue", modelFromBase.getText());
            assertEquals("baseOther", modelFromBase.getOther());

            final Resource extendedComponentResource = resolver.getResource(extendedComponentPath);
            assertNotNull(extendedComponentResource);
            final FakeRequest extendedRequest = new FakeRequest(extendedComponentResource);
            final DelegateInterface modelFromExtended = modelFactory.createModel(extendedRequest, DelegateInterface.class);
            assertNotNull("Extended Model should not be null", modelFromExtended);
            assertTrue("Model should be DelegateExtendedModel", modelFromExtended instanceof DelegateExtendedModel);
            assertEquals("EXTENDEDTESTVALUE", modelFromExtended.getText());
            assertEquals("extendedOther", modelFromExtended.getOther());
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }
}
