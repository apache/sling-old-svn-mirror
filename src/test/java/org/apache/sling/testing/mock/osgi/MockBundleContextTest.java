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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
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
        this.bundleContext = MockOsgi.newBundleContext();
    }

    @Test
    public void testBundle() {
        assertNotNull(this.bundleContext.getBundle());
    }

    @Test
    public void testServiceRegistration() throws InvalidSyntaxException {
        // prepare test services
        String clazz1 = String.class.getName();
        Object service1 = new Object();
        Dictionary properties1 = getServiceProperties(null);
        ServiceRegistration reg1 = this.bundleContext.registerService(clazz1, service1, properties1);

        String[] clazzes2 = new String[] { String.class.getName(), Integer.class.getName() };
        Object service2 = new Object();
        Dictionary properties2 = getServiceProperties(null);
        ServiceRegistration reg2 = this.bundleContext.registerService(clazzes2, service2, properties2);

        String clazz3 = Integer.class.getName();
        Object service3 = new Object();
        Dictionary properties3 = getServiceProperties(100L);
        ServiceRegistration reg3 = this.bundleContext.registerService(clazz3, service3, properties3);

        // test get service references
        ServiceReference refString = this.bundleContext.getServiceReference(String.class.getName());
        assertSame(reg1.getReference(), refString);

        ServiceReference refInteger = this.bundleContext.getServiceReference(Integer.class.getName());
        assertSame(reg3.getReference(), refInteger);

        ServiceReference[] refsString = this.bundleContext.getServiceReferences(String.class.getName(), null);
        assertEquals(2, refsString.length);
        assertSame(reg1.getReference(), refsString[0]);
        assertSame(reg2.getReference(), refsString[1]);

        ServiceReference[] refsInteger = this.bundleContext.getServiceReferences(Integer.class.getName(), null);
        assertEquals(2, refsInteger.length);
        assertSame(reg3.getReference(), refsInteger[0]);
        assertSame(reg2.getReference(), refsInteger[1]);

        ServiceReference[] allRefsString = this.bundleContext.getAllServiceReferences(String.class.getName(), null);
        assertArrayEquals(refsString, allRefsString);

        // test get services
        assertSame(service1, this.bundleContext.getService(refsString[0]));
        assertSame(service2, this.bundleContext.getService(refsString[1]));
        assertSame(service3, this.bundleContext.getService(refInteger));

        // unget does nothing
        this.bundleContext.ungetService(refsString[0]);
        this.bundleContext.ungetService(refsString[1]);
        this.bundleContext.ungetService(refInteger);
    }

    private Dictionary getServiceProperties(final Long serviceRanking) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        if (serviceRanking != null) {
            props.put(Constants.SERVICE_RANKING, serviceRanking);
        }
        return props;
    }

    @Test
    public void testGetBundles() throws Exception {
        assertEquals(0, this.bundleContext.getBundles().length);
    }

    @Test
    public void testServiceListener() throws Exception {
        ServiceListener serviceListener = mock(ServiceListener.class);
        bundleContext.addServiceListener(serviceListener);

        // prepare test services
        String clazz1 = String.class.getName();
        Object service1 = new Object();
        this.bundleContext.registerService(clazz1, service1, null);

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
        this.bundleContext.addFrameworkListener(null);
        this.bundleContext.removeFrameworkListener(null);
    }

}
