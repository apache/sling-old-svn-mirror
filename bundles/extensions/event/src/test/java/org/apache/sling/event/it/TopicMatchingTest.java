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

import java.io.IOException;

import org.apache.sling.event.impl.Barrier;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(PaxExam.class)
public class TopicMatchingTest extends AbstractJobHandlingTest {

    public static final String TOPIC = "sling/test/a";

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        this.sleep(1000L);
    }

    @Override
    @After
    public void cleanup() {
        super.cleanup();
    }

    /**
     * Test simple pattern matching /*
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSimpleMatching() throws Exception {
        final Barrier barrier = new Barrier(2);

        this.registerJobExecutor("sling/test/*",
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        return context.result().succeeded();
                    }
                });
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(final Event event) {
                        barrier.block();
                    }
                });

        this.getJobManager().addJob(TOPIC, null);
        barrier.block();
    }

    /**
     * Test deep pattern matching /**
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testDeepMatching() throws Exception {
        final Barrier barrier = new Barrier(2);

        this.registerJobExecutor("sling/**",
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        return context.result().succeeded();
                    }
                });
        this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(final Event event) {
                        barrier.block();
                    }
                });

        this.getJobManager().addJob(TOPIC, null);
        barrier.block();
    }

    /**
     * Test ordering of matchers
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testOrdering() throws Exception {
        final Barrier barrier1 = new Barrier(2);
        final Barrier barrier2 = new Barrier(2);
        final Barrier barrier3 = new Barrier(2);

        this.registerJobExecutor("sling/**",
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        barrier1.block();
                        return context.result().succeeded();
                    }
                });
        final ServiceRegistration<JobExecutor> reg2 = this.registerJobExecutor("sling/test/*",
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        barrier2.block();
                        return context.result().succeeded();
                    }
                });
        final ServiceRegistration<JobExecutor> reg3 = this.registerJobExecutor(TOPIC,
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        barrier3.block();
                        return context.result().succeeded();
                    }
                });

        // first test, all three registered, reg3 should get the precedence
        this.getJobManager().addJob(TOPIC, null);
        barrier3.block();

        // second test, unregister reg3, now it should be reg2
        long cc = this.getConsumerChangeCount();
        this.unregister(reg3);
        this.waitConsumerChangeCount(cc + 1);
        this.getJobManager().addJob(TOPIC, null);
        barrier2.block();

        // third test, unregister reg2, reg1 is now the only one
        cc = this.getConsumerChangeCount();
        this.unregister(reg2);
        this.waitConsumerChangeCount(cc + 1);
        this.getJobManager().addJob(TOPIC, null);
        barrier1.block();
    }
}
