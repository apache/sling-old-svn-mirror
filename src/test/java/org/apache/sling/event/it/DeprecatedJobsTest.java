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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.event.impl.Barrier;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.impl.support.ResourceHelper;
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
public class DeprecatedJobsTest extends AbstractJobHandlingTest {

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
            props.put(JobUtil.PROPERTY_JOB_NAME, id);
        }

        return new Event(JobUtil.TOPIC_JOB, props);
    }

    /**
     * Test simple job execution.
     * The job is executed once and finished successfully.
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSimpleJobExecutionUsingBridge() throws Exception {
        final Barrier cb = new Barrier(2);

        final ServiceRegistration reg = this.registerEventHandler(TOPIC,
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
            final Event e = jobManager.findJob("sling/test", Collections.singletonMap(JobUtil.PROPERTY_JOB_NAME, (Object)"myid3"));
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
}