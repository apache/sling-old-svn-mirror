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

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotNull;

/**
 * IT for validating SLING-3502.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OutdatedMainQueueTest extends AbstractJobHandlingTest {

    private static final String QUEUE_NAME = "<main queue>";

    private static final String TOPIC = "sling/event/test/queue/outdate";

    private static int NUM_JOBS = 2;

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();
    }

    @After
    public void cleanUp() throws IOException {
        super.cleanup();
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testOutdatedMainQueue() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);

        final ServiceRegistration testConsumerRegistration = this.registerJobConsumer(TOPIC,
                new JobConsumer() {
                    @Override
                    public JobResult process(Job job) {
                        count.incrementAndGet();
                        return JobResult.OK;
                    }
                });

        try {
            final JobManager jobManager = this.getJobManager();

            //reset the statistic
            jobManager.getStatistics().reset();

            //first start some jobs, this will implicitly instantiate the main queue
            List<Job> testJobs = new ArrayList<Job>();
            for (int i = 0; i < NUM_JOBS; i++) {
                Job job = jobManager.createJob(TOPIC).add();
                testJobs.add(job);
            }

            //wait for test jobs to complete
            while (!testJobsDone(testJobs, jobManager)) {
                Thread.sleep(1000);
            }

            //assert successful job execution
            assertEquals("No queued jobs expected", 0, jobManager.getStatistics().getNumberOfQueuedJobs());
            assertEquals("Number of executed jobs does not match", NUM_JOBS, count.get());
            assertEquals("Number of finished jobs does not match", NUM_JOBS, jobManager.getStatistics().getNumberOfFinishedJobs());

            //assert the queues: only main queue expected, not outdated
            List<Queue> queues = getQueues(jobManager);
            assertEquals("Only 1 job queue expected", 1, queues.size());
            Queue mainQueue = jobManager.getQueue("<main queue>");
            assertNotNull("Main queue expected", mainQueue);
            assertEquals("Main queue expected", "<main queue>", mainQueue.getName());
            assertSame("Two different main queues", queues.get(0), mainQueue);

            //restart job manager, has same effect as topology change, as well calls  JobManagerImpl.outdateQueue()
            jobManager.restart();

            // we wait a little bit
            Thread.sleep(1200);

            //assert the queues: only one outdated main queue expected
            queues = getQueues(jobManager);
            assertEquals("Only 1 job queue expected", 1, queues.size());
            mainQueue = queues.get(0);
            String outdatedQueueName = getOutdatedMainQueueName(mainQueue);
            mainQueue = jobManager.getQueue(outdatedQueueName);
            assertNotNull("Outdated main queue expected", mainQueue);
            assertEquals("Outdated main queue expected", getOutdatedMainQueueName(mainQueue), mainQueue.getName());

        } finally {
            testConsumerRegistration.unregister();
        }
    }

    private List<Queue> getQueues(JobManager jobManager) {
        List<Queue> queues = new ArrayList<Queue>();
        Iterator<Queue> queueIter = jobManager.getQueues().iterator();
        while (queueIter.hasNext()) {
            queues.add(queueIter.next());
        }
        return queues;
    }

    private String getOutdatedMainQueueName(Queue queue) {
        return "<main queue><outdated>(" + queue.hashCode() + ")";
    }

    private boolean testJobsDone(List<Job> testJobs, JobManager jobManager) {
        boolean done = false;
        for (Job testJob : testJobs) {
            Job tmpJob = jobManager.getJobById(testJob.getId());
            if (tmpJob == null || tmpJob.getJobState().equals(Job.JobState.SUCCEEDED)) {
                done = true;
            } else {
                done = false;
                break;
            }
        }
        return done;
    }
}
