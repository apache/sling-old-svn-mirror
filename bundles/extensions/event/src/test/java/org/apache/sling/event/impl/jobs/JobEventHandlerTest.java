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
package org.apache.sling.event.impl.jobs;

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
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.observation.EventListenerIterator;

import junitx.util.PrivateAccessor;

import org.apache.sling.event.impl.Barrier;
import org.apache.sling.event.impl.RepositoryTestUtil;
import org.apache.sling.event.impl.SimpleEventAdmin;
import org.apache.sling.event.impl.jobs.jcr.JCRHelper;
import org.apache.sling.event.impl.jobs.jcr.PersistenceHandler;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.JobManager.QueryType;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.JobsIterator;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

@RunWith(JMock.class)
public class JobEventHandlerTest extends AbstractJobEventHandlerTest {

    protected Mockery context;

    public JobEventHandlerTest() {
        this.context = new JUnit4Mockery();
    }

    @Override
    protected Mockery getMockery() {
        return this.context;
    }

    @Override
    protected Hashtable<String, Object> getComponentConfig() {
        final Hashtable<String, Object> config =  super.getComponentConfig();
        config.put("cleanup.period", 1); // set clean up to 1 minute
        config.put("load.delay", 1); // load delay to 1 sec
        return config;
    }

    /**
     * Simple setup test which checks if the session and the session listener
     * is registered.
     */
    @org.junit.Test public void testSetup() throws Exception {
        assertEquals(Environment.APPLICATION_ID, SLING_ID);
        assertEquals(PrivateAccessor.getField(this.handler, "repositoryPath"), REPO_PATH);
        assertNotNull(PrivateAccessor.getField(this.handler, "backgroundSession"));
        final EventListenerIterator iter = ((Session)PrivateAccessor.getField(this.handler, "backgroundSession")).getWorkspace().getObservationManager().getRegisteredEventListeners();
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
        return getJobEvent(queueName, id, parallel, false);
    }

    /**
     * Helper method to create a job event.
     */
    private Event getJobEvent(String queueName, String id, String parallel, boolean runlocal) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(JobUtil.PROPERTY_JOB_TOPIC, "sling/test");
        if ( id != null ) {
            props.put(JobUtil.PROPERTY_JOB_NAME, id);
        }
        props.put(JobUtil.PROPERTY_JOB_RETRY_DELAY, 2000L);
        props.put(JobUtil.PROPERTY_JOB_RETRIES, 2);
        if ( queueName != null ) {
            props.put(JobUtil.PROPERTY_JOB_QUEUE_NAME, queueName);
        }
        if ( parallel != null ) {
            props.put(JobUtil.PROPERTY_JOB_PARALLEL, parallel);
        }
        if ( runlocal ) {
            props.put(JobUtil.PROPERTY_JOB_RUN_LOCAL, "true");
        }
        return new Event(JobUtil.TOPIC_JOB, props);
    }

    /**
     * Test simple job execution.
     * The job is executed once and finished successfully.
     */
    @org.junit.Test public void testSimpleJobExecution() throws Exception {
        final PersistenceHandler jeh = this.handler;
        final Barrier cb = new Barrier(2);
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            JobUtil.acknowledgeJob(event);
                            JobUtil.finishedJob(event);
                            cb.block();
                        }

                    }
                }));
        jeh.handleEvent(getJobEvent(null, null, null));
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
    }

    private long getSize(final JobsIterator i) {
        long size = i.getSize();
        if ( size == - 1 ) {
            size = 0;
            while ( i.hasNext() ) {
                i.next();
                size++;
            }
        }
        return size;
    }
    /**
     * Test simple job execution with job id.
     * The job is executed once and finished successfully.
     */
    @org.junit.Test public void testSimpleJobWithIdExecution() throws Exception {
        final PersistenceHandler jeh = this.handler;
        final Barrier cb = new Barrier(2);
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            JobUtil.acknowledgeJob(event);
                            JobUtil.finishedJob(event);
                            cb.block();
                        }

                    }
                }));
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
        final PersistenceHandler jeh = this.handler;
        final Barrier cb = new Barrier(2);
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            JobUtil.acknowledgeJob(event);
                            cb.block();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                            JobUtil.rescheduleJob(event);
                        }

                    }
                }));
        jeh.handleEvent(getJobEvent(null, "myid", null));
        cb.block();
        assertEquals(1, this.getSize(this.jobManager.queryJobs(QueryType.ALL, "sling/test")));
        // job is currently sleeping, therefore cancel fails
        final Event e1 = this.jobManager.findJob("sling/test", Collections.singletonMap(JobUtil.PROPERTY_JOB_NAME, (Object)"myid"));
        assertNotNull(e1);
        assertFalse(this.jobManager.removeJob((String)e1.getProperty(JobUtil.JOB_ID)));
        try {
            Thread.sleep(900);
        } catch (InterruptedException e) {
            // ignore
        }
        // the job is now in the queue again
        final Event e2 = this.jobManager.findJob("sling/test", Collections.singletonMap(JobUtil.PROPERTY_JOB_NAME, (Object)"myid"));
        assertNotNull(e2);
        assertTrue(this.jobManager.removeJob((String)e2.getProperty(JobUtil.JOB_ID)));
        assertEquals(0, this.getSize(this.jobManager.queryJobs(QueryType.ALL, "sling/test")));
    }

    /**
     * Test force cancelling a job
     * The job execution always fails
     */
    @org.junit.Test public void testForceCancelJob() throws Exception {
        final PersistenceHandler jeh = this.handler;
        final Barrier cb = new Barrier(2);
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            JobUtil.acknowledgeJob(event);
                            cb.block();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                // ignore
                            }
                            JobUtil.rescheduleJob(event);
                        }

                    }
                }));
        jeh.handleEvent(getJobEvent(null, "myid", null));
        cb.block();
        assertEquals(1, this.getSize(this.jobManager.queryJobs(QueryType.ALL, "sling/test")));
        // job is currently sleeping, but force cancel always waits!
        final Event e = this.jobManager.findJob("sling/test", Collections.singletonMap(JobUtil.PROPERTY_JOB_NAME, (Object)"myid"));
        assertNotNull(e);
        this.jobManager.forceRemoveJob((String)e.getProperty(JobUtil.JOB_ID));
        // the job is now removed
        assertEquals(0, this.getSize(this.jobManager.queryJobs(QueryType.ALL, "sling/test")));
    }

    /**
     * Reschedule test.
     * The job is rescheduled two times before it fails.
     */
    @org.junit.Test public void testStartJobAndReschedule() throws Exception {
        final List<Integer> retryCountList = new ArrayList<Integer>();
        final PersistenceHandler jeh = this.handler;
        final Barrier cb = new Barrier(2);
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        int retryCount;
                        public void handleEvent(Event event) {
                            JobUtil.acknowledgeJob(event);
                            int retry = 0;
                            if ( event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                                retry = (Integer)event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT);
                            }
                            if ( retry == retryCount ) {
                                retryCountList.add(retry);
                            }
                            retryCount++;
                            JobUtil.rescheduleJob(event);
                            cb.block();
                        }
                    }
                }));
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
        final PersistenceHandler jeh = this.handler;
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        int retryCount;
                        public void handleEvent(Event event) {
                            JobUtil.acknowledgeJob(event);
                            int retry = 0;
                            if ( event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                                retry = (Integer)event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT);
                            }
                            if ( retry == retryCount ) {
                                retryCountList.add(retry);
                            }
                            retryCount++;
                            JobUtil.rescheduleJob(event);
                            cb.block();
                        }
                    }
                }));
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
        final PersistenceHandler jeh = this.handler;
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test",
                JobUtil.TOPIC_JOB_CANCELLED,
                JobUtil.TOPIC_JOB_FAILED,
                JobUtil.TOPIC_JOB_FINISHED,
                JobUtil.TOPIC_JOB_STARTED},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            JobUtil.acknowledgeJob(event);
                            // events 1 and 4 finish the first time
                            final String id = (String)event.getProperty(JobUtil.PROPERTY_JOB_NAME);
                            if ( "1".equals(id) || "4".equals(id) ) {
                                JobUtil.finishedJob(event);
                            } else
                            // 5 fails always
                            if ( "5".equals(id) ) {
                                JobUtil.rescheduleJob(event);
                            }
                            int retry = 0;
                            if ( event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                                retry = (Integer)event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT);
                            }
                            // 2 fails the first time
                            if ( "2".equals(id) ) {
                                if ( retry == 0 ) {
                                    JobUtil.rescheduleJob(event);
                                } else {
                                    JobUtil.finishedJob(event);
                                }
                            }
                            // 3 fails the first and second time
                            if ( "3".equals(id) ) {
                                if ( retry == 0 || retry == 1 ) {
                                    JobUtil.rescheduleJob(event);
                                } else {
                                    JobUtil.finishedJob(event);
                                }
                            }
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            final Event job = (Event) event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
                            final String id = (String)job.getProperty(JobUtil.PROPERTY_JOB_NAME);
                            cancelled.add(id);
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            final Event job = (Event) event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
                            final String id = (String)job.getProperty(JobUtil.PROPERTY_JOB_NAME);
                            failed.add(id);
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            final Event job = (Event) event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
                            final String id = (String)job.getProperty(JobUtil.PROPERTY_JOB_NAME);
                            finished.add(id);
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            final Event job = (Event) event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
                            final String id = (String)job.getProperty(JobUtil.PROPERTY_JOB_NAME);
                            started.add(id);
                        }
                    }
                }));
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
        } while ( count < 5 || started.size() < 10 );
        assertEquals("Finished count", 4, finished.size());
        assertEquals("Cancelled count", 1, cancelled.size());
        assertEquals("Started count", 10, started.size());
        assertEquals("Failed count", 5, failed.size());
    }

    private void writeEvent(final Calendar finished, final String name) throws Exception {
        final Node eventNode = RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).addNode(name, JCRHelper.JOB_NODE_TYPE);
        eventNode.setProperty(JCRHelper.NODE_PROPERTY_TOPIC, "test");
        eventNode.setProperty(JobUtil.PROPERTY_JOB_TOPIC, "test");
        eventNode.setProperty(JCRHelper.NODE_PROPERTY_CREATED, Calendar.getInstance());
        eventNode.setProperty(JCRHelper.NODE_PROPERTY_APPLICATION, Environment.APPLICATION_ID);
        if ( finished != null ) {
            eventNode.setProperty(JCRHelper.NODE_PROPERTY_FINISHED, finished);
        }
    }

    @org.junit.Test public void testCleanup() throws Exception {
        final Calendar obsolete = Calendar.getInstance();
        obsolete.add(Calendar.MINUTE, -10);

        writeEvent(obsolete, "1");
        writeEvent(obsolete, "2");
        writeEvent(obsolete, "3");
        writeEvent(obsolete, "4");

        final Calendar future = Calendar.getInstance();
        future.add(Calendar.MINUTE, +10);

        writeEvent(future, "5");
        writeEvent(future, "6");
        writeEvent(null, "7");
        writeEvent(null, "8");

        RepositoryTestUtil.getAdminSession().save();
        assertEquals(obsolete, RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).getNode("1").getProperty(JCRHelper.NODE_PROPERTY_FINISHED).getDate());
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("2"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("3"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("4"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("5"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("6"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("7"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("8"));

        handler.run();

        assertFalse(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("1"));
        assertFalse(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("2"));
        assertFalse(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("3"));
        assertFalse(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("4"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("5"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("6"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("7"));
        assertTrue(RepositoryTestUtil.getAdminSession().getNode(REPO_PATH).hasNode("8"));
    }

    @org.junit.Test public void testLoad() throws Throwable {
        final List<Integer> retryCountList = new ArrayList<Integer>();
        final PersistenceHandler jeh = this.handler;
        final Barrier cb = new Barrier(2);
        final EventAdmin ea = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        int retryCount;
                        public void handleEvent(Event event) {
                            retryCountList.add(retryCount);
                            JobUtil.acknowledgeJob(event);
                            if ( retryCount == 0 ) {
                                JobUtil.rescheduleJob(event);
                            } else {
                                JobUtil.finishedJob(event);
                            }
                            retryCount++;
                            cb.block();
                        }
                    }
                });
        setEventAdmin(ea);
        jeh.handleEvent(getJobEvent(null, null, null));
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        this.deactivate();
        assertEquals("Unexpected number of retries", 1, retryCountList.size());
        Thread.sleep(3000);
        assertEquals("Unexpected number of retries", 1, retryCountList.size());
        this.activate(ea);
        // the job is retried after loading, so we wait again
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
        assertEquals("Unexpected number of retries", 2, retryCountList.size());
    }

    @org.junit.Test public void testRunLocal() throws Throwable {
        final List<Integer> retryCountList = new ArrayList<Integer>();
        final List<String> sessionPath = new ArrayList<String>();
        PersistenceHandler jeh = this.handler;
        final Barrier cb = new Barrier(2);
        final EventAdmin ea = new SimpleEventAdmin(new String[] {"sling/test"},
                new EventHandler[] {
                    new EventHandler() {
                        int retryCount;
                        public void handleEvent(Event event) {
                            retryCountList.add(retryCount);
                            JobUtil.acknowledgeJob(event);
                            if ( retryCount == 0 || retryCount == 1) {
                                // get the job node from the context
                                sessionPath.add((String)event.getProperty(JobUtil.JOB_ID));
                                JobUtil.rescheduleJob(event);
                            } else {
                                JobUtil.finishedJob(event);
                            }
                            retryCount++;
                            cb.block();
                        }
                    }
                });
        setEventAdmin(ea);
        // first test: local event and we change the application id
        jeh.handleEvent(getJobEvent(null, null, null, true));
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        this.deactivate();
        assertEquals("Unexpected number of retries", 1, retryCountList.size());
        Thread.sleep(3000);
        assertEquals("Unexpected number of retries", 1, retryCountList.size());
        assertEquals("Unexpected number of paths", 1, sessionPath.size());
        // change app id
        final String nodePath = REPO_PATH + '/' + sessionPath.get(0);
        session.getNode(nodePath).setProperty(JCRHelper.NODE_PROPERTY_APPLICATION, "unknown");
        session.save();

        this.activate(ea);
        jeh = this.handler;
        // the job is not retried after loading, so we wait again
        assertFalse("Unexpected event received in the given time.", cb.block(5));
        cb.reset();
        assertEquals("Unexpected number of retries", 1, retryCountList.size());

        // second test: local event and we don't change the application id
        jeh.handleEvent(getJobEvent(null, null, null, true));
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        this.deactivate();
        assertEquals("Unexpected number of retries", 2, retryCountList.size());
        Thread.sleep(3000);
        assertEquals("Unexpected number of retries", 2, retryCountList.size());
        assertEquals("Unexpected number of paths", 2, sessionPath.size());

        this.activate(ea);
        // the job is retried after loading, so we wait again
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
        cb.reset();
        assertEquals("Unexpected number of retries", 3, retryCountList.size());
    }

    @org.junit.Test public void testManyJobs() throws Exception {
        final PersistenceHandler jeh = this.handler;
        final AtomicInteger count = new AtomicInteger(0);
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test",
                JobUtil.TOPIC_JOB_FINISHED},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            JobUtil.processJob(event, new JobProcessor() {

                                public boolean process(Event job) {
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException ie) {
                                        // ignore
                                    }
                                    return true;
                                }
                            });
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            count.incrementAndGet();
                        }
                    }}));
        // we start "some" jobs
        final int COUNT = 300;
        for(int i = 0; i < COUNT; i++ ) {
            final String queueName = "queue" + (i % 20);
            jeh.handleEvent(getJobEvent(queueName, null, "2"));
        }
        while ( count.get() < COUNT ) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
        assertEquals("Finished count", COUNT, count.get());
        assertEquals("Finished count", COUNT, this.jobManager.getStatistics().getNumberOfFinishedJobs());
    }

    /**
     * Test sending of jobs with and without a processor
     */
    @org.junit.Test(timeout=1000*60*4) public void testNoJobProcessor() throws Exception {
        final PersistenceHandler jeh = this.handler;
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger unprocessedCount = new AtomicInteger(0);
        setEventAdmin(new SimpleEventAdmin(new String[] {"sling/test",
                "sling/test2",
                JobUtil.TOPIC_JOB_FINISHED},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            JobUtil.processJob(event, new JobProcessor() {

                                public boolean process(Event job) {
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException ie) {
                                        // ignore
                                    }
                                    return true;
                                }
                            });
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            unprocessedCount.incrementAndGet();
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            count.incrementAndGet();
                        }
                    }}));
        // we start 20 jobs, every second job has no processor
        final long startTime = System.currentTimeMillis();
        final int COUNT = 20;
        for(int i = 0; i < COUNT; i++ ) {
            final String jobTopic = (i % 2 == 0 ? "sling/test" : "sling/test2");
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(JobUtil.PROPERTY_JOB_TOPIC, jobTopic);
            jeh.handleEvent(new Event(JobUtil.TOPIC_JOB, props));
        }
        while ( count.get() < COUNT / 2) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
        while ( unprocessedCount.get() < COUNT / 2) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
        // clean up waits for one minute, so we should do the same
        while ( System.currentTimeMillis() - startTime < 61000 ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
        this.jobManager.run();
        while ( unprocessedCount.get() < COUNT ) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
        assertEquals("Finished count", COUNT / 2, count.get());
        assertEquals("Unprocessed count",COUNT, unprocessedCount.get());
        assertEquals("Finished count", COUNT / 2, this.jobManager.getStatistics().getNumberOfFinishedJobs());
    }
}
