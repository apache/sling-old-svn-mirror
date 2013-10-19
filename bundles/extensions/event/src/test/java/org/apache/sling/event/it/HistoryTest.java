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

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.QueueConfiguration;
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

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HistoryTest extends AbstractJobHandlingTest {

    private static final String TOPIC = "sling/test/history";

    private static final String PROP_COUNTER = "counter";

    private String queueConfPid;

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        // create test queue - we use an ordered queue to have a stable processing order
        // keep the jobs in the history
        final org.osgi.service.cm.Configuration config = this.configAdmin.createFactoryConfiguration("org.apache.sling.event.jobs.QueueConfiguration", null);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ConfigurationConstants.PROP_NAME, "test");
        props.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.ORDERED.name());
        props.put(ConfigurationConstants.PROP_TOPICS, new String[] {TOPIC});
        props.put(ConfigurationConstants.PROP_RETRIES, 2);
        props.put(ConfigurationConstants.PROP_RETRY_DELAY, 2L);
        props.put(ConfigurationConstants.PROP_KEEP_JOBS, true);
        config.update(props);

        this.queueConfPid = config.getPid();
        this.sleep(1000L);
    }

    @After
    public void cleanUp() throws IOException {
        this.removeConfiguration(this.queueConfPid);
        super.cleanup();
    }

    private Job addJob(final long counter) {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_COUNTER, counter);
        return this.getJobManager().addJob(TOPIC, props );
    }

    /**
     * Test history.
     * Start 10 jobs and cancel some of them and succeed others
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testHistory() throws Exception {
        final ServiceRegistration reg = this.registerJobExecutor(TOPIC,
                new JobExecutor() {

                    @Override
                    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
                        sleep(5L);
                        final long count = job.getProperty(PROP_COUNTER, Long.class);
                        if ( count == 2 || count == 5 || count == 7 ) {
                            return context.result().message(Job.JobState.ERROR.name()).cancelled();
                        }
                        return context.result().message(Job.JobState.SUCCEEDED.name()).succeeded();
                    }

                });
        Collection<Job> col = null;
        try {
            for(int i = 0; i< 10; i++) {
                this.addJob(i);
            }
            this.sleep(200L);
            while ( this.getJobManager().findJobs(JobManager.QueryType.HISTORY, TOPIC, -1, (Map<String, Object>[])null).size() < 10 ) {
                this.sleep(20L);
            }
            col = this.getJobManager().findJobs(JobManager.QueryType.HISTORY, TOPIC, -1, (Map<String, Object>[])null);
            assertEquals(10, col.size());
            assertEquals(0, this.getJobManager().findJobs(JobManager.QueryType.ACTIVE, TOPIC, -1, (Map<String, Object>[])null).size());
            assertEquals(0, this.getJobManager().findJobs(JobManager.QueryType.QUEUED, TOPIC, -1, (Map<String, Object>[])null).size());
            assertEquals(0, this.getJobManager().findJobs(JobManager.QueryType.ALL, TOPIC, -1, (Map<String, Object>[])null).size());
            assertEquals(3, this.getJobManager().findJobs(JobManager.QueryType.CANCELLED, TOPIC, -1, (Map<String, Object>[])null).size());
            assertEquals(0, this.getJobManager().findJobs(JobManager.QueryType.DROPPED, TOPIC, -1, (Map<String, Object>[])null).size());
            assertEquals(3, this.getJobManager().findJobs(JobManager.QueryType.ERROR, TOPIC, -1, (Map<String, Object>[])null).size());
            assertEquals(0, this.getJobManager().findJobs(JobManager.QueryType.GIVEN_UP, TOPIC, -1, (Map<String, Object>[])null).size());
            assertEquals(0, this.getJobManager().findJobs(JobManager.QueryType.STOPPED, TOPIC, -1, (Map<String, Object>[])null).size());
            assertEquals(7, this.getJobManager().findJobs(JobManager.QueryType.SUCCEEDED, TOPIC, -1, (Map<String, Object>[])null).size());
            // verify order, message and state
            long last = 9;
            for(final Job j : col) {
                assertNotNull(j.getFinishedDate());
                final long count = j.getProperty(PROP_COUNTER, Long.class);
                assertEquals(last, count);
                if ( count == 2 || count == 5 || count == 7 ) {
                    assertEquals(Job.JobState.ERROR, j.getJobState());
                } else {
                    assertEquals(Job.JobState.SUCCEEDED, j.getJobState());
                }
                assertEquals(j.getJobState().name(), j.getResultMessage());
                last--;
            }
        } finally {
            if ( col != null ) {
                for(final Job j : col) {
                    this.getJobManager().removeJobById(j.getId());
                }
            }
            reg.unregister();
        }
    }
}