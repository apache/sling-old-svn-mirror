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
package org.apache.sling.discovery.impl.standalone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Test;
import org.osgi.framework.Constants;

public class NoClusterDiscoveryServiceTest {

    private void invoke(final Object obj, final String methodName) {
        invoke(obj, methodName, null, null);
    }

    private void invoke(final Object obj, final String methodName, final Class[] params, final Object[] args) {
        try {
            final Method activate = obj.getClass().getDeclaredMethod(methodName, params);
            activate.setAccessible(true);
            activate.invoke(obj, args);
        } catch (final Exception e) {
            throw new RuntimeException("Unable to invoke method " + methodName + " on " + obj, e);
        }
    }

    private Object setField(final Object obj, final String fieldName, final Object value) {
        Class<?> clazz = obj.getClass();
        while ( clazz != null ) {
            try {
                final Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);

                field.set(obj, value);
                return null;
            } catch ( final Exception ignore ) {
                // ignore
            }
            clazz = clazz.getSuperclass();
        }
        throw new RuntimeException("Field " + fieldName + " not found on object " + obj);
    }

    private DiscoveryService createService(final boolean activate) {
        final DiscoveryService service = new NoClusterDiscoveryService();

        setField(service, "settingsService", new SlingSettingsService() {

            @Override
            public String getSlingId() {
                return "my-sling-id";
            }

            @Override
            public String getSlingHomePath() {
                return null;
            }

            @Override
            public URL getSlingHome() {
                return null;
            }

            @Override
            public Set<String> getRunModes() {
                return null;
            }

            @Override
            public String getAbsolutePathWithinSlingHome(String relativePath) {
                return null;
            }
        });
        if ( activate ) {
            invoke(service, "activate");
        }

        return service;
    }

    @Test public void testBasics() throws Exception {
        final DiscoveryService service = this.createService(true);

        assertNotNull(service.getTopology());
        assertTrue(service.getTopology().isCurrent());

        invoke(service, "deactivate");

        assertNull(service.getTopology());
    }

    @Test public void testListenerAfter() throws Exception {
        final DiscoveryService service = this.createService(true);

        final List<TopologyEvent> events = new ArrayList<TopologyEvent>();

        final TopologyEventListener listener = new TopologyEventListener() {

            @Override
            public void handleTopologyEvent(final TopologyEvent event) {
                events.add(event);
            }
        };
        invoke(service, "bindTopologyEventListener", new Class[] {TopologyEventListener.class}, new Object[] {listener});
        assertEquals(1, events.size());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, events.get(0).getType());
        assertNotNull(events.get(0).getNewView());
        assertNull(events.get(0).getOldView());
    }

    @Test public void testListenerBefore() throws Exception {
        final DiscoveryService service = this.createService(false);

        final List<TopologyEvent> events = new ArrayList<TopologyEvent>();

        final TopologyEventListener listener = new TopologyEventListener() {

            @Override
            public void handleTopologyEvent(final TopologyEvent event) {
                events.add(event);
            }
        };
        invoke(service, "bindTopologyEventListener", new Class[] {TopologyEventListener.class}, new Object[] {listener});
        assertEquals(0, events.size());

        invoke(service, "activate");
        assertEquals(1, events.size());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, events.get(0).getType());
        assertNotNull(events.get(0).getNewView());
        assertNull(events.get(0).getOldView());
    }

    @Test public void testPropertyChanges() throws Exception {
        final DiscoveryService service = this.createService(true);

        final List<TopologyEvent> events = new ArrayList<TopologyEvent>();

        final TopologyEventListener listener = new TopologyEventListener() {

            @Override
            public void handleTopologyEvent(final TopologyEvent event) {
                events.add(event);
            }
        };
        invoke(service, "bindTopologyEventListener", new Class[] {TopologyEventListener.class}, new Object[] {listener});
        events.clear();

        final PropertyProvider provider = new PropertyProvider() {

            @Override
            public String getProperty(final String name) {
                if ( "a".equals(name) ) {
                    return "1";
                }
                if ( "b".equals(name) ) {
                    return "2";
                }
                if ( "c".equals(name) ) {
                    return "3";
                }
                return null;
            }
        };
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyProvider.PROPERTY_PROPERTIES, new String[] {"a", "b", "c"});
        properties.put(Constants.SERVICE_ID, 1L);

        invoke(service, "bindPropertyProvider", new Class[] {PropertyProvider.class, Map.class}, new Object[] {provider, properties});

        assertEquals(1, events.size());
        assertEquals(TopologyEvent.Type.PROPERTIES_CHANGED, events.get(0).getType());
        assertNotNull(events.get(0).getNewView());
        assertTrue(events.get(0).getNewView().isCurrent());
        assertNotNull(events.get(0).getOldView());
        assertFalse(events.get(0).getOldView().isCurrent());

        // test properties
        assertEquals("1", events.get(0).getNewView().getLocalInstance().getProperty("a"));
        assertEquals("2", events.get(0).getNewView().getLocalInstance().getProperty("b"));
        assertEquals("3", events.get(0).getNewView().getLocalInstance().getProperty("c"));
        assertNull(events.get(0).getOldView().getLocalInstance().getProperty("a"));
        assertNull(events.get(0).getOldView().getLocalInstance().getProperty("b"));
        assertNull(events.get(0).getOldView().getLocalInstance().getProperty("c"));

        events.clear();
        invoke(service, "unbindPropertyProvider", new Class[] {PropertyProvider.class, Map.class}, new Object[] {provider, properties});
        assertEquals(1, events.size());
        assertEquals(TopologyEvent.Type.PROPERTIES_CHANGED, events.get(0).getType());
        assertNotNull(events.get(0).getNewView());
        assertTrue(events.get(0).getNewView().isCurrent());
        assertNotNull(events.get(0).getOldView());
        assertFalse(events.get(0).getOldView().isCurrent());

        assertEquals("1", events.get(0).getOldView().getLocalInstance().getProperty("a"));
        assertEquals("2", events.get(0).getOldView().getLocalInstance().getProperty("b"));
        assertEquals("3", events.get(0).getOldView().getLocalInstance().getProperty("c"));
        assertNull(events.get(0).getNewView().getLocalInstance().getProperty("a"));
        assertNull(events.get(0).getNewView().getLocalInstance().getProperty("b"));
        assertNull(events.get(0).getNewView().getLocalInstance().getProperty("c"));
    }
}
