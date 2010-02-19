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
package org.apache.sling.event.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListenerIterator;

import org.apache.sling.event.EventUtil;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(JMock.class)
public class JobEventHandlerTest extends AbstractRepositoryEventHandlerTest {

    protected Mockery context;

    public JobEventHandlerTest() {
        this.handler = new JobEventHandler();
        this.context = new JUnit4Mockery();
    }

    @Override
    protected Mockery getMockery() {
        return this.context;
    }

    @org.junit.Test public void testSetup() throws RepositoryException {
        assertEquals(this.handler.applicationId, SLING_ID);
        assertEquals(this.handler.repositoryPath, REPO_PATH);
        assertNotNull(((JobEventHandler)this.handler).backgroundSession);
        final EventListenerIterator iter = ((JobEventHandler)this.handler).backgroundSession.getWorkspace().getObservationManager().getRegisteredEventListeners();
        boolean found = false;
        while ( !found && iter.hasNext() ) {
            final javax.jcr.observation.EventListener listener = iter.nextEventListener();
            found = (listener == this.handler);
        }
        assertTrue("Handler is not registered as event listener.", found);
    }

    private Event getJobEvent() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventUtil.PROPERTY_JOB_TOPIC, "sling/test");
        props.put(EventUtil.PROPERTY_JOB_RETRY_DELAY, 2000L);
        props.put(EventUtil.PROPERTY_JOB_RETRIES, 2);
        return new Event(EventUtil.TOPIC_JOB, props);
    }

    @org.junit.Test public void testSimpleJobExecution() throws Exception {
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        jeh.handleEvent(getJobEvent());
        final Barrier cb = new Barrier(2);
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            EventUtil.finishedJob(event);
                            cb.block();
                        }

                    }
                });
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
    }

    @org.junit.Test public void testStartJobAndReschedule() throws Exception {
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        jeh.handleEvent(getJobEvent());
        final Barrier cb = new Barrier(2);
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            EventUtil.rescheduleJob(event);
                            cb.block();
                        }

                    }
                });
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        // the job is retried after two seconds, so we wait again
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        // the job is retried after two seconds, so we wait again
        assertTrue("No event received in the given time.", cb.block(5));
        // we have reached the retry so we expect to not get an event
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
    }

}
