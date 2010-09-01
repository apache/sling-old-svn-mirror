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
package org.apache.sling.adapter.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.sling.adapter.mock.MockAdapterFactory;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

@RunWith(JMock.class)
public class AdapterManagerTest {

    private AdapterManagerImpl am;

    protected final Mockery context = new JUnit4Mockery();

    @org.junit.Before public void setUp() {
        am = new AdapterManagerImpl();
    }

    @org.junit.After public void tearDown() {
        am.deactivate(null); // not correct, but argument unused
    }

    /**
     * Helper method to create a mock bundle
     */
    protected Bundle createBundle(String name) {
        final Bundle bundle = this.context.mock(Bundle.class, name);
        this.context.checking(new Expectations() {{
            allowing(bundle).getBundleId();
            will(returnValue(1L));
        }});
        return bundle;
    }

    /**
     * Helper method to create a mock component context
     */
    protected ComponentContext createComponentContext() throws Exception {
        final BundleContext bundleCtx = this.context.mock(BundleContext.class);
        final Filter filter = this.context.mock(Filter.class);
        final ComponentContext ctx = this.context.mock(ComponentContext.class);
        this.context.checking(new Expectations() {{
            allowing(ctx).locateService(with(any(String.class)), with(any(ServiceReference.class)));
            will(returnValue(new MockAdapterFactory()));
            allowing(ctx).getBundleContext();
            will(returnValue(bundleCtx));
            allowing(bundleCtx).createFilter(with(any(String.class)));
            will(returnValue(filter));
            allowing(bundleCtx).addServiceListener(with(any(ServiceListener.class)), with(any(String.class)));
            allowing(bundleCtx).getServiceReferences(with(any(String.class)), with(any(String.class)));
            will(returnValue(null));
            allowing(bundleCtx).removeServiceListener(with(any(ServiceListener.class)));
        }});
        return ctx;
    }

    /**
     * Helper method to create a mock service reference
     */
    protected ServiceReference createServiceReference() {
        final Bundle bundle = this.createBundle("bundle");
        final ServiceReference ref = this.context.mock(ServiceReference.class, "serviceReference");
        this.context.checking(new Expectations() {{
            allowing(ref).getProperty(Constants.SERVICE_ID);
            will(returnValue(1L));
            allowing(ref).getProperty(AdapterFactory.ADAPTABLE_CLASSES);
            will(returnValue(new String[]{ TestSlingAdaptable.class.getName() }));
            allowing(ref).getProperty(AdapterFactory.ADAPTER_CLASSES);
            will(returnValue(ITestAdapter.class.getName()));
            allowing(ref).getBundle();
            will(returnValue(bundle));
        }});
        return ref;
    }

    /**
     * Helper method to create a mock service reference
     */
    protected ServiceReference createServiceReference2() {
        final Bundle bundle = this.createBundle("bundle2");
        final ServiceReference ref = this.context.mock(ServiceReference.class, "serviceReference2");
        this.context.checking(new Expectations() {{
            allowing(ref).getProperty(Constants.SERVICE_ID);
            will(returnValue(2L));
            allowing(ref).getProperty(AdapterFactory.ADAPTABLE_CLASSES);
            will(returnValue(new String[]{ TestSlingAdaptable2.class.getName() }));
            allowing(ref).getProperty(AdapterFactory.ADAPTER_CLASSES);
            will(returnValue(TestAdapter.class.getName()));
            allowing(ref).getBundle();
            will(returnValue(bundle));
        }});
        return ref;
    }

    @org.junit.Test public void testUnitialized() {
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());
    }

    @org.junit.Test public void testInitialized() throws Exception {
        am.activate(this.createComponentContext());

        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());
    }

    @org.junit.Test public void testBindBeforeActivate() throws Exception {
        final ServiceReference ref = createServiceReference();
        am.bindAdapterFactory(ref);

        // no cache and no factories yet
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());

        am.activate(this.createComponentContext());

        // expect the factory, but cache is empty
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertEquals("AdapterFactoryDescriptors must contain one entry", 1, am.getFactories().size());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());
    }

    @org.junit.Test public void testBindAfterActivate() throws Exception {
        am.activate(this.createComponentContext());

        // no cache and no factories yet
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());

        final ServiceReference ref = createServiceReference();
        am.bindAdapterFactory(ref);

        // expect the factory, but cache is empty
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertEquals("AdapterFactoryDescriptors must contain one entry", 1, am.getFactories().size());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());

        Map<String, AdapterFactoryDescriptorMap> f = am.getFactories();
        AdapterFactoryDescriptorMap afdm = f.get(TestSlingAdaptable.class.getName());
        assertNotNull(afdm);

        AdapterFactoryDescriptor afd = afdm.get(new AdapterFactoryDescriptorKey(ref));
        assertNotNull(afd);
        assertNotNull(afd.getFactory());
        assertNotNull(afd.getAdapters());
        assertEquals(1, afd.getAdapters().length);
        assertEquals(ITestAdapter.class.getName(), afd.getAdapters()[0]);

        assertNull(f.get(TestSlingAdaptable2.class.getName()));
    }

    @org.junit.Test public void testAdaptBase() throws Exception {
        am.activate(this.createComponentContext());

        TestSlingAdaptable data = new TestSlingAdaptable();
        assertNull("Expect no adapter", am.getAdapter(data, ITestAdapter.class));

        final ServiceReference ref = createServiceReference();
        am.bindAdapterFactory(ref);

        Object adapter = am.getAdapter(data, ITestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof ITestAdapter);
    }

    @org.junit.Test public void testAdaptExtended() throws Exception {
        am.activate(this.createComponentContext());

        TestSlingAdaptable2 data = new TestSlingAdaptable2();
        assertNull("Expect no adapter", am.getAdapter(data, ITestAdapter.class));

        final ServiceReference ref = createServiceReference();
        am.bindAdapterFactory(ref);

        Object adapter = am.getAdapter(data, ITestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof ITestAdapter);
    }

    @org.junit.Test public void testAdaptBase2() throws Exception {
        am.activate(this.createComponentContext());

        TestSlingAdaptable data = new TestSlingAdaptable();
        assertNull("Expect no adapter", am.getAdapter(data, ITestAdapter.class));

        final ServiceReference ref = createServiceReference();
        am.bindAdapterFactory(ref);

        final ServiceReference ref2 = createServiceReference2();
        am.bindAdapterFactory(ref2);

        Object adapter = am.getAdapter(data, ITestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof ITestAdapter);
    }

    @org.junit.Test public void testAdaptExtended2() throws Exception {
        am.activate(this.createComponentContext());

        final ServiceReference ref = createServiceReference();
        am.bindAdapterFactory(ref);

        final ServiceReference ref2 = createServiceReference2();
        am.bindAdapterFactory(ref2);

        TestSlingAdaptable data = new TestSlingAdaptable();
        Object adapter = am.getAdapter(data, ITestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof ITestAdapter);
        adapter = am.getAdapter(data, TestAdapter.class);
        assertNull(adapter);

        TestSlingAdaptable2 data2 = new TestSlingAdaptable2();
        adapter = am.getAdapter(data2, ITestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof ITestAdapter);
        adapter = am.getAdapter(data2, TestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof TestAdapter);
    }

    //---------- Test Adaptable and Adapter Classes ---------------------------

    public static class TestSlingAdaptable extends SlingAdaptable {

    }

    public static class TestSlingAdaptable2 extends TestSlingAdaptable {

    }

    public static interface ITestAdapter {

    }

    public static class TestAdapter {

    }

}
