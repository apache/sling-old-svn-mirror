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
package org.apache.sling.testing.mock.sling.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpSession;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.servlethelpers.MockHttpSession;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

@RunWith(MockitoJUnitRunner.class)
public class MockSlingHttpServletRequestTest {

    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private Resource resource;
    private BundleContext bundleContext = MockOsgi.newBundleContext();

    private MockSlingHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(resourceResolver, bundleContext);
    }
    
    @After
    public void tearDown() {
        MockOsgi.shutdown(bundleContext);
    }

    @Test
    public void testResourceResolver() {
        assertSame(resourceResolver, request.getResourceResolver());
    }

    @Test
    public void testDefaultResourceResolver() {
        assertNotNull(new MockSlingHttpServletRequest(bundleContext).getResourceResolver());
    }

    @Test
    public void testSession() {
        HttpSession session = request.getSession();
        assertNotNull(session);
        assertTrue(session instanceof MockHttpSession);
    }

    @Test
    public void testRequestPathInfo() {
        assertNotNull(request.getRequestPathInfo());
        assertTrue(request.getRequestPathInfo() instanceof MockRequestPathInfo);
    }

    @Test
    public void testDefaultResourceBundle() {
        ResourceBundle bundle = request.getResourceBundle(Locale.US);
        assertNotNull(bundle);
        assertFalse(bundle.getKeys().hasMoreElements());
    }

    @Test
    public void testResourceBundleFromProvider() {
        ResourceBundleProvider provider = mock(ResourceBundleProvider.class);
        bundleContext.registerService(ResourceBundleProvider.class.getName(), provider, null);
        when(provider.getResourceBundle("base1", Locale.US)).thenReturn(new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][] {
                        { "key1", "value1" }
                };
            }
        });        
        
        ResourceBundle bundle = request.getResourceBundle("base1", Locale.US);
        assertNotNull(bundle);
        assertEquals("value1", bundle.getString("key1"));

        ResourceBundle bundle2 = request.getResourceBundle("base2", Locale.US);
        assertNotNull(bundle2);
        assertFalse(bundle2.getKeys().hasMoreElements());
    }

}
