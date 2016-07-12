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
package org.apache.sling.resourceresolver.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class FactoryPreconditionsTest {

    @Test public void testNoRequiredProviders() {
        final ResourceProviderTracker tracker = Mockito.mock(ResourceProviderTracker.class);
        final ResourceProviderStorage storage = Mockito.mock(ResourceProviderStorage.class);
        Mockito.when(tracker.getResourceProviderStorage()).thenReturn(storage);

        FactoryPreconditions conditions = new FactoryPreconditions();
        conditions.activate(null, null, null, tracker);

        assertTrue(conditions.checkPreconditions(null, null));

        conditions = new FactoryPreconditions();
        conditions.activate(null, new String[0], new String[0], tracker);

        assertTrue(conditions.checkPreconditions(null, null));
    }

    @Test public void testDeactivated() {
        final ResourceProviderTracker tracker = Mockito.mock(ResourceProviderTracker.class);
        final ResourceProviderStorage storage = Mockito.mock(ResourceProviderStorage.class);
        Mockito.when(tracker.getResourceProviderStorage()).thenReturn(storage);

        FactoryPreconditions conditions = new FactoryPreconditions();
        conditions.activate(null, null, null, tracker);

        assertTrue(conditions.checkPreconditions(null, null));

        conditions.deactivate();

        assertFalse(conditions.checkPreconditions(null, null));
    }

    private List<ResourceProviderHandler> getResourceProviderHandlers(String[] pids) {
        final List<ResourceProviderHandler> result = new ArrayList<ResourceProviderHandler>();

        for(final String p : pids) {
            final ResourceProviderHandler handler = Mockito.mock(ResourceProviderHandler.class);
            final ResourceProviderInfo info = Mockito.mock(ResourceProviderInfo.class);
            final ServiceReference ref = Mockito.mock(ServiceReference.class);

            Mockito.when(handler.getInfo()).thenReturn(info);
            Mockito.when(info.getServiceReference()).thenReturn(ref);
            Mockito.when(ref.getProperty(Constants.SERVICE_PID)).thenReturn(p);

            result.add(handler);
        }
        return result;
    }

    private List<ResourceProviderHandler> getResourceProviderHandlersWithNames(String[] names) {
        final List<ResourceProviderHandler> result = new ArrayList<ResourceProviderHandler>();

        for(final String n : names) {
            final ResourceProviderHandler handler = Mockito.mock(ResourceProviderHandler.class);
            final ResourceProviderInfo info = Mockito.mock(ResourceProviderInfo.class);
            Mockito.when(info.getName()).thenReturn(n);
            final ServiceReference ref = Mockito.mock(ServiceReference.class);

            Mockito.when(handler.getInfo()).thenReturn(info);
            Mockito.when(info.getServiceReference()).thenReturn(ref);

            result.add(handler);
        }
        return result;
    }

    @Test public void testPIDs() {
        final ResourceProviderTracker tracker = Mockito.mock(ResourceProviderTracker.class);
        final ResourceProviderStorage storage = Mockito.mock(ResourceProviderStorage.class);
        Mockito.when(tracker.getResourceProviderStorage()).thenReturn(storage);

        FactoryPreconditions conditions = new FactoryPreconditions();
        conditions.activate(null, new String[] {"pid1", "pid3"}, null, tracker);

        final List<ResourceProviderHandler> handlers1 = getResourceProviderHandlers(new String[] {"pid2"});
        Mockito.when(storage.getAllHandlers()).thenReturn(handlers1);
        assertFalse(conditions.checkPreconditions(null, null));

        final List<ResourceProviderHandler> handlers2 = getResourceProviderHandlers(new String[] {"pid1", "pid2", "pid3"});
        Mockito.when(storage.getAllHandlers()).thenReturn(handlers2);
        assertTrue(conditions.checkPreconditions(null, null));

        final List<ResourceProviderHandler> handlers3 = getResourceProviderHandlers(new String[] {"pid1"});
        Mockito.when(storage.getAllHandlers()).thenReturn(handlers3);
        assertFalse(conditions.checkPreconditions(null, null));
    }

    @Test public void testNames() {
        final ResourceProviderTracker tracker = Mockito.mock(ResourceProviderTracker.class);
        final ResourceProviderStorage storage = Mockito.mock(ResourceProviderStorage.class);
        Mockito.when(tracker.getResourceProviderStorage()).thenReturn(storage);

        FactoryPreconditions conditions = new FactoryPreconditions();
        conditions.activate(null, null, new String[] {"n1", "n2"}, tracker);

        final List<ResourceProviderHandler> handlers1 = getResourceProviderHandlersWithNames(new String[] {"n2"});
        Mockito.when(storage.getAllHandlers()).thenReturn(handlers1);
        assertFalse(conditions.checkPreconditions(null, null));

        final List<ResourceProviderHandler> handlers2 = getResourceProviderHandlersWithNames(new String[] {"n1", "n2", "n3"});
        Mockito.when(storage.getAllHandlers()).thenReturn(handlers2);
        assertTrue(conditions.checkPreconditions(null, null));

        final List<ResourceProviderHandler> handlers3 = getResourceProviderHandlersWithNames(new String[] {"n1"});
        Mockito.when(storage.getAllHandlers()).thenReturn(handlers3);
        assertFalse(conditions.checkPreconditions(null, null));
    }

    @Test public void testUnregisteringService() {
        final ResourceProviderTracker tracker = Mockito.mock(ResourceProviderTracker.class);
        final ResourceProviderStorage storage = Mockito.mock(ResourceProviderStorage.class);
        Mockito.when(tracker.getResourceProviderStorage()).thenReturn(storage);

        FactoryPreconditions conditions = new FactoryPreconditions();
        conditions.activate(null, new String[] {"pid1", "pid3"}, null, tracker);

        final List<ResourceProviderHandler> handlers2 = getResourceProviderHandlers(new String[] {"pid1", "pid2", "pid3"});
        Mockito.when(storage.getAllHandlers()).thenReturn(handlers2);
        assertTrue(conditions.checkPreconditions(null, null));

        assertTrue(conditions.checkPreconditions(null, "pid2"));

        assertFalse(conditions.checkPreconditions(null, "pid1"));
    }
}
