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

import java.util.Map;

import junit.framework.TestCase;

import org.apache.sling.adapter.SlingAdaptable;
import org.apache.sling.adapter.mock.MockBundle;
import org.apache.sling.adapter.mock.MockComponentContext;
import org.apache.sling.adapter.mock.MockServiceReference;
import org.apache.sling.api.adapter.AdapterFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

public class AdapterManagerTest extends TestCase {

    private AdapterManagerImpl am;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        am = new AdapterManagerImpl();
    }
    
    @Override
    protected void tearDown() throws Exception {
        if (AdapterManagerImpl.getInstance() == am) {
            am.deactivate(null); // not correct, but argument unused
        }
        
        super.tearDown();
    }
    
    public void testUnitialized() {
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());
    }

    public void testInitialized() {
        ComponentContext cc = new MockComponentContext();
        am.activate(cc);

        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());
    }

    public void testBindBeforeActivate() {
        Bundle bundle = new MockBundle(1L);
        MockServiceReference ref = new MockServiceReference(bundle);
        ref.setProperty(Constants.SERVICE_ID, 1L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, new String[]{ TestSlingAdaptable.class.getName() });
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, ITestAdapter.class.getName());
        am.bindAdapterFactory(ref);

        // no cache and no factories yet
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());

        // this should register the factory
        ComponentContext cc = new MockComponentContext();
        am.activate(cc);

        // expect the factory, but cache is empty
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertEquals("AdapterFactoryDescriptors must contain one entry", 1, am.getFactories().size());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());
    }

    public void testBindAfterActivate() {
        ComponentContext cc = new MockComponentContext();
        am.activate(cc);

        // no cache and no factories yet
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertNull("AdapterFactory cache must be null", am.getFactoryCache());

        Bundle bundle = new MockBundle(1L);
        MockServiceReference ref = new MockServiceReference(bundle);
        ref.setProperty(Constants.SERVICE_ID, 1L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, new String[]{ TestSlingAdaptable.class.getName() });
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, ITestAdapter.class.getName());
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

    public void testAdaptBase() {

        ComponentContext cc = new MockComponentContext();
        am.activate(cc);

        TestSlingAdaptable data = new TestSlingAdaptable();
        assertNull("Expect no adapter", am.getAdapter(data, ITestAdapter.class));

        Bundle bundle = new MockBundle(1L);
        MockServiceReference ref = new MockServiceReference(bundle);
        ref.setProperty(Constants.SERVICE_ID, 1L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, new String[]{ TestSlingAdaptable.class.getName() });
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, ITestAdapter.class.getName());
        am.bindAdapterFactory(ref);

        Object adapter = am.getAdapter(data, ITestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof ITestAdapter);
    }

    public void testAdaptExtended() {

        ComponentContext cc = new MockComponentContext();
        am.activate(cc);

        TestSlingAdaptable2 data = new TestSlingAdaptable2();
        assertNull("Expect no adapter", am.getAdapter(data, ITestAdapter.class));

        Bundle bundle = new MockBundle(1L);
        MockServiceReference ref = new MockServiceReference(bundle);
        ref.setProperty(Constants.SERVICE_ID, 1L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, new String[]{ TestSlingAdaptable.class.getName() });
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, ITestAdapter.class.getName());
        am.bindAdapterFactory(ref);

        Object adapter = am.getAdapter(data, ITestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof ITestAdapter);
    }

    public void testAdaptBase2() {

        ComponentContext cc = new MockComponentContext();
        am.activate(cc);

        TestSlingAdaptable data = new TestSlingAdaptable();
        assertNull("Expect no adapter", am.getAdapter(data, ITestAdapter.class));

        Bundle bundle = new MockBundle(1L);
        MockServiceReference ref = new MockServiceReference(bundle);
        ref.setProperty(Constants.SERVICE_ID, 1L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, new String[]{ TestSlingAdaptable.class.getName() });
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, ITestAdapter.class.getName());
        am.bindAdapterFactory(ref);

        ref = new MockServiceReference(bundle);
        ref.setProperty(Constants.SERVICE_ID, 2L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, new String[]{ TestSlingAdaptable2.class.getName() });
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, TestAdapter.class.getName());
        am.bindAdapterFactory(ref);

        Object adapter = am.getAdapter(data, ITestAdapter.class);
        assertNotNull(adapter);
        assertTrue(adapter instanceof ITestAdapter);
    }

    public void testAdaptExtended2() {

        ComponentContext cc = new MockComponentContext();
        am.activate(cc);

        Bundle bundle = new MockBundle(1L);
        MockServiceReference ref = new MockServiceReference(bundle);
        ref.setProperty(Constants.SERVICE_ID, 1L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, new String[]{ TestSlingAdaptable.class.getName() });
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, ITestAdapter.class.getName());
        am.bindAdapterFactory(ref);

        ref = new MockServiceReference(bundle);
        ref.setProperty(Constants.SERVICE_ID, 2L);
        ref.setProperty(AdapterFactory.ADAPTABLE_CLASSES, new String[]{ TestSlingAdaptable2.class.getName() });
        ref.setProperty(AdapterFactory.ADAPTER_CLASSES, TestAdapter.class.getName());
        am.bindAdapterFactory(ref);

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
