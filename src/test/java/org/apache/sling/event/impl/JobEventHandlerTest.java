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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

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
        ((JobEventHandler)this.handler).scheduler = new SimpleScheduler();
    }

    @Override
    protected Mockery getMockery() {
        return this.context;
    }

    @Override
    protected Dictionary<String, Object> getComponentConfig() {
        final Dictionary<String, Object> config =  super.getComponentConfig();
        config.put("cleanup.period", 1); // set clean up to 1 minute
        return config;
    }

    /**
     * Simple setup test which checks if the session and the session listener
     * is registered.
     */
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

    /**
     * Helper method to create a job event.
     */
    private Event getJobEvent(String queueName, String id, String parallel) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventUtil.PROPERTY_JOB_TOPIC, "sling/test");
        if ( id != null ) {
            props.put(EventUtil.PROPERTY_JOB_ID, id);
        }
        props.put(EventUtil.PROPERTY_JOB_RETRY_DELAY, 2000L);
        props.put(EventUtil.PROPERTY_JOB_RETRIES, 2);
        if ( queueName != null ) {
            props.put(EventUtil.PROPERTY_JOB_QUEUE_NAME, queueName);
        }
        if ( parallel != null ) {
            props.put(EventUtil.PROPERTY_JOB_PARALLEL, parallel);
        }
        return new Event(EventUtil.TOPIC_JOB, props);
    }

    /**
     * Test simple job execution.
     * The job is executed once and finished successfully.
     */
    @org.junit.Test public void testSimpleJobExecution() throws Exception {
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        final Barrier cb = new Barrier(2);
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            EventUtil.acknowledgeJob(event);
                            EventUtil.finishedJob(event);
                            cb.block();
                        }

                    }
                });
        jeh.handleEvent(getJobEvent(null, null, null));
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
    }

    /**
     * Test simple job execution with job id.
     * The job is executed once and finished successfully.
     */
    @org.junit.Test public void testSimpleJobWithIdExecution() throws Exception {
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        final Barrier cb = new Barrier(2);
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            EventUtil.acknowledgeJob(event);
                            EventUtil.finishedJob(event);
                            cb.block();
                        }

                    }
                });
        jeh.handleEvent(getJobEvent(null, "myid", null));
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
    }

    /**
     * Test cancelling a job
     * The job execution always fails
     */
    @org.junit.Test public void testCancelJob() throws Exception {
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        final Barrier cb = new Barrier(2);
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            EventUtil.acknowledgeJob(event);
                            cb.block();
                            try {
                                Thread.sleep(400);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                            EventUtil.rescheduleJob(event);
                        }

                    }
                });
        jeh.handleEvent(getJobEvent(null, "myid", null));
        // sleep a little to give the job handler time write the job
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // ignore
        }

        assertEquals(1, jeh.getAllJobs("sling/test").size());
        cb.block();
        // job is currently sleeping, therefore cancel fails
        assertFalse(jeh.removeJob("sling/test", "myid"));
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            // ignore
        }
        // the job is now in the queue again
        assertTrue(jeh.removeJob("sling/test", "myid"));
        assertEquals(0, jeh.getAllJobs("sling/test").size());
    }

    /**
     * Test force cancelling a job
     * The job execution always fails
     */
    @org.junit.Test public void testForceCancelJob() throws Exception {
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        final Barrier cb = new Barrier(2);
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            EventUtil.acknowledgeJob(event);
                            cb.block();
                            try {
                                Thread.sleep(400);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                            EventUtil.rescheduleJob(event);
                        }

                    }
                });
        jeh.handleEvent(getJobEvent(null, "myid", null));
        // sleep a little to give the job handler time write the job
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // ignore
        }
        assertEquals(1, jeh.getAllJobs("sling/test").size());
        cb.block();
        // job is currently sleeping, but force cancel always waits!
        jeh.forceRemoveJob("sling/test", "myid");
        // the job is now removed
        assertEquals(0, jeh.getAllJobs("sling/test").size());
    }

    /**
     * Reschedule test.
     * The job is rescheduled two times before it fails.
     */
    @org.junit.Test public void testStartJobAndReschedule() throws Exception {
        final List<Integer> retryCountList = new ArrayList<Integer>();
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        final Barrier cb = new Barrier(2);
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        int retryCount;
                        public void handleEvent(Event event) {
                            EventUtil.acknowledgeJob(event);
                            int retry = 0;
                            if ( event.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                                retry = (Integer)event.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT);
                            }
                            if ( retry == retryCount ) {
                                retryCountList.add(retry);
                            }
                            retryCount++;
                            EventUtil.rescheduleJob(event);
                            cb.block();
                        }
                    }
                });
        jeh.handleEvent(getJobEvent(null, null, null));
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
        assertEquals("Unexpected number of retries", 3, retryCountList.size());
    }

    /**
     * Reschedule test.
     * The job is rescheduled two times before it fails.
     */
    @org.junit.Test public void testStartJobAndRescheduleInJobQueue() throws Exception {
        final List<Integer> retryCountList = new ArrayList<Integer>();
        final Barrier cb = new Barrier(2);
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        int retryCount;
                        public void handleEvent(Event event) {
                            EventUtil.acknowledgeJob(event);
                            int retry = 0;
                            if ( event.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                                retry = (Integer)event.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT);
                            }
                            if ( retry == retryCount ) {
                                retryCountList.add(retry);
                            }
                            retryCount++;
                            EventUtil.rescheduleJob(event);
                            cb.block();
                        }
                    }
                });
        jeh.handleEvent(getJobEvent("testqueue", null, null));
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
        assertEquals("Unexpected number of retries", 3, retryCountList.size());
    }

    /**
     * Notifications.
     * We send several jobs which are treated different and then see
     * how many invocations have been sent.
     */
    @org.junit.Test public void testNotifications() throws Exception {
        final List<String> cancelled = Collections.synchronizedList(new ArrayList<String>());
        final List<String> failed = Collections.synchronizedList(new ArrayList<String>());
        final List<String> finished = Collections.synchronizedList(new ArrayList<String>());
        final List<String> started = Collections.synchronizedList(new ArrayList<String>());
        final JobEventHandler jeh = (JobEventHandler)this.handler;
        jeh.eventAdmin = new SimpleEventAdmin(new String[] {"sling/test",
                EventUtil.TOPIC_JOB_CANCELLED,
                EventUtil.TOPIC_JOB_FAILED,
                EventUtil.TOPIC_JOB_FINISHED,
                EventUtil.TOPIC_JOB_STARTED},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            EventUtil.acknowledgeJob(event);
                            // events 1 and 4 finish the first time
                            final String id = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
                            if ( "1".equals(id) || "4".equals(id) ) {
                                EventUtil.finishedJob(event);
                            } else
                            // 5 fails always
                            if ( "5".equals(id) ) {
                                EventUtil.rescheduleJob(event);
                            }
                            int retry = 0;
                            if ( event.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                                retry = (Integer)event.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT);
                            }
                            // 2 fails the first time
                            if ( "2".equals(id) ) {
                                if ( retry == 0 ) {
                                    EventUtil.rescheduleJob(event);
                                } else {
                                    EventUtil.finishedJob(event);
                                }
                            }
                            // 3 fails the first and second time
                            if ( "3".equals(id) ) {
                                if ( retry == 0 || retry == 1 ) {
                                    EventUtil.rescheduleJob(event);
                                } else {
                                    EventUtil.finishedJob(event);
                                }
                            }
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            final Event job = (Event) event.getProperty(EventUtil.PROPERTY_NOTIFICATION_JOB);
                            final String id = (String)job.getProperty(EventUtil.PROPERTY_JOB_ID);
                            cancelled.add(id);
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            final Event job = (Event) event.getProperty(EventUtil.PROPERTY_NOTIFICATION_JOB);
                            final String id = (String)job.getProperty(EventUtil.PROPERTY_JOB_ID);
                            failed.add(id);
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            final Event job = (Event) event.getProperty(EventUtil.PROPERTY_NOTIFICATION_JOB);
                            final String id = (String)job.getProperty(EventUtil.PROPERTY_JOB_ID);
                            finished.add(id);
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            final Event job = (Event) event.getProperty(EventUtil.PROPERTY_NOTIFICATION_JOB);
                            final String id = (String)job.getProperty(EventUtil.PROPERTY_JOB_ID);
                            started.add(id);
                        }
                    }
                });
        jeh.handleEvent(getJobEvent(null, "1", "true"));
        jeh.handleEvent(getJobEvent(null, "2", "true"));
        jeh.handleEvent(getJobEvent(null, "3", "true"));
        jeh.handleEvent(getJobEvent(null, "4", "true"));
        jeh.handleEvent(getJobEvent(null, "5", "true"));
        int count = 0;
        final long startTime = System.currentTimeMillis();
        do {
            count = finished.size() + cancelled.size();
            // after 25 seconds we cancel the test
            if ( System.currentTimeMillis() - startTime > 25000 ) {
                throw new Exception("Timeout during notification test.");
            }
        } while ( count < 5);
        assertEquals("Finished count", 4, finished.size());
        assertEquals("Cancelled count", 1, cancelled.size());
        assertEquals("Started count", 10, started.size());
        assertEquals("Failed count", 5, failed.size());
    }

    @org.junit.Test public void testCleanup() throws Exception {
        final Calendar obsolete = Calendar.getInstance();
        obsolete.add(Calendar.MINUTE, -10);
        handler.writeEvent(new Event("test", (Dictionary<String, Object>)null), "1").setProperty(EventHelper.NODE_PROPERTY_FINISHED, obsolete);
        handler.writeEvent(new Event("test", (Dictionary<String, Object>)null), "2").setProperty(EventHelper.NODE_PROPERTY_FINISHED, obsolete);
        handler.writeEvent(new Event("test", (Dictionary<String, Object>)null), "3").setProperty(EventHelper.NODE_PROPERTY_FINISHED, obsolete);
        handler.writeEvent(new Event("test", (Dictionary<String, Object>)null), "4").setProperty(EventHelper.NODE_PROPERTY_FINISHED, obsolete);

        final Calendar future = Calendar.getInstance();
        future.add(Calendar.MINUTE, +10);
        handler.writeEvent(new Event("test", (Dictionary<String, Object>)null), "5").setProperty(EventHelper.NODE_PROPERTY_FINISHED, future);
        handler.writeEvent(new Event("test", (Dictionary<String, Object>)null), "6").setProperty(EventHelper.NODE_PROPERTY_FINISHED, future);
        handler.writeEvent(new Event("test", (Dictionary<String, Object>)null), "7");
        handler.writeEvent(new Event("test", (Dictionary<String, Object>)null), "8");

        handler.writerSession.save();
        assertTrue(handler.getWriterRootNode().hasNode("1"));
        assertEquals(obsolete, handler.getWriterRootNode().getNode("1").getProperty(EventHelper.NODE_PROPERTY_FINISHED).getDate());
        assertTrue(handler.getWriterRootNode().hasNode("2"));
        assertTrue(handler.getWriterRootNode().hasNode("3"));
        assertTrue(handler.getWriterRootNode().hasNode("4"));
        assertTrue(handler.getWriterRootNode().hasNode("5"));
        assertTrue(handler.getWriterRootNode().hasNode("6"));
        assertTrue(handler.getWriterRootNode().hasNode("7"));
        assertTrue(handler.getWriterRootNode().hasNode("8"));

        ((JobEventHandler)handler).run();

        assertFalse(handler.getWriterRootNode().hasNode("1"));
        assertFalse(handler.getWriterRootNode().hasNode("2"));
        assertFalse(handler.getWriterRootNode().hasNode("3"));
        assertFalse(handler.getWriterRootNode().hasNode("4"));
        assertTrue(handler.getWriterRootNode().hasNode("5"));
        assertTrue(handler.getWriterRootNode().hasNode("6"));
        assertTrue(handler.getWriterRootNode().hasNode("7"));
        assertTrue(handler.getWriterRootNode().hasNode("8"));
    }
}
