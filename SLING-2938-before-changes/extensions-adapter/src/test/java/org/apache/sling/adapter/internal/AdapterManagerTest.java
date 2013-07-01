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

import org.apache.sling.adapter.mock.MockAdapterFactory;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;
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

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
     * Helper method to create a mock component context
     */
    protected ComponentContext createMultipleAdaptersComponentContext(final ServiceReference firstServiceReference, final ServiceReference secondServiceReference) throws Exception {
        final BundleContext bundleCtx = this.context.mock(BundleContext.class);
        final Filter filter = this.context.mock(Filter.class);
        final ComponentContext ctx = this.context.mock(ComponentContext.class);
        this.context.checking(new Expectations() {{
            allowing(ctx).locateService(with(any(String.class)), with(firstServiceReference));
            will(returnValue(new FirstImplementationAdapterFactory()));
            allowing(ctx).locateService(with(any(String.class)), with(secondServiceReference));
            will(returnValue(new SecondImplementationAdapterFactory()));
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

    public static <T> Matcher<T> any(@SuppressWarnings("unused") Class<T> type) {
        return new IsAnything<T>();
    }

    /**
     * Helper method to create a mock service reference
     */
    protected ServiceReference createServiceReference() {
        final ServiceReference ref = new ServiceReferenceImpl(1, new String[]{ TestSlingAdaptable.class.getName() }, new String[]{ITestAdapter.class.getName()});
        return ref;
    }

    private static final class ServiceReferenceImpl implements ServiceReference {

        private int ranking;
        private String[] adapters;
        private String[] classes;

        public ServiceReferenceImpl(final int order, final String[] adapters, final String[] classes) {
            this.ranking = order;
            this.adapters = adapters;
            this.classes = classes;
        }

        public boolean isAssignableTo(Bundle bundle, String className) {
            // TODO Auto-generated method stub
            return false;
        }

        public Bundle[] getUsingBundles() {
            // TODO Auto-generated method stub
            return null;
        }

        public String[] getPropertyKeys() {
            // TODO Auto-generated method stub
            return null;
        }

        public Object getProperty(String key) {
            if ( key.equals(Constants.SERVICE_RANKING) ) {
                return ranking;
            }
            if ( key.equals(AdapterFactory.ADAPTABLE_CLASSES) ) {
                return adapters;
            }
            if ( key.equals(AdapterFactory.ADAPTER_CLASSES) ) {
                return classes;
            }

            return null;
        }

        public Bundle getBundle() {
            // TODO Auto-generated method stub
            return null;
        }

        public int compareTo(Object reference) {
            Integer ranking1 = (Integer)getProperty(Constants.SERVICE_RANKING);
            Integer ranking2 = (Integer)((ServiceReference)reference).getProperty(Constants.SERVICE_RANKING);
            return ranking1.compareTo(ranking2);
        }
    };
    /**
     * Helper method to create a mock service reference
     */
    protected ServiceReference createServiceReference2() {
        final ServiceReference ref = new ServiceReferenceImpl(2, new String[]{ TestSlingAdaptable2.class.getName() }, new String[]{TestAdapter.class.getName()});
        return ref;
    }

    @org.junit.Test public void testUnitialized() {
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertTrue("AdapterFactory cache must be empty", am.getFactoryCache().isEmpty());
    }

    @org.junit.Test public void testInitialized() throws Exception {
        am.activate(this.createComponentContext());

        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertTrue("AdapterFactory cache must be empty", am.getFactoryCache().isEmpty());
    }

    @org.junit.Test public void testBindBeforeActivate() throws Exception {
        final ServiceReference ref = createServiceReference();
        am.bindAdapterFactory(ref);

        // no cache and no factories yet
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertTrue("AdapterFactory cache must be empty", am.getFactoryCache().isEmpty());

        am.activate(this.createComponentContext());

        // expect the factory, but cache is empty
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertEquals("AdapterFactoryDescriptors must contain one entry", 1, am.getFactories().size());
        assertTrue("AdapterFactory cache must be empty", am.getFactoryCache().isEmpty());
    }

    @org.junit.Test public void testBindAfterActivate() throws Exception {
        am.activate(this.createComponentContext());

        // no cache and no factories yet
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertTrue("AdapterFactoryDescriptors must be empty", am.getFactories().isEmpty());
        assertTrue("AdapterFactory cache must be empty", am.getFactoryCache().isEmpty());

        final ServiceReference ref = createServiceReference();
        am.bindAdapterFactory(ref);

        // expect the factory, but cache is empty
        assertNotNull("AdapterFactoryDescriptors must not be null", am.getFactories());
        assertEquals("AdapterFactoryDescriptors must contain one entry", 1, am.getFactories().size());
        assertTrue("AdapterFactory cache must be empty", am.getFactoryCache().isEmpty());

        Map<String, AdapterFactoryDescriptorMap> f = am.getFactories();
        AdapterFactoryDescriptorMap afdm = f.get(TestSlingAdaptable.class.getName());
        assertNotNull(afdm);

        AdapterFactoryDescriptor afd = afdm.get(ref);
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

    @org.junit.Test public void testAdaptMultipleAdapterFactories() throws Exception {
        final ServiceReference firstAdaptable = new ServiceReferenceImpl(1, new String[]{AdapterObject.class.getName()},  new String[]{ ParentInterface.class.getName(), FirstImplementation.class.getName()});
        final ServiceReference secondAdaptable = new ServiceReferenceImpl(2, new String[]{ AdapterObject.class.getName() }, new String[]{ParentInterface.class.getName(), SecondImplementation.class.getName()});

        am.activate(this.createMultipleAdaptersComponentContext(firstAdaptable, secondAdaptable));

        AdapterObject first = new AdapterObject(Want.FIRST_IMPL);
        assertNull("Expect no adapter", am.getAdapter(first, ParentInterface.class));

        AdapterObject second = new AdapterObject(Want.SECOND_IMPL);
        assertNull("Expect no adapter", am.getAdapter(second, ParentInterface.class));

        am.bindAdapterFactory(firstAdaptable);
        am.bindAdapterFactory(secondAdaptable);

        Object adapter = am.getAdapter(first, ParentInterface.class);
        assertNotNull("Did not get an adapter back for first implementation, service ranking 1", adapter);
        assertTrue("Did not get the correct adaptable back for first implementation, service ranking 1, ", adapter instanceof FirstImplementation);

        adapter = am.getAdapter(second, ParentInterface.class);
        assertNotNull("Did not get an adapter back for second implementation, service ranking 2", adapter);
        assertTrue("Did not get the correct adaptable back for second implementation, service ranking 2, ", adapter instanceof SecondImplementation);

        adapter = am.getAdapter(first, FirstImplementation.class);
        assertNotNull("Did not get an adapter back for first implementation, service ranking 1", adapter);
        assertTrue("Did not get the correct adaptable back for first implementation, service ranking 1, ", adapter instanceof FirstImplementation);

        adapter = am.getAdapter(second, SecondImplementation.class);
        assertNotNull("Did not get an adapter back for second implementation, service ranking 2", adapter);
        assertTrue("Did not get the correct adaptable back for second implementation, service ranking 2, ", adapter instanceof SecondImplementation);
    }

    @org.junit.Test public void testAdaptMultipleAdapterFactoriesReverseOrder() throws Exception {
        final ServiceReference firstAdaptable = new ServiceReferenceImpl(2, new String[]{AdapterObject.class.getName()},  new String[]{ ParentInterface.class.getName()});
        final ServiceReference secondAdaptable = new ServiceReferenceImpl(1, new String[]{AdapterObject.class.getName()},  new String[]{ ParentInterface.class.getName()});
        am.activate(this.createMultipleAdaptersComponentContext(firstAdaptable, secondAdaptable));

        AdapterObject first = new AdapterObject(Want.FIRST_IMPL);
        assertNull("Expect no adapter", am.getAdapter(first, ParentInterface.class));

        AdapterObject second = new AdapterObject(Want.SECOND_IMPL);
        assertNull("Expect no adapter", am.getAdapter(second, ParentInterface.class));

        am.bindAdapterFactory(firstAdaptable);
        am.bindAdapterFactory(secondAdaptable);

        Object adapter = am.getAdapter(first, ParentInterface.class);
        assertNotNull("Did not get an adapter back for first implementation, service ranking 2", adapter);
        assertTrue("Did not get the correct adaptable back for first implementation, service ranking 2, ", adapter instanceof FirstImplementation);

    }

    @org.junit.Test public void testAdaptMultipleAdapterFactoriesServiceRanking() throws Exception {
        final ServiceReference firstAdaptable = new ServiceReferenceImpl(1, new String[]{AdapterObject.class.getName()},  new String[]{ ParentInterface.class.getName(), FirstImplementation.class.getName()});
        final ServiceReference secondAdaptable = new ServiceReferenceImpl(2, new String[]{ AdapterObject.class.getName() }, new String[]{ParentInterface.class.getName(), SecondImplementation.class.getName()});

        am.activate(this.createMultipleAdaptersComponentContext(firstAdaptable, secondAdaptable));

        AdapterObject first = new AdapterObject(Want.INDIFFERENT);
        assertNull("Expect no adapter", am.getAdapter(first, ParentInterface.class));

        AdapterObject second = new AdapterObject(Want.INDIFFERENT);
        assertNull("Expect no adapter", am.getAdapter(second, ParentInterface.class));

        am.bindAdapterFactory(firstAdaptable);
        am.bindAdapterFactory(secondAdaptable);

        Object adapter = am.getAdapter(first, ParentInterface.class);
        assertNotNull("Did not get an adapter back for first implementation (from ParentInterface), service ranking 1", adapter);
        assertTrue("Did not get the correct adaptable back for first implementation, service ranking 1, ", adapter instanceof FirstImplementation);

        adapter = am.getAdapter(first, FirstImplementation.class);
        assertNotNull("Did not get an adapter back for first implementation, service ranking 1", adapter);
        assertTrue("Did not get the correct adaptable back for first implementation, service ranking 1, ", adapter instanceof FirstImplementation);

        adapter = am.getAdapter(second, SecondImplementation.class);
        assertNotNull("Did not get an adapter back for second implementation, service ranking 2", adapter);
        assertTrue("Did not get the correct adaptable back for second implementation, service ranking 2, ", adapter instanceof SecondImplementation);
    }

    @org.junit.Test public void testAdaptMultipleAdapterFactoriesServiceRankingSecondHigherOrder() throws Exception {
        final ServiceReference firstAdaptable = new ServiceReferenceImpl(2, new String[]{AdapterObject.class.getName()},  new String[]{ ParentInterface.class.getName(), FirstImplementation.class.getName()});
        final ServiceReference secondAdaptable = new ServiceReferenceImpl(1, new String[]{ AdapterObject.class.getName() }, new String[]{ParentInterface.class.getName(), SecondImplementation.class.getName()});

        am.activate(this.createMultipleAdaptersComponentContext(firstAdaptable, secondAdaptable));

        AdapterObject first = new AdapterObject(Want.INDIFFERENT);
        assertNull("Expect no adapter", am.getAdapter(first, ParentInterface.class));

        AdapterObject second = new AdapterObject(Want.INDIFFERENT);
        assertNull("Expect no adapter", am.getAdapter(second, ParentInterface.class));

        am.bindAdapterFactory(firstAdaptable);
        am.bindAdapterFactory(secondAdaptable);

        Object adapter = am.getAdapter(first, ParentInterface.class);
        assertNotNull("Did not get an adapter back for second implementation (from ParentInterface), service ranking 1", adapter);
        assertTrue("Did not get the correct adaptable back for second implementation, service ranking 1, ", adapter instanceof SecondImplementation);

        adapter = am.getAdapter(first, FirstImplementation.class);
        assertNotNull("Did not get an adapter back for first implementation, service ranking 1", adapter);
        assertTrue("Did not get the correct adaptable back for first implementation, service ranking 1, ", adapter instanceof FirstImplementation);

        adapter = am.getAdapter(second, SecondImplementation.class);
        assertNotNull("Did not get an adapter back for second implementation, service ranking 2", adapter);
        assertTrue("Did not get the correct adaptable back for second implementation, service ranking 2, ", adapter instanceof SecondImplementation);
    }

    @org.junit.Test public void testAdaptMultipleAdapterFactoriesServiceRankingReverse() throws Exception {
        final ServiceReference firstAdaptable = new ServiceReferenceImpl(1, new String[]{AdapterObject.class.getName()},  new String[]{ ParentInterface.class.getName(), FirstImplementation.class.getName()});
        final ServiceReference secondAdaptable = new ServiceReferenceImpl(2, new String[]{ AdapterObject.class.getName() }, new String[]{ParentInterface.class.getName(), SecondImplementation.class.getName()});

        am.activate(this.createMultipleAdaptersComponentContext(firstAdaptable, secondAdaptable));

        AdapterObject first = new AdapterObject(Want.INDIFFERENT);
        assertNull("Expect no adapter", am.getAdapter(first, ParentInterface.class));

        AdapterObject second = new AdapterObject(Want.INDIFFERENT);
        assertNull("Expect no adapter", am.getAdapter(second, ParentInterface.class));

        // bind these in reverse order from the non-reverse test
        am.bindAdapterFactory(secondAdaptable);
        am.bindAdapterFactory(firstAdaptable);

        Object adapter = am.getAdapter(first, ParentInterface.class);
        assertNotNull("Did not get an adapter back for first implementation (from ParentInterface), service ranking 1", adapter);
        assertTrue("Did not get the correct adaptable back for first implementation, service ranking 1, ", adapter instanceof FirstImplementation);

        adapter = am.getAdapter(first, FirstImplementation.class);
        assertNotNull("Did not get an adapter back for first implementation, service ranking 1", adapter);
        assertTrue("Did not get the correct adaptable back for first implementation, service ranking 1, ", adapter instanceof FirstImplementation);

        adapter = am.getAdapter(second, SecondImplementation.class);
        assertNotNull("Did not get an adapter back for second implementation, service ranking 2", adapter);
        assertTrue("Did not get the correct adaptable back for second implementation, service ranking 2, ", adapter instanceof SecondImplementation);
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

    public class FirstImplementationAdapterFactory implements AdapterFactory {

        public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
            if (adaptable instanceof AdapterObject) {
                AdapterObject adapterObject = (AdapterObject) adaptable;
                switch (adapterObject.getWhatWeWant()) {
                    case FIRST_IMPL:
                    case INDIFFERENT:
                        return (AdapterType) new FirstImplementation();
                    case SECOND_IMPL:
                        return null;
                }
            }
            throw new RuntimeException("Must pass the correct adaptable");
        }
    }

    public class SecondImplementationAdapterFactory implements AdapterFactory {

        public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
            if (adaptable instanceof AdapterObject) {
                AdapterObject adapterObject = (AdapterObject) adaptable;
                switch (adapterObject.getWhatWeWant()) {
                    case SECOND_IMPL:
                    case INDIFFERENT:
                        return (AdapterType) new SecondImplementation();
                    case FIRST_IMPL:
                        return null;
                }
            }
            throw new RuntimeException("Must pass the correct adaptable");
        }
    }

    public static interface ParentInterface {
    }

    public static class FirstImplementation implements ParentInterface {
    }

    public static class SecondImplementation implements ParentInterface {
    }

    public enum Want {

        /**
         * Indicates we definitively want the "first implementation" adapter factory to execute the adapt
         */
        FIRST_IMPL,

        /**
         * Indicates we definitively want the "second implementation" adapter factory to execute the adapt
         */
        SECOND_IMPL,

        /**
         * Indicates we are indifferent to which factory is used to execute the adapt, used for testing service ranking
         */
        INDIFFERENT
    }

    public static class AdapterObject {
        private Want whatWeWant;

        public AdapterObject(Want whatWeWant) {
            this.whatWeWant = whatWeWant;
        }

        public Want getWhatWeWant() {
            return whatWeWant;
        }
    }

}
