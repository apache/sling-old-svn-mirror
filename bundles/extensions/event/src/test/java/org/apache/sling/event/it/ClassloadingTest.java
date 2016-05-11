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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(PaxExam.class)
public class ClassloadingTest extends AbstractJobHandlingTest {

    private static final int CONDITION_INTERVAL_MILLIS = 50;
    private static final int CONDITION_TIMEOUT_SECONDS = 5;

    private static final String QUEUE_NAME = "cltest";
    private static final String TOPIC = "sling/cltest";

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        // create ignore test queue
        final org.osgi.service.cm.Configuration orderedConfig = this.configAdmin.createFactoryConfiguration("org.apache.sling.event.jobs.QueueConfiguration", null);
        final Dictionary<String, Object> orderedProps = new Hashtable<String, Object>();
        orderedProps.put(ConfigurationConstants.PROP_NAME, QUEUE_NAME);
        orderedProps.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.UNORDERED.name());
        orderedProps.put(ConfigurationConstants.PROP_TOPICS, TOPIC);
        orderedConfig.update(orderedProps);

        this.sleep(1000L);
    }

    @Override
    @After
    public void cleanup() {
        super.cleanup();
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSimpleClassloading() throws Exception {
        final AtomicInteger processedJobsCount = new AtomicInteger(0);
        final List<Event> finishedEvents = Collections.synchronizedList(new ArrayList<Event>());
        final CountDownLatch latch = new CountDownLatch(1);
        this.registerJobConsumer(TOPIC,
                new JobConsumer() {
                    @Override
                    public JobResult process(Job job) {
                        processedJobsCount.incrementAndGet();
                        return JobResult.OK;
                    }
                });
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        finishedEvents.add(event);
                        latch.countDown();
                    }
                });
        final JobManager jobManager = this.getJobManager();

        final List<String> list = new ArrayList<String>();
        list.add("1");
        list.add("2");

        final Map<String, String> map = new HashMap<String, String>();
        map.put("a", "a1");
        map.put("b", "b2");

        // we start a single job
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("string", "Hello");
        props.put("int", new Integer(5));
        props.put("long", new Long(7));
        props.put("list", list);
        props.put("map", map);

        final String jobId = jobManager.addJob(TOPIC, props).getId();
        try {
            latch.await(5, TimeUnit.SECONDS);
            assertFalse("At least one finished job", finishedEvents.isEmpty());
            assertEquals(1, processedJobsCount.get());

            final String jobTopic = (String)finishedEvents.get(0).getProperty(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC);
            assertNotNull(jobTopic);
            assertEquals("Hello", finishedEvents.get(0).getProperty("string"));
            assertEquals(new Integer(5), Integer.valueOf(finishedEvents.get(0).getProperty("int").toString()));
            assertEquals(new Long(7), Long.valueOf(finishedEvents.get(0).getProperty("long").toString()));
            assertEquals(list, finishedEvents.get(0).getProperty("list"));
            assertEquals(map, finishedEvents.get(0).getProperty("map"));
        } finally {
            jobManager.removeJobById(jobId);
        }
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testFailedClassloading() throws Exception {
        final AtomicInteger failedJobsCount = new AtomicInteger(0);
        final List<Event> finishedEvents = Collections.synchronizedList(new ArrayList<Event>());
        this.registerJobConsumer(TOPIC + "/failed",
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        failedJobsCount.incrementAndGet();
                        return JobResult.OK;
                    }
                });
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        finishedEvents.add(event);
                    }
                });
        final JobManager jobManager = this.getJobManager();

        // dao is an invisible class for the dynamic class loader as it is not public
        // therefore scheduling this job should fail!
        final DataObject dao = new DataObject();

        // we start a single job
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("dao", dao);

        final String id = jobManager.addJob(TOPIC + "/failed", props).getId();

        try {
            // wait until the conditions are met
            new RetryLoop(new RetryLoop.Condition() {

                @Override
                public boolean isTrue() throws Exception {
                    return failedJobsCount.get() == 0
                            && finishedEvents.size() == 0
                            && jobManager.findJobs(JobManager.QueryType.ALL, TOPIC + "/failed", -1,
                                    (Map<String, Object>[]) null).size() == 1
                            && jobManager.getStatistics().getNumberOfQueuedJobs() == 0
                            && jobManager.getStatistics().getNumberOfActiveJobs() == 0;
                }

                @Override
                public String getDescription() {
                    return "Waiting for job failure to be recorded. Conditions " +
                           "faildJobsCount=" + failedJobsCount.get() +
                           ", finishedEvents=" + finishedEvents.size() +
                           ", findJobs= " + jobManager.findJobs(JobManager.QueryType.ALL, TOPIC + "/failed", -1,
                                   (Map<String, Object>[]) null).size()
                           +", queuedJobs=" + jobManager.getStatistics().getNumberOfQueuedJobs()
                           +", activeJobs=" + jobManager.getStatistics().getNumberOfActiveJobs();
                }
            }, CONDITION_TIMEOUT_SECONDS, CONDITION_INTERVAL_MILLIS);

            jobManager.removeJobById(id); // moves the job to the history section
            assertEquals(0, jobManager.findJobs(JobManager.QueryType.ALL, TOPIC + "/failed", -1, (Map<String, Object>[])null).size());
        } finally {
            jobManager.removeJobById(id); // removes the job permanently
        }
    }

    private static final class DataObject implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
