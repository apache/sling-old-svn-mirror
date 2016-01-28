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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/** Test the service-ranking based sorting of mock service references */
public class MockServiceReferencesSortTest {
    
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
    public void testServicesOrder() {
        assertEquals("12345", getSortedServicesString(bundleContext));
    }

    private static ServiceRegistration<?> registerStringService(BundleContext ctx, int index) {
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, new Integer(index));
        return ctx.registerService(String.class.getName(), String.valueOf(index), props);
    }
    
    /** Register services with a specific ranking, sort their references and 
     *  return their concatenated toString() values.
     *  Use to test service references sorting.
     */
    private static String getSortedServicesString(BundleContext ctx) {
        final List<ServiceRegistration<?>> toCleanup = new ArrayList<ServiceRegistration<?>>();
        
        toCleanup.add(registerStringService(ctx, 3));
        toCleanup.add(registerStringService(ctx, 5));
        toCleanup.add(registerStringService(ctx, 4));
        toCleanup.add(registerStringService(ctx, 1));
        toCleanup.add(registerStringService(ctx, 2));
        
        ServiceReference<?> [] refs = null;
        try {
            refs = ctx.getServiceReferences(String.class.getName(), null);
        } catch(InvalidSyntaxException ise) {
            fail("Unexpected InvalidSyntaxException");
        }
        assertNotNull("Expecting our service references", refs);
        Arrays.sort(refs);
        
        final StringBuilder sb = new StringBuilder();
        for(ServiceReference<?> ref : refs) {
            sb.append(ctx.getService(ref).toString());
            ctx.ungetService(ref);
        }
        
        for(ServiceRegistration<?> reg : toCleanup) {
            reg.unregister();
        }
        
        return sb.toString();
    }

}