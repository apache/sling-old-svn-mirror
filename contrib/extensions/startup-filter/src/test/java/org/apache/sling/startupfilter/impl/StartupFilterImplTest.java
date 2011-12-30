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

import org.apache.sling.startupfilter.StartupFilter;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.action.DoAllAction;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

/** Test the StartupFilterImpl */
public class StartupFilterImplTest {
    static private class TestPip implements StartupFilter.ProgressInfoProvider {
        String info;
        
        TestPip(String s) {
            info = s;
        }
        
        public String getInfo() {
            return info;
        }
    };
    
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
    private AtomicInteger doChainCount;
    private int lastReturnedStatus;
    private String lastReturnedMessage;
    private AtomicInteger activeFilterCount;
    private ServiceRegistration serviceRegistration;

    @Before
    public void setup() throws Exception {
        doChainCount = new AtomicInteger();
        activeFilterCount = new AtomicInteger();
        mockery = new Mockery();
        final BundleContext bundleContext = mockery.mock(BundleContext.class); 
        final ComponentContext componentContext = mockery.mock(ComponentContext.class); 
        request = mockery.mock(HttpServletRequest.class); 
        response = mockery.mock(HttpServletResponse.class);
        chain = mockery.mock(FilterChain.class);
        serviceRegistration = mockery.mock(ServiceRegistration.class);
        filter = new TestFilterImpl();
        
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
        props.put("default.filter.active", Boolean.TRUE);
        
        mockery.checking(new Expectations() {{
            allowing(componentContext).getBundleContext();
            will(returnValue(bundleContext));
            
            allowing(componentContext).getProperties();
            will(returnValue(props));
            
            allowing(bundleContext).registerService(with(Filter.class.getName()), with(any(Object.class)), with(any(Dictionary.class)));
            will(new DoAllAction(
                    new ChangeInteger(activeFilterCount, true),
                    returnValue(serviceRegistration)
                    ));
            
            allowing(chain).doFilter(request, response);
            will(new ChangeInteger(doChainCount, true));
            
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
        final int oldDoChainCount = doChainCount.get();
        
        filter.doFilter(request, response, chain);
        
        // status 0 means we expect the request to go through
        if(expectedStatus == 0) {
            assertEquals("Expecting doChain to have been be called once", 
                    1, doChainCount.get() - oldDoChainCount);
        } else {
            assertEquals("Expecting status to match", 
                    expectedStatus, lastReturnedStatus);
            assertEquals("Expecting message to match", 
                    expectedMessage, lastReturnedMessage);
        }
    }
    
    @Test
    public void testInitialState() throws Exception {
        assertEquals("Initially expecting one filter service", 1, activeFilterCount.get());
        assertRequest(503, StartupFilter.DEFAULT_STATUS_MESSAGE);
    }
    
    @Test
    public void testDefaultFilterRemoved() throws Exception {
        assertEquals("Initially expecting one filter service", 1, activeFilterCount.get());
        filter.removeProgressInfoProvider(StartupFilter.DEFAULT_INFO_PROVIDER);
        assertEquals("Expecting filter service to be gone", 0, activeFilterCount.get());
        assertRequest(0, null);
    }
    
    @Test
    public void testSeveralProviders() throws Exception {
        final StartupFilter.ProgressInfoProvider [] pips = {
                new TestPip("one"),
                new TestPip("two"),
                new TestPip("three"),
        };
        
        assertEquals("Initially expecting one filter service", 1, activeFilterCount.get());
        
        // Last added provider must be active
        for(StartupFilter.ProgressInfoProvider pip : pips) {
            filter.addProgressInfoProvider(pip);
            assertRequest(503, pip.getInfo());
        }
        
        assertEquals("After adding several providers, expecting one filter service", 1, activeFilterCount.get());
        
        // When removing a provider the previous one becomes active
        for(int i = pips.length - 1; i >= 0; i--) {
            assertRequest(503, pips[i].getInfo());
            filter.removeProgressInfoProvider(pips[i]);
        }

        // After removing all, default is active again
        assertEquals("After removing providers, expecting one filter service", 1, activeFilterCount.get());
        assertRequest(503, StartupFilter.DEFAULT_STATUS_MESSAGE);

        // Now remove default and check
        filter.removeProgressInfoProvider(StartupFilter.DEFAULT_INFO_PROVIDER);
        assertRequest(0, null);
        assertEquals("Expecting filter service to be gone", 0, activeFilterCount.get());
    }
}
