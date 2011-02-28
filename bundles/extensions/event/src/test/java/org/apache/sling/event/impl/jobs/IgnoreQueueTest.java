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
package org.apache.sling.event.impl.jobs;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.impl.AbstractTest;
import org.apache.sling.event.impl.SimpleEventAdmin;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.jcr.PersistenceHandler;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(JMock.class)
public class IgnoreQueueTest extends AbstractJobEventHandlerTest {

    private static final String QUEUE_NAME = "orderedtest";
    private static final String TOPIC = "sling/test";
    private static int NUM_JOBS = 10;

    protected Mockery context;

    private InternalQueueConfiguration mainConfiguration;

    public IgnoreQueueTest() {
        this.context = new JUnit4Mockery();
    }

    @Override
    protected Mockery getMockery() {
        return this.context;
    }

    @Override
    protected Hashtable<String, Object> getComponentConfig() {
        final Hashtable<String, Object> config =  super.getComponentConfig();
        config.put("cleanup.period", 1); // set clean up to 1 minute
        config.put("load.delay", 1); // load delay to 1 sec
        return config;
    }

    private void createConfiguration(final QueueConfiguration.Type type) {
        // create a new dictionary with the missing info and do some sanety puts
        final Map<String, Object> queueProps = new HashMap<String, Object>();
        queueProps.put(ConfigurationConstants.PROP_TOPICS, TOPIC);
        queueProps.put(ConfigurationConstants.PROP_NAME, QUEUE_NAME);
        queueProps.put(ConfigurationConstants.PROP_TYPE, type);
        queueProps.put(ConfigurationConstants.PROP_MAX_PARALLEL, new Integer(3));

        this.mainConfiguration = InternalQueueConfiguration.fromConfiguration(queueProps);
    }

    @Override
    protected QueueConfigurationManager createQueueConfigManager() {
        this.createConfiguration(QueueConfiguration.Type.IGNORE);
        return new QueueConfigurationManager() {

            @Override
            public InternalQueueConfiguration[] getConfigurations() {
                return new InternalQueueConfiguration[] {mainConfiguration};
            }
        };
    }

    /**
     * Helper method to create a job event.
     */
    private Event getJobEvent() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(JobUtil.PROPERTY_JOB_TOPIC, TOPIC);
        return new Event(JobUtil.TOPIC_JOB, props);
    }

    @org.junit.Test(timeout=30000) public void testIgnoreQueue() throws Exception {
        final PersistenceHandler jeh = this.handler;

        // set new event admin
        final AtomicInteger count = new AtomicInteger(0);
        setEventAdmin(new SimpleEventAdmin(new String[] {TOPIC },
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            JobUtil.processJob(event, new JobProcessor() {

                                public boolean process(Event job) {
                                    count.incrementAndGet();
                                    return true;
                                }
                            });
                        }
                    }}));
        // we start "some" jobs:
        for(int i = 0; i < NUM_JOBS; i++ ) {
            jeh.handleEvent(getJobEvent());
        }

        // we wait until NUM_JOBS have been processed by the JobManager
        while ( ((ExtendedJobManager)this.jobManager).getAdded() < NUM_JOBS ) {
            AbstractTest.sleep(400);
        }

        // no jobs queued, none processed but available
        assertEquals(0, this.jobManager.getStatistics().getNumberOfQueuedJobs());
        assertEquals(0, this.jobManager.getStatistics().getNumberOfProcessedJobs());
        assertEquals(0, count.get());
        assertEquals(NUM_JOBS, this.jobManager.queryJobs(JobManager.QueryType.ALL, TOPIC).getSize());

        // let'see if restarting helps
        this.createConfiguration(QueueConfiguration.Type.UNORDERED);
        this.jobManager.restart();
        // we wait until all jobs are processed
        while ( count.get() < NUM_JOBS ) {
            AbstractTest.sleep(500);
        }
        // we wait until all jobs are removed
        while ( ((ExtendedJobManager)this.jobManager).getRemoved() < NUM_JOBS ) {
            AbstractTest.sleep(500);
        }
        // no jobs queued, but processed and not available
        assertEquals(0, this.jobManager.getStatistics().getNumberOfQueuedJobs());
        assertEquals(NUM_JOBS, this.jobManager.getStatistics().getNumberOfProcessedJobs());
        assertEquals(NUM_JOBS, count.get());
        assertEquals(0, this.jobManager.queryJobs(JobManager.QueryType.ALL, TOPIC).getSize());
    }
}
