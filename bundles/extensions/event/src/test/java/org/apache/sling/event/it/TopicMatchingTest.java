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
import java.util.concurrent.atomic.AtomicInteger;

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
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class TopicMatchingTest extends AbstractJobHandlingTest {

    public static final String TOPIC = "sling/test/a";

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();
    }

    @Override
    @After
    public void cleanup() {
        super.cleanup();
    }

    /**
     * Test simple pattern matching
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSimpleMatching() throws Exception {
        final AtomicInteger finishedCount = new AtomicInteger();

        final ServiceRegistration reg = this.registerJobExecutor("sling/test/*",
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        return context.result().succeeded();
                    }
                });
        final ServiceRegistration eventHandler = this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(final Event event) {
                        finishedCount.incrementAndGet();
                    }
                });

        try {
            this.getJobManager().addJob(TOPIC, null);
            while ( finishedCount.get() == 0 ) {
                this.sleep(10);
            }
        } finally {
            reg.unregister();
            eventHandler.unregister();
        }
    }

    /**
     * Test deep pattern matching
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testDeepMatching() throws Exception {
        final AtomicInteger finishedCount = new AtomicInteger();

        final ServiceRegistration reg = this.registerJobExecutor("sling/**",
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        return context.result().succeeded();
                    }
                });
        final ServiceRegistration eventHandler = this.registerEventHandler(NotificationConstants.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(final Event event) {
                        finishedCount.incrementAndGet();
                    }
                });

        try {
            this.getJobManager().addJob(TOPIC, null);
            while ( finishedCount.get() == 0 ) {
                this.sleep(10);
            }
        } finally {
            reg.unregister();
            eventHandler.unregister();
        }
    }

    /**
     * Test ordering of matchers
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testOrdering() throws Exception {
        final AtomicInteger count1 = new AtomicInteger();
        final AtomicInteger count2 = new AtomicInteger();
        final AtomicInteger count3 = new AtomicInteger();

        final ServiceRegistration reg1 = this.registerJobExecutor("sling/**",
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        count1.incrementAndGet();
                        return context.result().succeeded();
                    }
                });
        final ServiceRegistration reg2 = this.registerJobExecutor("sling/test/*",
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        count2.incrementAndGet();
                        return context.result().succeeded();
                    }
                });
        final ServiceRegistration reg3 = this.registerJobExecutor(TOPIC,
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        count3.incrementAndGet();
                        return context.result().succeeded();
                    }
                });

        // first test, all three registered, reg3 should get the precedence
        this.getJobManager().addJob(TOPIC, null);
        while ( count3.get() != 1 ) {
            this.sleep(10);
        }

        // second test, unregister reg3, now it should be reg2
        reg3.unregister();
        this.getJobManager().addJob(TOPIC, null);
        while ( count2.get() != 1 ) {
            this.sleep(10);
        }

        // third test, unregister reg2, reg1 is now the only one
        reg2.unregister();
        this.getJobManager().addJob(TOPIC, null);
        while ( count1.get() != 1 ) {
            this.sleep(10);
        }
        reg1.unregister();
    }
}
