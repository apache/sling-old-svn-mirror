/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.rewriter.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class HashingServiceTrackerCustomizerTest {

    @Test public void testAddRemove() {
        final BundleContext bc = mock(BundleContext.class);
        final HashingServiceTrackerCustomizer<Object> tracker = new HashingServiceTrackerCustomizer<Object>(bc, "java.lang.Object");

        assertNull(tracker.getFactory("a"));
        final Object service1 = new Object();
        final ServiceReference ref1 = mock(ServiceReference.class);
        when(ref1.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");
        when(bc.getService(ref1)).thenReturn(service1);
        tracker.addingService(ref1);

        assertNotNull(tracker.getFactory("a"));
        assertEquals(service1, tracker.getFactory("a"));

        tracker.removedService(ref1, service1);
        assertNull(tracker.getFactory("a"));
    }

    @Test public void testMultipleAddRemove() {
        final BundleContext bc = mock(BundleContext.class);
        final HashingServiceTrackerCustomizer<Object> tracker = new HashingServiceTrackerCustomizer<Object>(bc, "java.lang.Object");

        assertNull(tracker.getFactory("a"));
        final Object service1 = new Object();
        final Object service2 = new Object();
        final Object service3 = new Object();

        final ServiceReference ref1 = mock(ServiceReference.class);
        when(ref1.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");
        when(bc.getService(ref1)).thenReturn(service1);

        final ServiceReference ref2 = mock(ServiceReference.class);
        when(ref2.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");
        when(bc.getService(ref2)).thenReturn(service2);

        final ServiceReference ref3 = mock(ServiceReference.class);
        when(ref3.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");
        when(bc.getService(ref3)).thenReturn(service3);

        // ordering ref3 has highest ranking, then ref2, then ref1
        when(ref1.compareTo(ref2)).thenReturn(-1);
        when(ref1.compareTo(ref3)).thenReturn(-1);
        when(ref2.compareTo(ref1)).thenReturn( 1);
        when(ref2.compareTo(ref3)).thenReturn(-1);
        when(ref3.compareTo(ref1)).thenReturn( 1);
        when(ref3.compareTo(ref2)).thenReturn( 1);

        tracker.addingService(ref1);

        assertNotNull(tracker.getFactory("a"));
        assertEquals(service1, tracker.getFactory("a"));

        tracker.addingService(ref2);
        assertEquals(service2, tracker.getFactory("a"));

        tracker.addingService(ref3);
        assertEquals(service3, tracker.getFactory("a"));

        tracker.removedService(ref1, service1);
        assertEquals(service3, tracker.getFactory("a"));

        tracker.removedService(ref3, service3);
        assertEquals(service2, tracker.getFactory("a"));

        tracker.removedService(ref2, service2);
        assertNull(tracker.getFactory("a"));
    }
}
