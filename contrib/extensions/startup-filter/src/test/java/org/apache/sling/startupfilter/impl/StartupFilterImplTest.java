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
package org.apache.sling.startupfilter.impl;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.startupfilter.StartupInfoProvider;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.action.DoAllAction;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

/** Test the StartupFilterImpl */
public class StartupFilterImplTest {
    static private class TestProvider implements StartupInfoProvider, ServiceReference {
        private final String info;
        
        TestProvider(String s) {
            info = s;
        }
        
        public String getProgressInfo() {
            return info;
        }

        public Object getProperty(String key) {
            return null;
        }

        public String[] getPropertyKeys() {
            return null;
        }

        public Bundle getBundle() {
            return null;
        }

        public Bundle[] getUsingBundles() {
            return null;
        }

        public boolean isAssignableTo(Bundle bundle, String className) {
            return false;
        }

        public int compareTo(Object reference) {
            return 0;
        }
    }
    static private class TestFilterImpl extends StartupFilterImpl {
        void setup(ComponentContext ctx) throws Exception {
            activate(ctx);
        }
    };
    
    static private class ChangeInteger implements Action {
        private final boolean increment;
        private final AtomicInteger value;
        
        ChangeInteger(AtomicInteger value, boolean increment) {
            this.increment = increment;
            this.value = value;
        }
        public void describeTo(Description d) {
            d.appendText(increment ? "increment" : "decrement");
            d.appendText(" an integer");
        }
        public Object invoke(Invocation invocation) throws Throwable {
            if(increment) {
                value.incrementAndGet();
            } else {
                value.decrementAndGet();
            }
            return null;
        }
    };

    private TestFilterImpl filter;
    private Mockery mockery;
    private HttpServletRequest request; 
    private HttpServletResponse response;
    private FilterChain chain;
    private int lastReturnedStatus;
    private String lastReturnedMessage;
    private AtomicInteger activeFilterCount;
    private ServiceRegistration serviceRegistration;

    @Before
    public void setup() {
        activeFilterCount = new AtomicInteger();
        mockery = new Mockery();
        request = mockery.mock(HttpServletRequest.class); 
        response = mockery.mock(HttpServletResponse.class);
        chain = mockery.mock(FilterChain.class);
        serviceRegistration = mockery.mock(ServiceRegistration.class);
        filter = new TestFilterImpl();
    }
    
    private void setProvider(final TestProvider provider) throws Exception {
        final BundleContext bundleContext = mockery.mock(BundleContext.class); 
        final ComponentContext componentContext = mockery.mock(ComponentContext.class); 
        
        final Action storeResponse = new Action() {
            public void describeTo(Description d) {
                d.appendText("Store HTTP response values");
            }

            public Object invoke(Invocation invocation) throws Throwable {
                lastReturnedStatus = (Integer)invocation.getParameter(0);
                lastReturnedMessage = (String)invocation.getParameter(1);
                return null;
            }
        };
        
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(StartupFilterImpl.ACTIVE_BY_DEFAULT_PROP, Boolean.TRUE);
        
        final ServiceReference [] providerRefs = provider == null ? null : new ServiceReference[] { provider };
        mockery.checking(new Expectations() {{
            allowing(componentContext).getBundleContext();
            will(returnValue(bundleContext));
            
            allowing(componentContext).getProperties();
            will(returnValue(props));
            
            allowing(bundleContext).createFilter(with(any(String.class)));
            allowing(bundleContext).addServiceListener(with(any(ServiceListener.class)));
            allowing(bundleContext).addServiceListener(with(any(ServiceListener.class)), with(any(String.class)));
            
            allowing(bundleContext).getServiceReferences(StartupInfoProvider.class.getName(), null);
            will(returnValue(providerRefs));
            allowing(bundleContext).getService(with(any(ServiceReference.class)));
            will(returnValue(provider));
            
            allowing(bundleContext).registerService(with(Filter.class.getName()), with(any(Object.class)), with(any(Dictionary.class)));
            will(new DoAllAction(
                    new ChangeInteger(activeFilterCount, true),
                    returnValue(serviceRegistration)
                    ));
            
            allowing(response).sendError(with(any(Integer.class)), with(any(String.class)));
            will(storeResponse);
            
            allowing(serviceRegistration).unregister();
            will(new ChangeInteger(activeFilterCount, false));
        }});
        
        filter.setup(componentContext);
    }
    
    private void assertRequest(final int expectedStatus, final String expectedMessage) throws Exception {
        lastReturnedMessage = null;
        lastReturnedStatus = -1;
        
        filter.doFilter(request, response, chain);
        
        // status 0 means we expect the request to go through
        assertEquals("Expecting status to match", 
                expectedStatus, lastReturnedStatus);
        assertEquals("Expecting message to match", 
                expectedMessage, lastReturnedMessage);
    }
    
    @Test
    public void testInitialState() throws Exception {
        setProvider(null);
        assertEquals("Initially expecting the default status message", 1, activeFilterCount.get());
        assertRequest(503, StartupFilterImpl.DEFAULT_MESSAGE);
    }

    @Test
    public void testDisabling() throws Exception {
        setProvider(null);
        assertEquals("Initially expecting one filter service", 1, activeFilterCount.get());
        filter.disable();
        assertEquals("Expecting filter service to be gone", 0, activeFilterCount.get());
    }

    @Test
    public void testProviders() throws Exception {
        final TestProvider p = new TestProvider("TEST");
        
        setProvider(p);
        assertEquals("Initially expecting one filter service", 1, activeFilterCount.get());
        
        final String expectedMessage = StartupFilterImpl.DEFAULT_MESSAGE + "\nTEST";
        assertRequest(503, expectedMessage);

        filter.disable();
        assertEquals("Expecting filter service to be gone", 0, activeFilterCount.get());
    }
}
