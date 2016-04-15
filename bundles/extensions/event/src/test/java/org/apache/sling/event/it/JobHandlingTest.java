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
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(PaxExam.class)
public class JobHandlingTest extends AbstractJobHandlingTest {

    public static final String TOPIC = "sling/test";

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

        this.sleep(1000L);
    }

    @Override
    @After
    public void cleanup() {
        super.cleanup();
    }

    /**
     * Test simple job execution.
     * The job is executed once and finished successfully.
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSimpleJobExecutionUsingJobConsumer() throws Exception {
        final Barrier cb = new Barrier(2);

        this.registerJobConsumer(TOPIC,
                new JobConsumer() {

            @Override
                    public JobResult process(final Job job) {
                        cb.block();
                        return JobResult.OK;
                    }
                 });

        this.getJobManager().addJob(TOPIC, null);
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
    }

    /**
     * Test simple job execution.
     * The job is executed once and finished successfully.
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSimpleJobExecutionUsingJobExecutor() throws Exception {
        final Barrier cb = new Barrier(2);

        this.registerJobExecutor(TOPIC,
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        cb.block();
                        return context.result().succeeded();
                    }
                });

        this.getJobManager().addJob(TOPIC, null);
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();
        assertFalse("Unexpected event received in the given time.", cb.block(5));
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testManyJobs() throws Exception {
        this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(final Job job) {
                        return JobResult.OK;
                    }

                 });
        final AtomicInteger count = new AtomicInteger(0);
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {
                    @Override
                    public void handleEvent(final Event event) {
                        count.incrementAndGet();
                    }
                 });

        // we start "some" jobs
        final int COUNT = 300;
        for(int i = 0; i < COUNT; i++ ) {
            this.getJobManager().addJob(TOPIC, null);
        }
        while ( count.get() < COUNT ) {
            this.sleep(50);
        }
        assertEquals("Finished count", COUNT, count.get());
        assertEquals("Finished count", COUNT, this.getJobManager().getStatistics().getNumberOfFinishedJobs());
    }

    /**
     * Test canceling a job
     * The job execution always fails
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testCancelJob() throws Exception {
        final Barrier cb = new Barrier(2);
        final Barrier cb2 = new Barrier(2);
        this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        cb.block();
                        cb2.block();
                        return JobResult.FAILED;
                    }
                });

        final Map<String, Object> jobProperties = Collections.singletonMap("id", (Object)"cancelJobId");
        @SuppressWarnings("unchecked")
        final Map<String, Object>[] jobPropertiesAsArray = new Map[1];
        jobPropertiesAsArray[0] = jobProperties;

        // create job
        final JobManager jobManager = this.getJobManager();
        jobManager.addJob(TOPIC, jobProperties);
        cb.block();

        assertEquals(1, jobManager.findJobs(JobManager.QueryType.ALL, TOPIC, -1, jobPropertiesAsArray).size());
        // job is currently waiting, therefore cancel fails
        final Job e1 = jobManager.getJob(TOPIC, jobProperties);
        assertNotNull(e1);
        cb2.block(); // and continue job

        sleep(200);

        // the job is now in the queue again
        final Job e2 = jobManager.getJob(TOPIC, jobProperties);
        assertNotNull(e2);
        assertTrue(jobManager.removeJobById(e2.getId()));
        assertEquals(0, jobManager.findJobs(JobManager.QueryType.ALL, TOPIC, -1, jobPropertiesAsArray).size());
        final Collection<Job> col = jobManager.findJobs(JobManager.QueryType.HISTORY, TOPIC, -1,
                jobPropertiesAsArray);
        try {
            assertEquals(1, col.size());
        } finally {
            for(final Job j : col) {
                jobManager.removeJobById(j.getId());
            }
        }
   }

    /**
     * Test get a job
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testGetJob() throws Exception {
        final Barrier cb = new Barrier(2);
        final Barrier cb2 = new Barrier(2);
        this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        cb.block();
                        cb2.block();
                        return JobResult.OK;
                    }
                });
        final JobManager jobManager = this.getJobManager();
        final Job j = jobManager.addJob(TOPIC, null);
        cb.block();

        assertNotNull(jobManager.getJob(TOPIC, null));

        cb2.block(); // and continue job

        jobManager.removeJobById(j.getId());
    }

    /**
     * Reschedule test.
     * The job is rescheduled two times before it fails.
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testStartJobAndReschedule() throws Exception {
        final List<Integer> retryCountList = new ArrayList<Integer>();
        final Barrier cb = new Barrier(2);

        this.registerJobConsumer(TOPIC,
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

        final JobManager jobManager = this.getJobManager();
        final Job job = jobManager.addJob(TOPIC, null);

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
        this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        // events 1 and 4 finish the first time
                        final String id = (String)job.getProperty("id");
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
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_CANCELLED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        final String id = (String)event.getProperty("id");
                        cancelled.add(id);
                    }
                });
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_FAILED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        final String id = (String)event.getProperty("id");
                        failed.add(id);
                    }
                });
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        final String id = (String)event.getProperty("id");
                        finished.add(id);
                    }
                });
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_STARTED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        final String id = (String)event.getProperty("id");
                        started.add(id);
                    }
                });

        final JobManager jobManager = this.getJobManager();

        jobManager.addJob(TOPIC, Collections.singletonMap("id", (Object)"1"));
        jobManager.addJob(TOPIC, Collections.singletonMap("id", (Object)"2"));
        jobManager.addJob(TOPIC, Collections.singletonMap("id", (Object)"3"));
        jobManager.addJob(TOPIC, Collections.singletonMap("id", (Object)"4"));
        jobManager.addJob(TOPIC, Collections.singletonMap("id", (Object)"5"));

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

    /**
     * Test sending of jobs with and without a processor
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testNoJobProcessor() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);

        this.registerJobConsumer(TOPIC,
                new JobConsumer() {

            @Override
            public JobResult process(final Job job) {
                count.incrementAndGet();

                return JobResult.OK;
            }
         });

        final JobManager jobManager = this.getJobManager();

        // we start 20 jobs, every second job has no processor
        final int COUNT = 20;
        for(int i = 0; i < COUNT; i++ ) {
            final String jobTopic = (i % 2 == 0 ? TOPIC : TOPIC + "2");

            jobManager.addJob(jobTopic, null);
        }
        while ( jobManager.getStatistics().getNumberOfFinishedJobs() < COUNT / 2) {
            this.sleep(50);
        }

        assertEquals("Finished count", COUNT / 2, count.get());
        // unprocessed count should be 0 as there is no job consumer for this job
        assertEquals("Unprocessed count", 0, jobManager.getStatistics().getNumberOfJobs());
        assertEquals("Finished count", COUNT / 2, jobManager.getStatistics().getNumberOfFinishedJobs());
    }
}