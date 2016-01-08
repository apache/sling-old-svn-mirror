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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.impl.Barrier;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(PaxExam.class)
public class UnorderedQueueTest extends AbstractJobHandlingTest {

    private static final String QUEUE_NAME = "unorderedtestqueue";
    private static final String TOPIC = "sling/unorderedtest";
    private static int MAX_PAR = 5;
    private static int NUM_JOBS = 300;

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        // create round robin test queue
        final org.osgi.service.cm.Configuration rrConfig = this.configAdmin.createFactoryConfiguration("org.apache.sling.event.jobs.QueueConfiguration", null);
        final Dictionary<String, Object> rrProps = new Hashtable<String, Object>();
        rrProps.put(ConfigurationConstants.PROP_NAME, QUEUE_NAME);
        rrProps.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.UNORDERED.name());
        rrProps.put(ConfigurationConstants.PROP_TOPICS, TOPIC + "/*");
        rrProps.put(ConfigurationConstants.PROP_RETRIES, 2);
        rrProps.put(ConfigurationConstants.PROP_RETRY_DELAY, 2000L);
        rrProps.put(ConfigurationConstants.PROP_MAX_PARALLEL, MAX_PAR);
        rrConfig.update(rrProps);

        this.sleep(1000L);
    }

    @Override
    @After
    public void cleanup() {
        super.cleanup();
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testUnorderedQueue() throws Exception {
        final JobManager jobManager = this.getJobManager();

        final Barrier cb = new Barrier(2);

        final ServiceRegistration jc1Reg = this.registerJobConsumer(TOPIC + "/start",
                new JobConsumer() {

                    @Override
                    public JobResult process(final Job job) {
                        cb.block();
                        return JobResult.OK;
                    }
                });

        // register new consumer and event handle
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger parallelCount = new AtomicInteger(0);
        final Set<Integer> maxParticipants = new HashSet<Integer>();

        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC + "/*",
                new JobConsumer() {

                    @Override
                    public JobResult process(final Job job) {
                        final int max = parallelCount.incrementAndGet();
                        if ( max > MAX_PAR ) {
                            parallelCount.decrementAndGet();
                            return JobResult.FAILED;
                        }
                        synchronized ( maxParticipants ) {
                            maxParticipants.add(max);
                        }
                        sleep(job.getProperty("sleep", 30));
                        parallelCount.decrementAndGet();
                        return JobResult.OK;
                    }
                });
        final ServiceRegistration ehReg = this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(final Event event) {
                        count.incrementAndGet();
                    }
                });

        try {
            // we first sent one event to get the queue started
            jobManager.addJob(TOPIC + "/start", null);
            assertTrue("No event received in the given time.", cb.block(5));
            cb.reset();

            // get the queue
            final Queue q = jobManager.getQueue(QUEUE_NAME);
            assertNotNull("Queue '" + QUEUE_NAME + "' should exist!", q);

            // suspend it
            q.suspend();

            // we start "some" jobs:
            for(int i = 0; i < NUM_JOBS; i++ ) {
                final String subTopic = TOPIC + "/sub" + (i % 10);
                final Map<String, Object> props = new HashMap<String, Object>();
                if ( i < 10 ) {
                    props.put("sleep", 300);
                } else {
                    props.put("sleep", 30);
                }
                jobManager.addJob(subTopic, props);
            }
            // start the queue
            q.resume();
            while ( count.get() < NUM_JOBS  + 1 ) {
                assertEquals("Failed count", 0, q.getStatistics().getNumberOfFailedJobs());
                assertEquals("Cancelled count", 0, q.getStatistics().getNumberOfCancelledJobs());
                sleep(300);
            }
            // we started one event before the test, so add one
            assertEquals("Finished count", NUM_JOBS + 1, count.get());
            assertEquals("Finished count", NUM_JOBS + 1, jobManager.getStatistics().getNumberOfFinishedJobs());
            assertEquals("Finished count", NUM_JOBS + 1, q.getStatistics().getNumberOfFinishedJobs());
            assertEquals("Failed count", 0, q.getStatistics().getNumberOfFailedJobs());
            assertEquals("Cancelled count", 0, q.getStatistics().getNumberOfCancelledJobs());
            for(int i=1; i <= MAX_PAR; i++) {
                assertTrue("# Participants " + String.valueOf(i) + " not in " + maxParticipants,
                        maxParticipants.contains(i));
            }
        } finally {
            jc1Reg.unregister();
            jcReg.unregister();
            ehReg.unregister();
        }
    }
}
