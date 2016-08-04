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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.Service3;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceInterface1;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceInterface1Optional;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceInterface2;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceInterface3;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest.ServiceSuperInterface3;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.google.common.collect.ImmutableSet;

@RunWith(MockitoJUnitRunner.class)
public class MockBundleContextDynamicReferncesTest {

    private BundleContext bundleContext;
    private Service3 service;
    private ServiceRegistration reg1a;
    private ServiceRegistration reg2a;
    
    @Mock
    private ServiceInterface1 dependency1a;
    @Mock
    private ServiceInterface1 dependency1b;
    @Mock
    private ServiceInterface1Optional dependency1aOptional;
    @Mock
    private ServiceInterface1Optional dependency1bOptional;
    @Mock
    private ServiceInterface2 dependency2a;
    @Mock
    private ServiceInterface2 dependency2b;
    @Mock
    private ServiceSuperInterface3 dependency3a;
    @Mock
    private ServiceSuperInterface3 dependency3b;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
        
        // setup service instance with only minimum mandatory references
        reg1a = bundleContext.registerService(ServiceInterface1.class.getName(), dependency1a, null);
        reg2a = bundleContext.registerService(ServiceInterface2.class.getName(), dependency2a, null);
        
        service = new Service3();
        MockOsgi.injectServices(service, bundleContext);
        MockOsgi.activate(service, bundleContext);
        bundleContext.registerService(Service3.class.getName(), service, null);
        
        assertDependency1(dependency1a);
        assertDependency1Optional(null);
        assertDependencies2(dependency2a);
        assertDependencies3();
    }

    @Test
    public void testAddRemoveOptionalUnaryService() {
        ServiceRegistration reg1aOptional = bundleContext.registerService(ServiceInterface1Optional.class.getName(), dependency1aOptional, null);
        assertDependency1Optional(dependency1aOptional);
        
        reg1aOptional.unregister();
        assertDependency1Optional(null);
    }
    
    public void testAddOptionalUnaryService_TooMany() {
        bundleContext.registerService(ServiceInterface1Optional.class.getName(), dependency1aOptional, null);
        assertDependency1Optional(dependency1aOptional);
        
        // in real OSGi this should fail - but this is not covered by the current implementation. so test the real implementation here.
        bundleContext.registerService(ServiceInterface1Optional.class.getName(), dependency1bOptional, null);
        assertDependency1Optional(dependency1bOptional);
    }
    
    @Test(expected = ReferenceViolationException.class)
    public void testAddMandatoryUnaryService_TooMany() {
        bundleContext.registerService(ServiceInterface1.class.getName(), dependency1b, null);
    }
    
    @Test(expected = ReferenceViolationException.class)
    public void testRemoveMandatoryUnaryService_TooMany() {
        reg1a.unregister();
    }
    
    @Test
    public void testAddRemoveOptionalMultipleService() {
        ServiceRegistration reg3a = bundleContext.registerService(ServiceInterface3.class.getName(), dependency3a, null);
        assertDependencies3(dependency3a);

        ServiceRegistration reg3b = bundleContext.registerService(ServiceInterface3.class.getName(), dependency3b, null);
        assertDependencies3(dependency3a, dependency3b);

        reg3a.unregister();
        assertDependencies3(dependency3b);
        
        reg3b.unregister();
        assertDependencies3();
    }
    
    @Test
    public void testAddRemoveMandatoryMultipleService() {
        ServiceRegistration reg2b = bundleContext.registerService(ServiceInterface2.class.getName(), dependency2b, null);
        assertDependencies2(dependency2a, dependency2b);

        reg2b.unregister();
        assertDependencies2(dependency2a);
        
        // in real OSGi this should fail - but this is not covered by the current implementation. so test the real implementation here.
        reg2a.unregister();
        assertDependencies2();
    }
    
    private void assertDependency1(ServiceInterface1 instance) {
        if (instance == null) {
            assertNull(service.getReference1());
        }
        else {
            assertSame(instance, service.getReference1());
        }
    }
    
    private void assertDependency1Optional(ServiceInterface1Optional instance) {
        if (instance == null) {
            assertNull(service.getReference1Optional());
        }
        else {
            assertSame(instance, service.getReference1Optional());
        }
    }
    
    private void assertDependencies2(ServiceInterface2... instances) {
        assertEquals(ImmutableSet.<ServiceInterface2>copyOf(instances), 
                ImmutableSet.<ServiceInterface2>copyOf(service.getReferences2()));
    }
    
    private void assertDependencies3(ServiceSuperInterface3... instances) {
        assertEquals(ImmutableSet.<ServiceSuperInterface3>copyOf(instances), 
                ImmutableSet.<ServiceSuperInterface3>copyOf(service.getReferences3()));
    }
    
}
