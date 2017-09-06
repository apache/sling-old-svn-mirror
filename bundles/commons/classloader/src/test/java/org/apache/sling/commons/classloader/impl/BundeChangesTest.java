/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.classloader.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

public class BundeChangesTest {

    protected Mockery context;

    public BundeChangesTest() {
        this.context = new JUnit4Mockery();
    }

    @Test public void testBundleUpdate() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        final AtomicBoolean registerCalled = new AtomicBoolean(false);
        final AtomicBoolean unregisterCalled = new AtomicBoolean(false);
        final Activator listener = new Activator() {

            @Override
            protected void registerManagerFactory() {
                registerCalled.set(true);
            }

            @Override
            protected void unregisterManagerFactory() {
                unregisterCalled.set(true);
            }
        };
        final Bundle bundle = this.context.mock(Bundle.class);
        this.context.checking(new Expectations() {{
            allowing(bundle).getHeaders();
            will(returnValue(new Hashtable<>()));
            allowing(bundle).getBundleId();
            will(returnValue(2L));
        }});
        final AtomicBoolean hasUnresolvedPackages = new AtomicBoolean(false);
        final DynamicClassLoaderManagerFactory dclmf = new DynamicClassLoaderManagerFactory(null, null) {

            @Override
            public boolean hasUnresolvedPackages(Bundle bundle) {
                return hasUnresolvedPackages.get();
            }

        };
        final Field field = Activator.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(listener, dclmf);

        listener.bundleChanged(new BundleEvent(BundleEvent.STARTED, bundle));
        assertFalse(registerCalled.get());
        assertFalse(unregisterCalled.get());

        // at this point the bundle is not used, so nothing should happen on update:

        // step one: stop bundle and unresolve
        listener.bundleChanged(new BundleEvent(BundleEvent.STOPPING, bundle));
        listener.bundleChanged(new BundleEvent(BundleEvent.STOPPED, bundle));
        listener.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, bundle));
        assertFalse(registerCalled.get());
        assertFalse(unregisterCalled.get());

        // step two: update bundle and resolved
        listener.bundleChanged(new BundleEvent(BundleEvent.UPDATED, bundle));
        listener.bundleChanged(new BundleEvent(BundleEvent.RESOLVED, bundle));
        assertFalse(registerCalled.get());
        assertFalse(unregisterCalled.get());

        // step three: start bundle
        listener.bundleChanged(new BundleEvent(BundleEvent.STARTING, bundle));
        assertFalse(registerCalled.get());
        assertFalse(unregisterCalled.get());
        listener.bundleChanged(new BundleEvent(BundleEvent.STARTED, bundle));
        assertFalse(registerCalled.get());
        assertFalse(unregisterCalled.get());

        // mark bundle as being used
        dclmf.addUsedBundle(bundle); // bundle is used

        // and update
        listener.bundleChanged(new BundleEvent(BundleEvent.STOPPING, bundle));
        listener.bundleChanged(new BundleEvent(BundleEvent.STOPPED, bundle));
        listener.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, bundle));
        assertTrue(registerCalled.get());
        assertTrue(unregisterCalled.get());

        registerCalled.set(false);
        unregisterCalled.set(false);

        // step two: update bundle
        listener.bundleChanged(new BundleEvent(BundleEvent.UPDATED, bundle));
        listener.bundleChanged(new BundleEvent(BundleEvent.RESOLVED, bundle));
        assertTrue(registerCalled.get());
        assertTrue(unregisterCalled.get());

        registerCalled.set(false);
        unregisterCalled.set(false);

        // step three: start bundle
        listener.bundleChanged(new BundleEvent(BundleEvent.STARTING, bundle));
        assertFalse(registerCalled.get());
        assertFalse(unregisterCalled.get());
        listener.bundleChanged(new BundleEvent(BundleEvent.STARTED, bundle));
        assertTrue(registerCalled.get());
        assertTrue(unregisterCalled.get());
    }
}
