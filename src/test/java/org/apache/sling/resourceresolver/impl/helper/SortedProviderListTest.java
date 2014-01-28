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
package org.apache.sling.resourceresolver.impl.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.tree.ResourceProviderFactoryHandler;
import org.apache.sling.resourceresolver.impl.tree.ResourceProviderHandler;
import org.junit.Test;
import org.osgi.framework.Constants;

public class SortedProviderListTest {

    @Test public void testAddRemoveResourceProvider() {
        final ResourceProviderImpl rp1 = new ResourceProviderImpl(null, 1L);
        final AdaptableResourceProviderImpl rp2 = new AdaptableResourceProviderImpl(null, 2L);
        final AdaptableResourceProviderImpl rp3 = new AdaptableResourceProviderImpl(new String[] {"/hello"}, 3L);
        final ResourceProviderImpl rp4 = new ResourceProviderImpl(new String[] {"/you"}, 4L);

        final SortedProviderList<Adaptable> spl = new SortedProviderList<Adaptable>(Adaptable.class);
        check(spl, null);
        spl.add(new ResourceProviderHandler(rp1, rp1.getProperties()));
        check(spl, null);
        spl.add(new ResourceProviderHandler(rp2, rp2.getProperties()));
        check(spl, null, rp2);
        spl.add(new ResourceProviderHandler(rp3, rp3.getProperties()));
        check(spl, null, rp2, rp3);
        spl.add(new ResourceProviderHandler(rp4, rp4.getProperties()));
        check(spl, null, rp2, rp3);

        spl.remove(new ResourceProviderHandler(rp1, rp1.getProperties()));
        check(spl, null, rp2, rp3);
        spl.remove(new ResourceProviderHandler(rp1, rp1.getProperties()));
        check(spl, null, rp2, rp3);
        spl.remove(new ResourceProviderHandler(rp4, rp4.getProperties()));
        check(spl, null, rp2, rp3);
        spl.remove(new ResourceProviderHandler(rp2, rp2.getProperties()));
        check(spl, null, rp3);
        spl.remove(new ResourceProviderHandler(rp3, rp3.getProperties()));
        check(spl, null);
    }

    @Test public void testSortingRP() {
        final AdaptableResourceProviderImpl rp1 = new AdaptableResourceProviderImpl(new String[] {"/d", "/a", "x"}, 1L);
        final AdaptableResourceProviderImpl rp2 = new AdaptableResourceProviderImpl(null, 2L);
        final AdaptableResourceProviderImpl rp3 = new AdaptableResourceProviderImpl(new String[] {"/b"}, 3L);
        final AdaptableResourceProviderImpl rp4 = new AdaptableResourceProviderImpl(new String[] {"/a/a"}, 4L);
        final AdaptableResourceProviderImpl rp5 = new AdaptableResourceProviderImpl(new String[] {"/all/or/nothing"}, 5L);

        final SortedProviderList<Adaptable> spl = new SortedProviderList<Adaptable>(Adaptable.class);
        check(spl, null);
        spl.add(new ResourceProviderHandler(rp1, rp1.getProperties()));
        check(spl, null, rp1);
        spl.add(new ResourceProviderHandler(rp2, rp2.getProperties()));
        check(spl, null, rp2, rp1);
        spl.add(new ResourceProviderHandler(rp3, rp3.getProperties()));
        check(spl, null, rp2, rp1, rp3);
        spl.add(new ResourceProviderHandler(rp4, rp4.getProperties()));
        check(spl, null, rp2, rp1, rp4, rp3);
        spl.add(new ResourceProviderHandler(rp5, rp5.getProperties()));
        check(spl, null, rp2, rp1, rp4, rp5, rp3);
    }

    @Test public void testAddRemoveResourceProviderFactory() {
        final ResourceProviderImpl rp1 = new ResourceProviderImpl(null, 1L);
        final AdaptableResourceProviderImpl rp2 = new AdaptableResourceProviderImpl(null, 2L);
        final AdaptableResourceProviderImpl rp3 = new AdaptableResourceProviderImpl(new String[] {"/hello"}, 3L);
        final ResourceProviderImpl rp4 = new ResourceProviderImpl(new String[] {"/you"}, 4L);

        final ResourceResolverContext ctx = new ResourceResolverContext(false, null, new ResourceAccessSecurityTracker(), MockFeaturesHolder.INSTANCE);

        final SortedProviderList<Adaptable> spl = new SortedProviderList<Adaptable>(Adaptable.class);
        check(spl, ctx);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp1), rp1.getProperties()));
        check(spl, ctx);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp2), rp2.getProperties()));
        check(spl, ctx, rp2);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp3), rp3.getProperties()));
        check(spl, ctx, rp2, rp3);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp4), rp4.getProperties()));
        check(spl, ctx, rp2, rp3);

        spl.remove(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp1), rp1.getProperties()));
        check(spl, ctx, rp2, rp3);
        spl.remove(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp1), rp1.getProperties()));
        check(spl, ctx, rp2, rp3);
        spl.remove(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp4), rp4.getProperties()));
        check(spl, ctx, rp2, rp3);
        spl.remove(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp2), rp2.getProperties()));
        check(spl, ctx, rp3);
        spl.remove(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp3), rp3.getProperties()));
        check(spl, ctx);
    }

    @Test public void testSortingRF() {
        final AdaptableResourceProviderImpl rp1 = new AdaptableResourceProviderImpl(new String[] {"/d", "/a", "x"}, 1L);
        final AdaptableResourceProviderImpl rp2 = new AdaptableResourceProviderImpl(null, 2L);
        final AdaptableResourceProviderImpl rp3 = new AdaptableResourceProviderImpl(new String[] {"/b"}, 3L);
        final AdaptableResourceProviderImpl rp4 = new AdaptableResourceProviderImpl(new String[] {"/a/a"}, 4L);
        final AdaptableResourceProviderImpl rp5 = new AdaptableResourceProviderImpl(new String[] {"/all/or/nothing"}, 5L);

        final ResourceResolverContext ctx = new ResourceResolverContext(false, null, new ResourceAccessSecurityTracker(), null);

        final SortedProviderList<Adaptable> spl = new SortedProviderList<Adaptable>(Adaptable.class);
        check(spl, ctx);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp1), rp1.getProperties()));
        check(spl, ctx, rp1);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp2), rp2.getProperties()));
        check(spl, ctx, rp2, rp1);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp3), rp3.getProperties()));
        check(spl, ctx, rp2, rp1, rp3);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp4), rp4.getProperties()));
        check(spl, ctx, rp2, rp1, rp4, rp3);
        spl.add(new ResourceProviderFactoryHandler(new ResourceProviderFactoryImpl(rp5), rp5.getProperties()));
        check(spl, ctx, rp2, rp1, rp4, rp5, rp3);
    }

    @Test public void checkExceptions() {
        final AdaptableResourceProviderImpl rp2 = new AdaptableResourceProviderImpl(null, 2L);

        final SortedProviderList<Adaptable> spl = new SortedProviderList<Adaptable>(Adaptable.class);
        spl.add(new ResourceProviderHandler(rp2, rp2.getProperties()));

        final Iterator<Adaptable> i = spl.getProviders(null, null);
        assertTrue(i.hasNext());
        i.next(); // one entry
        assertFalse(i.hasNext());
        try {
            i.remove();
            fail();
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
        try {
            i.next();
            fail();
        } catch (NoSuchElementException nsee) {
            // expected
        }
        assertFalse(i.hasNext());
    }

    /**
     * Helper method checking the order of the sorted array.
     */
    private void check(final SortedProviderList<Adaptable> spl,
                    final ResourceResolverContext ctx,
                    final Adaptable... objects) {
        final int expectedCount = objects == null ? 0 : objects.length;
        final Iterator<Adaptable> i = spl.getProviders(ctx, null);
        int count = 0;
        while ( i.hasNext() ) {
            final Adaptable a = i.next();
            assertEquals(objects[count], a);
            count++;
        }
        assertEquals(expectedCount, count);
    }

    private static class ResourceProviderImpl implements ResourceProvider {

        private final String[] roots;
        private final Long serviceId;

        public ResourceProviderImpl(String[] roots, Long serviceId) {
            this.roots = roots;
            this.serviceId = serviceId;
        }

        public Map<String, Object> getProperties() {
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(Constants.SERVICE_ID, serviceId);
            props.put(ResourceProvider.ROOTS, roots);
            return props;
        }

        public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request, String path) {
            return null;
        }

        public Resource getResource(ResourceResolver resourceResolver, String path) {
            return null;
        }

        public Iterator<Resource> listChildren(Resource parent) {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if ( obj instanceof ResourceProviderImpl ) {
                return this.serviceId.equals(((ResourceProviderImpl)obj).serviceId);
            }
            return false;
        }
    }

    private static class AdaptableResourceProviderImpl extends ResourceProviderImpl
    implements Adaptable {

        public AdaptableResourceProviderImpl(String[] roots, Long serviceId) {
            super(roots, serviceId);
        }

        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return null;
        }
    }

    private static class ResourceProviderFactoryImpl implements ResourceProviderFactory {

        private final ResourceProviderImpl resourceProviderImpl;

        public ResourceProviderFactoryImpl(ResourceProviderImpl rpi) {
            this.resourceProviderImpl = rpi;
        }

        public ResourceProvider getResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
            return this.resourceProviderImpl;
        }

        public ResourceProvider getAdministrativeResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
            return this.resourceProviderImpl;
        }

        @Override
        public boolean equals(Object obj) {
            if ( obj instanceof ResourceProviderFactoryImpl ) {
                return this.resourceProviderImpl.equals(((ResourceProviderFactoryImpl)obj).resourceProviderImpl);
            }
            return false;
        }
    }
}
