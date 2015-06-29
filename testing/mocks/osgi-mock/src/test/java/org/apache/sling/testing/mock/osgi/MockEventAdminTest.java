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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class MockEventAdminTest {
    
    private static final String TOPIC_SAMPLE_1 = "sample/topic1";
    private static final String TOPIC_SAMPLE_2 = "sample/topic2";
    private static final String TOPIC_SAMPLE_ALL = "sample/*";
    private static final String TOPIC_OTHER_3 = "other/topic3";
    
    private static final Event EVENT_SAMPLE_1 = new Event(TOPIC_SAMPLE_1, (Dictionary<String,?>)null);
    private static final Event EVENT_SAMPLE_2 = new Event(TOPIC_SAMPLE_2, (Dictionary<String,?>)null);
    private static final Event EVENT_OTHER_3 = new Event(TOPIC_OTHER_3, (Dictionary<String,?>)null);
    
    @Rule
    public OsgiContext context = new OsgiContext();
    
    private DummyEventHandler eventHandler1;
    private DummyEventHandler eventHandler12;
    private DummyEventHandler eventHandlerSampleAll;
    private DummyEventHandler eventHandlerAll;
    
    @Before
    public void setUp() {
        eventHandler1 = (DummyEventHandler)context.registerService(EventHandler.class, new DummyEventHandler(),
                ImmutableMap.<String, Object>of(EventConstants.EVENT_TOPIC, TOPIC_SAMPLE_1));
        eventHandler12 = (DummyEventHandler)context.registerService(EventHandler.class, new DummyEventHandler(),
                ImmutableMap.<String, Object>of(EventConstants.EVENT_TOPIC, new String[] { TOPIC_SAMPLE_1, TOPIC_SAMPLE_2 }));
        eventHandlerSampleAll = (DummyEventHandler)context.registerService(EventHandler.class, new DummyEventHandler(),
                ImmutableMap.<String, Object>of(EventConstants.EVENT_TOPIC, TOPIC_SAMPLE_ALL));
        eventHandlerAll = (DummyEventHandler)context.registerService(EventHandler.class, new DummyEventHandler());
    }
    
    @Test
    public void testSendEvent_Sample1() {
        EventAdmin eventAdmin = context.getService(EventAdmin.class);
        eventAdmin.sendEvent(EVENT_SAMPLE_1);
        
        assertEquals(ImmutableList.of(EVENT_SAMPLE_1), eventHandler1.getReceivedEvents());
        assertEquals(ImmutableList.of(EVENT_SAMPLE_1), eventHandler12.getReceivedEvents());
        assertEquals(ImmutableList.of(EVENT_SAMPLE_1), eventHandlerSampleAll.getReceivedEvents());
        assertEquals(ImmutableList.of(EVENT_SAMPLE_1), eventHandlerAll.getReceivedEvents());
    }

    @Test
    public void testSendEvent_Sample2() {
        EventAdmin eventAdmin = context.getService(EventAdmin.class);
        eventAdmin.sendEvent(EVENT_SAMPLE_2);
        
        assertEquals(ImmutableList.of(), eventHandler1.getReceivedEvents());
        assertEquals(ImmutableList.of(EVENT_SAMPLE_2), eventHandler12.getReceivedEvents());
        assertEquals(ImmutableList.of(EVENT_SAMPLE_2), eventHandlerSampleAll.getReceivedEvents());
        assertEquals(ImmutableList.of(EVENT_SAMPLE_2), eventHandlerAll.getReceivedEvents());
    }

    @Test
    public void testSendEvent_Other3() {
        EventAdmin eventAdmin = context.getService(EventAdmin.class);
        eventAdmin.sendEvent(EVENT_OTHER_3);
        
        assertEquals(ImmutableList.of(), eventHandler1.getReceivedEvents());
        assertEquals(ImmutableList.of(), eventHandler12.getReceivedEvents());
        assertEquals(ImmutableList.of(), eventHandlerSampleAll.getReceivedEvents());
        assertEquals(ImmutableList.of(EVENT_OTHER_3), eventHandlerAll.getReceivedEvents());
    }

    @Test(timeout = 2000)
    public void testPostEvents() {
        EventAdmin eventAdmin = context.getService(EventAdmin.class);
        eventAdmin.postEvent(EVENT_SAMPLE_2);
        eventAdmin.postEvent(EVENT_OTHER_3);
        
        // wait until result is as expected (with timeout)
        boolean expectedResult = false;
        while (!expectedResult) {
            expectedResult = ObjectUtils.equals(ImmutableList.of(), eventHandler1.getReceivedEvents())
                    && ObjectUtils.equals(ImmutableList.of(EVENT_SAMPLE_2), eventHandler12.getReceivedEvents())
                    && ObjectUtils.equals(ImmutableList.of(EVENT_SAMPLE_2), eventHandlerSampleAll.getReceivedEvents())
                    && ObjectUtils.equals(ImmutableList.of(EVENT_SAMPLE_2, EVENT_OTHER_3), eventHandlerAll.getReceivedEvents());
        }
    }

    private static class DummyEventHandler implements EventHandler {
        
        private final List<Event> receivedEvents = new ArrayList<Event>();

        @Override
        public void handleEvent(Event event) {
            receivedEvents.add(event);
        }

        public List<Event> getReceivedEvents() {
            return ImmutableList.copyOf(receivedEvents);
        }
        
    }

}
