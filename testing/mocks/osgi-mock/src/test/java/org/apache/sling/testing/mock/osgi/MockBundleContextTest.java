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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

@RunWith(MockitoJUnitRunner.class)
public class MockBundleContextTest {

    private BundleContext bundleContext;

    @Before
    public void setUp() {
        bundleContext = MockOsgi.newBundleContext();
    }

    @After
    public void tearDown() {
        MockOsgi.shutdown(bundleContext);
    }

    @Test
    public void testBundle() {
        assertNotNull(bundleContext.getBundle());
    }

    @Test
    public void testServiceRegistration() throws InvalidSyntaxException {
        // prepare test services
        String clazz1 = String.class.getName();
        Object service1 = new Object();
        Dictionary<String, Object> properties1 = getServiceProperties(null);
        ServiceRegistration reg1 = bundleContext.registerService(clazz1, service1, properties1);

        String[] clazzes2 = new String[] { String.class.getName(), Integer.class.getName() };
        Object service2 = new Object();
        Dictionary<String, Object> properties2 = getServiceProperties(null);
        ServiceRegistration reg2 = bundleContext.registerService(clazzes2, service2, properties2);

        String clazz3 = Integer.class.getName();
        Object service3 = new Object();
        Dictionary<String, Object> properties3 = getServiceProperties(100L);
        ServiceRegistration reg3 = bundleContext.registerService(clazz3, service3, properties3);

        // test get service references
        ServiceReference<?> refString = bundleContext.getServiceReference(String.class.getName());
        assertSame(reg1.getReference(), refString);

        ServiceReference<?> refInteger = bundleContext.getServiceReference(Integer.class.getName());
        assertSame(reg3.getReference(), refInteger);

        ServiceReference<?>[] refsString = bundleContext.getServiceReferences(String.class.getName(), null);
        assertEquals(2, refsString.length);
        assertSame(reg1.getReference(), refsString[0]);
        assertSame(reg2.getReference(), refsString[1]);

        Collection<ServiceReference<String>> refColString = bundleContext.getServiceReferences(String.class, null);
        assertEquals(2, refColString.size());
        assertSame(reg1.getReference(), refColString.iterator().next());

        ServiceReference<?>[] refsInteger = bundleContext.getServiceReferences(Integer.class.getName(), null);
        assertEquals(2, refsInteger.length);
        assertSame(reg3.getReference(), refsInteger[0]);
        assertSame(reg2.getReference(), refsInteger[1]);

        ServiceReference<?>[] allRefsString = bundleContext.getAllServiceReferences(String.class.getName(), null);
        assertArrayEquals(refsString, allRefsString);

        // test get services
        assertSame(service1, bundleContext.getService(refsString[0]));
        assertSame(service2, bundleContext.getService(refsString[1]));
        assertSame(service3, bundleContext.getService(refInteger));

        // unget does nothing
        bundleContext.ungetService(refsString[0]);
        bundleContext.ungetService(refsString[1]);
        bundleContext.ungetService(refInteger);
    }
    
    @Test
    public void testNoServiceReferences() throws InvalidSyntaxException {
        ServiceReference<?>[] refs = bundleContext.getServiceReferences(String.class.getName(), null);
        assertNull(refs);

        Collection<ServiceReference<String>> refCol = bundleContext.getServiceReferences(String.class, null);
        assertNotNull(refCol);
        assertTrue(refCol.isEmpty());
    }
    
    @Test
    public void testServiceUnregistration() {
        // prepare test services
        String clazz1 = String.class.getName();
        Object service1 = new Object();
        Dictionary<String, Object> properties1 = getServiceProperties(null);
        ServiceRegistration reg1 = bundleContext.registerService(clazz1, service1, properties1);
        
        assertNotNull(bundleContext.getServiceReference(clazz1));

        reg1.unregister();

        assertNull(bundleContext.getServiceReference(clazz1));
    }
    

    private Dictionary<String, Object> getServiceProperties(final Long serviceRanking) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        if (serviceRanking != null) {
            props.put(Constants.SERVICE_RANKING, serviceRanking);
        }
        return props;
    }

    @Test
    public void testGetBundles() throws Exception {
        assertEquals(0, bundleContext.getBundles().length);
    }

    @Test
    public void testServiceListener() throws Exception {
        ServiceListener serviceListener = mock(ServiceListener.class);
        bundleContext.addServiceListener(serviceListener);

        // prepare test services
        String clazz1 = String.class.getName();
        Object service1 = new Object();
        bundleContext.registerService(clazz1, service1, null);

        verify(serviceListener).serviceChanged(any(ServiceEvent.class));

        bundleContext.removeServiceListener(serviceListener);
    }

    @Test
    public void testBundleListener() throws Exception {
        BundleListener bundleListener = mock(BundleListener.class);
        BundleEvent bundleEvent = mock(BundleEvent.class);

        bundleContext.addBundleListener(bundleListener);

        MockOsgi.sendBundleEvent(bundleContext, bundleEvent);
        verify(bundleListener).bundleChanged(bundleEvent);

        bundleContext.removeBundleListener(bundleListener);
    }

    @Test
    public void testFrameworkListener() throws Exception {
        // ensure that listeners can be called (although they are not expected
        // to to anything)
        bundleContext.addFrameworkListener(null);
        bundleContext.removeFrameworkListener(null);
    }

    @Test
    public void testGetProperty() {
        assertNull(bundleContext.getProperty("anyProperty"));
    }
    
    @Test
    public void testObjectClassFilterMatches() throws InvalidSyntaxException {
        
        Filter filter = bundleContext.createFilter("(" + Constants.OBJECTCLASS + "=" + Integer.class.getName() + ")");
        
        ServiceRegistration serviceRegistration = bundleContext.registerService(Integer.class.getName(), Integer.valueOf(1), null);
        
        assertTrue(filter.match(serviceRegistration.getReference()));
    }

    @Test
    public void testObjectClassFilterDoesNotMatch() throws InvalidSyntaxException {
        
        Filter filter = bundleContext.createFilter("(" + Constants.OBJECTCLASS + "=" + Integer.class.getName() + ")");
        
        ServiceRegistration serviceRegistration = bundleContext.registerService(Long.class.getName(), Long.valueOf(1), null);
        
        assertFalse(filter.match(serviceRegistration.getReference()));
    }

    @Test
    public void testGetDataFile() {
        File rootFile = bundleContext.getDataFile("");
        assertNotNull(rootFile);
        
        File childFile = bundleContext.getDataFile("child");
        assertNotNull(childFile);
        
        assertEquals(childFile.getParentFile(), rootFile);
    }
}
