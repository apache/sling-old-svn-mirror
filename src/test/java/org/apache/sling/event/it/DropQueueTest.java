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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
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
public class DropQueueTest extends AbstractJobHandlingTest {

    private static final String QUEUE_NAME = "droptest";
    private static final String TOPIC = "sling/droptest";
    private static int NUM_JOBS = 10;

    private String queueConfPid;

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        // create ignore test queue
        final org.osgi.service.cm.Configuration orderedConfig = this.configAdmin.createFactoryConfiguration("org.apache.sling.event.jobs.QueueConfiguration", null);
        final Dictionary<String, Object> orderedProps = new Hashtable<String, Object>();
        orderedProps.put(ConfigurationConstants.PROP_NAME, QUEUE_NAME);
        orderedProps.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.DROP.name());
        orderedProps.put(ConfigurationConstants.PROP_TOPICS, TOPIC);
        orderedConfig.update(orderedProps);

        this.queueConfPid = orderedConfig.getPid();

        this.sleep(1000L);
    }

    @After
    public void cleanUp() throws IOException {
        this.removeConfiguration(this.queueConfPid);
        super.cleanup();
    }

    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testDroppingQueue() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger dropCount = new AtomicInteger(0);
        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        count.incrementAndGet();
                        return JobResult.OK;
                    }
                });
        final ServiceRegistration ehReg = this.registerEventHandler(JobUtil.TOPIC_JOB_CANCELLED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        dropCount.incrementAndGet();
                    }
                });
        try {
            final JobManager jobManager = this.getJobManager();

            // we start "some" jobs:
            for(int i = 0; i < NUM_JOBS; i++ ) {
                jobManager.addJob(TOPIC, null, null);
            }
            while ( dropCount.get() < NUM_JOBS ) {
                // we wait a little bit
                Thread.sleep(100);
            }

            // no jobs queued, none processed and no available
            assertEquals(0, jobManager.getStatistics().getNumberOfQueuedJobs());
            assertEquals(0, count.get());
            assertEquals(0, jobManager.findJobs(JobManager.QueryType.ALL, TOPIC, -1, (Map<String, Object>[])null).size());

            // let'see if restarting helps with a new queue config
            final org.osgi.service.cm.Configuration cf = this.configAdmin.getConfiguration(this.queueConfPid, null);
            @SuppressWarnings("unchecked")
            final Dictionary<String, Object> orderedProps = cf.getProperties();

            orderedProps.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.UNORDERED.name());
            cf.update(orderedProps);

            jobManager.restart();
            // we wait a little bit
            Thread.sleep(1200); // TODO - we have to wait until reload is done

             // no jobs queued, none processed and no available
            assertEquals(0, jobManager.getStatistics().getNumberOfQueuedJobs());
            assertEquals(0, count.get());
            assertEquals(0, jobManager.findJobs(JobManager.QueryType.ALL, TOPIC, -1, (Map<String, Object>[])null).size());
        } finally {
            jcReg.unregister();
            ehReg.unregister();
        }
    }
}
