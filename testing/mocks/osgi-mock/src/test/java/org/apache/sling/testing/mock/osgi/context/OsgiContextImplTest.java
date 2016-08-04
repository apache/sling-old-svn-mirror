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
package org.apache.sling.testing.mock.osgi.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.testing.mock.osgi.NoScrMetadataException;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceInterface1;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceInterface2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class OsgiContextImplTest {

    private OsgiContextImpl context;

    @Before
    public void setUp() throws Exception {
        this.context = new OsgiContextImpl();
        this.context.setUp();
    }

    @After
    public void tearDown() throws Exception {
        this.context.tearDown();
    }
    
    @Test
    public void testContextObjects() {
        assertNotNull(context.componentContext());
        assertNotNull(context.bundleContext());
    }

    @Test
    public void testRegisterService() {
        Set<String> myService = new HashSet<String>();
        context.registerService(Set.class, myService);

        Set<?> serviceResult = context.getService(Set.class);
        assertSame(myService, serviceResult);
    }

    @Test
    public void testRegisterServiceWithProperties() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");

        Set<String> myService = new HashSet<String>();
        context.registerService(Set.class, myService, props);

        ServiceReference<?> serviceReference = context.bundleContext().getServiceReference(Set.class.getName());
        Object serviceResult = context.bundleContext().getService(serviceReference);
        assertSame(myService, serviceResult);
        assertEquals("value1", serviceReference.getProperty("prop1"));
    }

    @Test
    public void testRegisterMultipleServices() {
        Set<String> myService1 = new HashSet<String>();
        context.registerService(Set.class, myService1);
        Set<String> myService2 = new HashSet<String>();
        context.registerService(Set.class, myService2);
        
        assertSame(myService1, context.getService(Set.class));

        // expected: ascending order because ordering ascending by service ID
        Set[] serviceResults = context.getServices(Set.class, null);
        assertSame(myService1, serviceResults[0]);
        assertSame(myService2, serviceResults[1]);
    }

    @Test
    public void testRegisterInjectActivate() {
        context.registerService(ServiceInterface1.class, mock(ServiceInterface1.class));
        context.registerService(ServiceInterface2.class, mock(ServiceInterface2.class));
        context.registerInjectActivateService(new OsgiServiceUtilTest.Service3());
    }

    @Test(expected=RuntimeException.class)
    public void testRegisterInjectActivate_RefrenceMissing() {
        context.registerInjectActivateService(new OsgiServiceUtilTest.Service3());
    }

    @Test(expected=NoScrMetadataException.class)
    public void testRegisterInjectActivateInvalid() {
        context.registerInjectActivateService(new Object());
    }

}
