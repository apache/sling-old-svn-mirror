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
package org.apache.sling.event.it;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.impl.Barrier;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JobHandlingTest extends AbstractJobHandlingTest {

    public static final String TOPIC = "sling/test";

    private String queueConfPid;

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        // create test queue
        final org.osgi.service.cm.Configuration config = this.configAdmin.createFactoryConfiguration("org.apache.sling.event.jobs.QueueConfiguration", null);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ConfigurationConstants.PROP_NAME, "test");
        props.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.UNORDERED.name());
        props.put(ConfigurationConstants.PROP_TOPICS, new String[] {TOPIC, TOPIC + "2"});
        props.put(ConfigurationConstants.PROP_RETRIES, 2);
        props.put(ConfigurationConstants.PROP_RETRY_DELAY, 2000L);
        config.update(props);

        this.queueConfPid = config.getPid();
        this.sleep(1000L);
    }

    @After
    public void cleanUp() throws IOException {
        this.removeConfiguration(this.queueConfPid);
        super.cleanup();
    }

    /**
     * Helper method to create a job event.
     */
    private Event getJobEvent(String id) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ResourceHelper.PROPERTY_JOB_TOPIC, "sling/test");
        if ( id != null ) {
            props.put(ResourceHelper.PROPERTY_JOB_NAME, id);
        }

        return new Event(JobUtil.TOPIC_JOB, props);
    }

    /**
     * Test simple job execution.
     * The job is executed once and finished successfully.
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSimpleJobExecution() throws Exception {
        final Barrier cb = new Barrier(2);

        final ServiceRegistration reg = this.registerEventHandler("sling/test",
                new EventHandler() {
                    @Override
                    public void handleEvent(Event event) {
                        JobUtil.acknowledgeJob(event);
                        JobUtil.finishedJob(event);
                        cb.block();
                    }

                 });

        try {
            this.eventAdmin.sendEvent(getJobEvent(null));
            assertTrue("No event received in the given time.", cb.block(5));
            cb.reset();
            assertFalse("Unexpected event received in the given time.", cb.block(5));
        } finally {
            reg.unregister();
        }
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testManyJobs() throws Exception {
        final ServiceRegistration reg1 = this.registerEventHandler("sling/test",
                new EventHandler() {
                    @Override
                    public void handleEvent(Event event) {
                        JobUtil.processJob(event, new JobProcessor() {

                            @Override
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
                 });
        final AtomicInteger count = new AtomicInteger(0);
        final ServiceRegistration reg2 = this.registerEventHandler(JobUtil.TOPIC_JOB_FINISHED,
                new EventHandler() {
                    @Override
                    public void handleEvent(Event event) {
                        count.incrementAndGet();
                    }
                 });

        try {
            // we start "some" jobs
            final int COUNT = 300;
            for(int i = 0; i < COUNT; i++ ) {
//                final String queueName = "queue" + (i % 20);
                this.eventAdmin.sendEvent(this.getJobEvent(null));
            }
            while ( count.get() < COUNT ) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            assertEquals("Finished count", COUNT, count.get());
            assertEquals("Finished count", COUNT, this.getJobManager().getStatistics().getNumberOfFinishedJobs());
        } finally {
            reg1.unregister();
            reg2.unregister();
        }
    }

    /**
     * Test simple job execution with job id.
     * The job is executed once and finished successfully.
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSimpleJobWithIdExecution() throws Exception {
        final Barrier cb = new Barrier(2);
        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        cb.block();
                        return JobResult.OK;
                    }
                });
        try {
            final JobManager jobManager = this.getJobManager();
            jobManager.addJob(TOPIC, "myid1", null);
            assertTrue("No event received in the given time.", cb.block(5));
            cb.reset();
            assertFalse("Unexpected event received in the given time.", cb.block(5));
        } finally {
            jcReg.unregister();
        }
    }

    /**
     * Test canceling a job
     * The job execution always fails
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testCancelJob() throws Exception {
        final Barrier cb = new Barrier(2);
        final Barrier cb2 = new Barrier(2);
        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        cb.block();
                        cb2.block();
                        return JobResult.FAILED;
                    }
                });
        try {
            final JobManager jobManager = this.getJobManager();
            jobManager.addJob(TOPIC, "myid2", null);
            cb.block();

            assertEquals(1, jobManager.findJobs(JobManager.QueryType.ALL, "sling/test", -1, (Map<String, Object>[])null).size());
            // job is currently waiting, therefore cancel fails
            final Event e1 = jobManager.findJob("sling/test", Collections.singletonMap(ResourceHelper.PROPERTY_JOB_NAME, (Object)"myid2"));
            assertNotNull(e1);
            assertFalse(jobManager.removeJob((String)e1.getProperty(ResourceHelper.PROPERTY_JOB_ID)));
            cb2.block(); // and continue job

            sleep(200);

            // the job is now in the queue again
            final Event e2 = jobManager.findJob("sling/test", Collections.singletonMap(ResourceHelper.PROPERTY_JOB_NAME, (Object)"myid2"));
            assertNotNull(e2);
            assertTrue(jobManager.removeJob((String)e2.getProperty(ResourceHelper.PROPERTY_JOB_ID)));
            assertEquals(0, jobManager.findJobs(JobManager.QueryType.ALL, "sling/test", -1, (Map<String, Object>[])null).size());
            final Collection<Job> col = jobManager.findJobs(JobManager.QueryType.HISTORY, "sling/test", -1, (Map<String, Object>[])null);
            try {
                assertEquals(1, col.size());
            } finally {
                for(final Job j : col) {
                    jobManager.removeJobById(j.getId());
                }
            }
        } finally {
            jcReg.unregister();
        }
   }

    /**
     * Test force canceling a job
     * The job execution always fails
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testForceCancelJob() throws Exception {
        final Barrier cb = new Barrier(2);
        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        cb.block();
                        sleep(1000);
                        return JobResult.FAILED;
                    }
                });
        try {
            final JobManager jobManager = this.getJobManager();
            jobManager.addJob(TOPIC, "myid3", null);
            cb.block();

            assertEquals(1, jobManager.findJobs(JobManager.QueryType.ALL, "sling/test", -1, (Map<String, Object>[])null).size());
            // job is currently sleeping, but force cancel always waits!
            final Event e = jobManager.findJob("sling/test", Collections.singletonMap(ResourceHelper.PROPERTY_JOB_NAME, (Object)"myid3"));
            assertNotNull(e);
            jobManager.forceRemoveJob((String)e.getProperty(ResourceHelper.PROPERTY_JOB_ID));
            // the job is now removed
            assertEquals(0, jobManager.findJobs(JobManager.QueryType.ALL, "sling/test", -1, (Map<String, Object>[])null).size());
            final Collection<Job> col = jobManager.findJobs(JobManager.QueryType.HISTORY, "sling/test", -1, (Map<String, Object>[])null);
            try {
                assertEquals(1, col.size());
            } finally {
                for(final Job j : col) {
                    jobManager.removeJobById(j.getId());
                }
            }
        } finally {
            jcReg.unregister();
        }
    }

    /**
     * Reschedule test.
     * The job is rescheduled two times before it fails.
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testStartJobAndReschedule() throws Exception {
        final List<Integer> retryCountList = new ArrayList<Integer>();
        final Barrier cb = new Barrier(2);
        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC,
                new JobConsumer() {
                    int retryCount;

                    @Override
                    public JobResult process(Job job) {
                        int retry = 0;
                        if ( job.getProperty(Job.PROPERTY_JOB_RETRY_COUNT) != null ) {
                            retry = (Integer)job.getProperty(Job.PROPERTY_JOB_RETRY_COUNT);
                        }
                        if ( retry == retryCount ) {
                            retryCountList.add(retry);
                        }
                        retryCount++;
                        cb.block();
                        return JobResult.FAILED;
                    }
                });
        try {
            final JobManager jobManager = this.getJobManager();
            final Job job = jobManager.addJob(TOPIC, null, null);

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

            jobManager.removeJobById(job.getId());
        } finally {
            jcReg.unregister();
        }
    }

    /**
     * Notifications.
     * We send several jobs which are treated different and then see
     * how many invocations have been sent.
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testNotifications() throws Exception {
        final List<String> cancelled = Collections.synchronizedList(new ArrayList<String>());
        final List<String> failed = Collections.synchronizedList(new ArrayList<String>());
        final List<String> finished = Collections.synchronizedList(new ArrayList<String>());
        final List<String> started = Collections.synchronizedList(new ArrayList<String>());
        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        // events 1 and 4 finish the first time
                        final String id = job.getName();
                        if ( "1".equals(id) || "4".equals(id) ) {
                            return JobResult.OK;

                        // 5 fails always
                        } else if ( "5".equals(id) ) {
                            return JobResult.FAILED;
                        } else {
                            int retry = 0;
                            if ( job.getProperty(Job.PROPERTY_JOB_RETRY_COUNT) != null ) {
                                retry = (Integer)job.getProperty(Job.PROPERTY_JOB_RETRY_COUNT);
                            }
                            // 2 fails the first time
                            if ( "2".equals(id) ) {
                                if ( retry == 0 ) {
                                    return JobResult.FAILED;
                                } else {
                                    return JobResult.OK;
                                }
                            }
                            // 3 fails the first and second time
                            if ( "3".equals(id) ) {
                                if ( retry == 0 || retry == 1 ) {
                                    return JobResult.FAILED;
                                } else {
                                    return JobResult.OK;
                                }
                            }
                        }
                        return JobResult.FAILED;
                    }
                });
        final ServiceRegistration eh1Reg = this.registerEventHandler(JobUtil.TOPIC_JOB_CANCELLED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        final Event job = (Event) event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
                        final String id = (String)job.getProperty(ResourceHelper.PROPERTY_JOB_NAME);
                        cancelled.add(id);
                    }
                });
        final ServiceRegistration eh2Reg = this.registerEventHandler(JobUtil.TOPIC_JOB_FAILED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        final Event job = (Event) event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
                        final String id = (String)job.getProperty(ResourceHelper.PROPERTY_JOB_NAME);
                        failed.add(id);
                    }
                });
        final ServiceRegistration eh3Reg = this.registerEventHandler(JobUtil.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        final Event job = (Event) event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
                        final String id = (String)job.getProperty(ResourceHelper.PROPERTY_JOB_NAME);
                        finished.add(id);
                    }
                });
        final ServiceRegistration eh4Reg = this.registerEventHandler(JobUtil.TOPIC_JOB_STARTED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        final Event job = (Event) event.getProperty(JobUtil.PROPERTY_NOTIFICATION_JOB);
                        final String id = (String)job.getProperty(ResourceHelper.PROPERTY_JOB_NAME);
                        started.add(id);
                    }
                });

        final JobManager jobManager = this.getJobManager();
        try {
            jobManager.addJob(TOPIC, "1", null);
            jobManager.addJob(TOPIC, "2", null);
            jobManager.addJob(TOPIC, "3", null);
            jobManager.addJob(TOPIC, "4", null);
            jobManager.addJob(TOPIC, "5", null);

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
        } finally {
            final Collection<Job> col = jobManager.findJobs(JobManager.QueryType.HISTORY, "sling/test", -1, (Map<String, Object>[])null);
            for(final Job j : col) {
                jobManager.removeJobById(j.getId());
            }
            jcReg.unregister();
            eh1Reg.unregister();
            eh2Reg.unregister();
            eh3Reg.unregister();
            eh4Reg.unregister();
        }
    }

    /**
     * Test sending of jobs with and without a processor
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testNoJobProcessor() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger unprocessedCount = new AtomicInteger(0);

        final ServiceRegistration eh1 = this.registerEventHandler(TOPIC,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        JobUtil.processJob(event, new JobProcessor() {

                            @Override
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
                });
        final ServiceRegistration eh2 = this.registerEventHandler("sling/test2",
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        unprocessedCount.incrementAndGet();
                    }
                });
        final ServiceRegistration eh3 = this.registerEventHandler(JobUtil.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        count.incrementAndGet();
                    }
                });
        try {
            final JobManager jobManager = this.getJobManager();

            // we start 20 jobs, every second job has no processor
            final int COUNT = 20;
            for(int i = 0; i < COUNT; i++ ) {
                final String jobTopic = (i % 2 == 0 ? TOPIC : TOPIC + "2");
                final Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(ResourceHelper.PROPERTY_JOB_TOPIC, jobTopic);

                this.eventAdmin.postEvent(new Event(JobUtil.TOPIC_JOB, props));
            }
            final long startTime = System.currentTimeMillis();
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
            while ( System.currentTimeMillis() - startTime < 72000 ) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            ((Runnable)jobManager).run();
            while ( unprocessedCount.get() < COUNT ) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            assertEquals("Finished count", COUNT / 2, count.get());
            assertEquals("Unprocessed count",COUNT, unprocessedCount.get());
            assertEquals("Finished count", COUNT / 2, jobManager.getStatistics().getNumberOfFinishedJobs());

            // now remove jobs
            for(final Job j : jobManager.findJobs(JobManager.QueryType.ALL, TOPIC + "2", -1, (Map<String, Object>[])null)) {
                jobManager.removeJobById(j.getId());
            }
        } finally {
            eh1.unregister();
            eh2.unregister();
            eh3.unregister();
        }
    }
}