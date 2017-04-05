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
package org.apache.sling.auth.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.auth.core.AuthConstants;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

public class SlingAuthenticatorServiceListenerTest {

    private void assertPaths(final PathBasedHolderCache<AuthenticationRequirementHolder> cache,
            final String[] paths,
            final ServiceReference<?>[] refs) {
        assertEquals(paths.length, refs.length);
        assertEquals(paths.length, cache.getHolders().size());
        for(final AuthenticationRequirementHolder h : cache.getHolders()) {
            boolean found = false;
            int index = 0;
            while ( !found && index < paths.length ) {
                if (paths[index].equals(h.path) && refs[index].equals(h.serviceReference) ) {
                    found = true;
                } else {
                    index++;
                }
            }
            assertTrue(Arrays.toString(paths) + " should contain " + h.path, found);
        }
    }

    private long serviceId = 1;

    private final List<ServiceReference<?>> refs = new ArrayList<>();

    private ServiceReference<?> createServiceReference(final String[] paths) {
        final ServiceReference<?> ref = mock(ServiceReference.class);
        when(ref.getProperty(AuthConstants.AUTH_REQUIREMENTS)).thenReturn(paths);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        serviceId++;

        for(final ServiceReference<?> r : refs) {
            when(ref.compareTo(r)).thenReturn(1);
            when(r.compareTo(ref)).thenReturn(-1);
        }
        when(ref.compareTo(ref)).thenReturn(0);

        refs.add(ref);
        return ref;
    }

    @Test public void testAddRemoveRegistration() {
        final PathBasedHolderCache<AuthenticationRequirementHolder> cache = new PathBasedHolderCache<AuthenticationRequirementHolder>();
        final BundleContext context = mock(BundleContext.class);
        final SlingAuthenticatorServiceListener listener = SlingAuthenticatorServiceListener.createListener(context, cache);

        assertTrue(cache.getHolders().isEmpty());

        final ServiceReference<?> ref = createServiceReference(new String[] {"/path1"});
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref));

        assertEquals(1, cache.getHolders().size());
        assertPaths(cache, new String[] {"/path1"},
                           new ServiceReference<?>[] {ref});
        assertEquals(ref, cache.getHolders().get(0).serviceReference);

        listener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref));

        assertTrue(cache.getHolders().isEmpty());
    }

    @Test public void testDuplicateRegistration() {
        final PathBasedHolderCache<AuthenticationRequirementHolder> cache = new PathBasedHolderCache<AuthenticationRequirementHolder>();
        final BundleContext context = mock(BundleContext.class);
        final SlingAuthenticatorServiceListener listener = SlingAuthenticatorServiceListener.createListener(context, cache);

        final ServiceReference<?> ref1 = createServiceReference(new String[] {"/path1", "/path1", "/path2"});
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref1));

        final ServiceReference<?> ref2 = createServiceReference(new String[] {"/path2", "/path3"});
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref2));
        assertPaths(cache, new String[] {"/path1", "/path2", "/path2", "/path3"},
                           new ServiceReference<?>[] {ref1, ref1, ref2, ref2});

        listener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref2));

        assertPaths(cache, new String[] {"/path1", "/path2"},
                           new ServiceReference<?>[] {ref1, ref1});

        listener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref1));
        assertTrue(cache.getHolders().isEmpty());
    }

    @Test public void testAddRemoveRegistrations() {
        final PathBasedHolderCache<AuthenticationRequirementHolder> cache = new PathBasedHolderCache<AuthenticationRequirementHolder>();
        final BundleContext context = mock(BundleContext.class);
        final SlingAuthenticatorServiceListener listener = SlingAuthenticatorServiceListener.createListener(context, cache);

        final ServiceReference<?> ref1 = createServiceReference(new String[] {"/path1"});
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref1));

        final ServiceReference<?> ref2 = createServiceReference(new String[] {"/path2", "/path3"});
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref2));

        final ServiceReference<?> ref3 = createServiceReference(new String[] {"/path4", "/path5"});
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref3));

        assertPaths(cache, new String[] { "/path1", "/path2", "/path3", "/path4", "/path5"},
                           new ServiceReference<?>[] {ref1, ref2, ref2, ref3, ref3});

        listener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref2));

        assertPaths(cache, new String[] { "/path1", "/path4", "/path5"},
                new ServiceReference<?>[] {ref1, ref3, ref3});

        listener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref1));
        assertPaths(cache, new String[] { "/path4", "/path5"},
                new ServiceReference<?>[] {ref3, ref3});

        listener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref3));
        assertTrue(cache.getHolders().isEmpty());
    }

    @Test public void testModifyRegistration() {
        final PathBasedHolderCache<AuthenticationRequirementHolder> cache = new PathBasedHolderCache<AuthenticationRequirementHolder>();
        final BundleContext context = mock(BundleContext.class);
        final SlingAuthenticatorServiceListener listener = SlingAuthenticatorServiceListener.createListener(context, cache);

        final ServiceReference<?> ref1 = createServiceReference(new String[] {"/path1", "/path2", "/path3"});
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref1));
        assertPaths(cache, new String[] { "/path1", "/path2", "/path3"},
                new ServiceReference<?>[] {ref1, ref1, ref1});

        when(ref1.getProperty(AuthConstants.AUTH_REQUIREMENTS)).thenReturn(new String[] {"/path1", "/path4", "/path5"});
        assertPaths(cache, new String[] { "/path1", "/path2", "/path3"},
                new ServiceReference<?>[] {ref1, ref1, ref1});

        listener.serviceChanged(new ServiceEvent(ServiceEvent.MODIFIED, ref1));
        assertPaths(cache, new String[] { "/path1", "/path4", "/path5"},
                new ServiceReference<?>[] {ref1, ref1, ref1});

        listener.serviceChanged(new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, ref1));
        assertTrue(cache.getHolders().isEmpty());

    }
}
