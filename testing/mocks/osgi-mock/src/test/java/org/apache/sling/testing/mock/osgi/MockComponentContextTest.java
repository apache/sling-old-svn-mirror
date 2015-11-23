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
package org.apache.sling.testing.mock.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class MockComponentContextTest {

    private ComponentContext underTest;

    @Before
    public void setUp() {
        underTest = MockOsgi.newComponentContext();
    }

    @Test
    public void testBundleContext() {
        assertNotNull(underTest.getBundleContext());
    }

    @Test
    public void testInitialProperties() {
        assertEquals(0, underTest.getProperties().size());
    }

    @Test
    public void testProvidedProperties() {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("prop1", "value1");
        props.put("prop2", 25);
        ComponentContext componentContextWithProperties = MockOsgi.newComponentContext(props);

        Dictionary contextProps = componentContextWithProperties.getProperties();
        assertEquals(2, contextProps.size());
        assertEquals("value1", contextProps.get("prop1"));
        assertEquals(25, contextProps.get("prop2"));
    }

    @Test
    public void testLocateService() {
        // prepare test service
        String clazz = String.class.getName();
        Object service = new Object();
        underTest.getBundleContext().registerService(clazz, service, null);
        ServiceReference<?> ref = underTest.getBundleContext().getServiceReference(clazz);

        // test locate service
        Object locatedService = underTest.locateService(null, ref);
        assertSame(service, locatedService);
    }

    @Test
    public void testIgnoredMethods() {
        underTest.enableComponent("myComponent");
        underTest.disableComponent("myComponent");
    }

    @Test
    public void testGetUsingBundle() {
        // test context without using bundle
        assertNull(underTest.getUsingBundle());
        
        // test context with using bundle
        Bundle usingBundle = mock(Bundle.class);
        ComponentContext contextWithUsingBundle = MockOsgi.componentContext().usingBundle(usingBundle).build();
        assertSame(usingBundle, contextWithUsingBundle.getUsingBundle());
    }

}
