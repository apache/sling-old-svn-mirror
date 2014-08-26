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
package org.apache.sling.event.dea.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.dea.DEAConstants;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactoryOptions;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class DistributingEventHandlerTest {

    private static final String TOPIC_PREFIX = "write/";

    private DistributedEventReceiver receiver;

    private DistributedEventSender sender;

    private static final String MY_APP_ID = "1234";

    private static final String OTHER_APP_ID = "5678";

    private final List<Event> events = Collections.synchronizedList(new ArrayList<Event>());

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.registerService(Mockito.any(String[].class),
                Mockito.any(),
                Mockito.any(Dictionary.class))).thenReturn(null);

        final SlingSettingsService otherSettings = Mockito.mock(SlingSettingsService.class);
        Mockito.when(otherSettings.getSlingId()).thenReturn(OTHER_APP_ID);

        final EventAdmin ea = new EventAdmin() {

            @Override
            public void sendEvent(final Event event) {
                this.postEvent(event);
            }

            @Override
            public void postEvent(final Event event) {
                final String topic = event.getTopic();
                if ( topic.equals(SlingConstants.TOPIC_RESOURCE_ADDED) ) {
                    sender.handleEvent(event);
                } else if ( topic.startsWith(TOPIC_PREFIX) ) {
                    events.add(event);
                }
            }
        };
        final MockResourceResolverFactoryOptions opts = new MockResourceResolverFactoryOptions();
        opts.setEventAdmin(ea);
        final ResourceResolverFactory factory = new MockResourceResolverFactory(opts);

        this.sender = new DistributedEventSender(bc, DistributedEventAdminImpl.DEFAULT_REPOSITORY_PATH,
                DistributedEventAdminImpl.DEFAULT_REPOSITORY_PATH + "/" + MY_APP_ID, factory, ea);

        this.receiver = new DistributedEventReceiver(bc, DistributedEventAdminImpl.DEFAULT_REPOSITORY_PATH,
                DistributedEventAdminImpl.DEFAULT_REPOSITORY_PATH + "/" + OTHER_APP_ID, 15, factory, otherSettings);
    }

    @After
    public void cleanup() {
        if ( this.sender != null ) {
            this.sender.stop();
            this.sender = null;
        }
        if ( this.receiver != null ) {
            this.receiver.stop();
            this.receiver = null;
        }
    }

    @org.junit.Test(timeout=5000) public void testSendEvent() throws Exception {
        this.events.clear();

        final String VALUE = "some value";
        final String topic = TOPIC_PREFIX + "event/test";
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("a property", VALUE);
        final Event e = new Event(topic, props);
        this.receiver.handleEvent(e);

        while ( this.events.size() == 0 ) {
            Thread.sleep(5);
        }
        final Event receivedEvent = this.events.get(0);

        assertEquals(topic, receivedEvent.getTopic());
        assertEquals(OTHER_APP_ID, receivedEvent.getProperty(DEAConstants.PROPERTY_APPLICATION));
        assertEquals(VALUE, receivedEvent.getProperty("a property"));
        assertNull(receivedEvent.getProperty(ResourceResolver.PROPERTY_RESOURCE_TYPE));

        this.events.clear();
    }

    @org.junit.Test(timeout=5000) public void testSendEventPlusAppId() throws Exception {
        this.events.clear();

        final String VALUE = "some value";
        final String topic = TOPIC_PREFIX + "event/test";
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("a property", "some value");
        // now we check if the application id is handled correctly
        props.put(DEAConstants.PROPERTY_APPLICATION, "foo");

        final Event e = new Event(topic, props);
        this.receiver.handleEvent(e);

        while ( this.events.size() == 0 ) {
            Thread.sleep(5);
        }
        final Event receivedEvent = this.events.get(0);

        assertEquals(topic, receivedEvent.getTopic());
        assertEquals(OTHER_APP_ID, receivedEvent.getProperty(DEAConstants.PROPERTY_APPLICATION));
        assertEquals(VALUE, receivedEvent.getProperty("a property"));
        assertNull(receivedEvent.getProperty(ResourceResolver.PROPERTY_RESOURCE_TYPE));

        this.events.clear();
    }

    @org.junit.Test(timeout=5000) public void testSendEventWithResourceType() throws Exception {
        this.events.clear();

        final String topic = TOPIC_PREFIX + "event/test";
        final String RT = "my:resourceType";

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, RT);

        final Event e = new Event(topic, props);
        this.receiver.handleEvent(e);

        while ( this.events.size() == 0 ) {
            Thread.sleep(5);
        }
        final Event receivedEvent = this.events.get(0);

        assertEquals(topic, receivedEvent.getTopic());
        assertEquals(OTHER_APP_ID, receivedEvent.getProperty(DEAConstants.PROPERTY_APPLICATION));
        assertEquals(RT, receivedEvent.getProperty(ResourceResolver.PROPERTY_RESOURCE_TYPE));
        assertNull(receivedEvent.getProperty("event.dea." + ResourceResolver.PROPERTY_RESOURCE_TYPE));

        this.events.clear();
    }
}
