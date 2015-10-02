/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.sling;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class MockSlingScriptHelperTest {

    private ResourceResolver resourceResolver;
    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;
    private BundleContext bundleContext;
    private SlingScriptHelper scriptHelper;

    @Before
    public void setUp() throws Exception {
        this.bundleContext = MockOsgi.newBundleContext();
        this.resourceResolver = MockSling.newResourceResolver(bundleContext);
        this.request = new MockSlingHttpServletRequest(this.resourceResolver);
        this.response = new MockSlingHttpServletResponse();
        this.scriptHelper = MockSling.newSlingScriptHelper(this.request, this.response, this.bundleContext);
    }
    
    @After
    public void tearDown() {
        this.resourceResolver.close();
        MockOsgi.shutdown(this.bundleContext);
    }

    @Test
    public void testRequest() {
        assertSame(this.request, this.scriptHelper.getRequest());
    }

    @Test
    public void testResponse() {
        assertSame(this.response, this.scriptHelper.getResponse());
    }

    @Test
    public void testGetService() {
        this.bundleContext.registerService(String.class.getName(), "test", null);
        assertEquals("test", this.scriptHelper.getService(String.class));
    }

    @Test
    public void testGetServices() {
        Integer[] services = new Integer[] { 1, 2, 3 };
        for (Integer service : services) {
            this.bundleContext.registerService(Integer.class.getName(), service, null);
        }
        Integer[] servicesResult = this.scriptHelper.getServices(Integer.class, null);
        assertArrayEquals(services, servicesResult);
    }

}
