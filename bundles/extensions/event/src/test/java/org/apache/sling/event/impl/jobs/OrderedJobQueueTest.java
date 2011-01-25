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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.impl.Barrier;
import org.apache.sling.event.impl.SimpleEventAdmin;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.jcr.PersistenceHandler;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(JMock.class)
public class OrderedJobQueueTest extends AbstractJobEventHandlerTest {

    private static final String QUEUE_NAME = "orderedtest";
    private static final String TOPIC = "sling/test";
    private static int NUM_JOBS = 30;

    protected Mockery context;

    public OrderedJobQueueTest() {
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

    @Override
    protected QueueConfigurationManager createQueueConfigManager() {
        // create a new dictionary with the missing info and do some sanety puts
        final Map<String, Object> queueProps = new HashMap<String, Object>();
        queueProps.put(ConfigurationConstants.PROP_TOPICS, TOPIC + "/*");
        queueProps.put(ConfigurationConstants.PROP_NAME, QUEUE_NAME);
        queueProps.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.ORDERED);

        final InternalQueueConfiguration mainConfiguration = InternalQueueConfiguration.fromConfiguration(queueProps);
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
    private Event getJobEvent(final String subTopic, final String id) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(JobUtil.PROPERTY_JOB_TOPIC, TOPIC + '/' + subTopic);
        if ( id != null ) {
            props.put(JobUtil.PROPERTY_JOB_NAME, id);
        }
        return new Event(JobUtil.TOPIC_JOB, props);
    }

    /**
     * Helper method to create a job event.
     */
    private Event getJobEvent(final String subTopic) {
        return this.getJobEvent(subTopic, null);
    }

    @org.junit.Test public void testOrderedQueue() throws Exception {
        final PersistenceHandler jeh = this.handler;

        // we first send one event to get the queue started
        final Barrier cb = new Barrier(2);
        setEventAdmin(new SimpleEventAdmin(new String[] {TOPIC + '*'},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(Event event) {
                            JobUtil.acknowledgeJob(event);
                            JobUtil.finishedJob(event);
                            cb.block();
                        }

                    }
                }));
        jeh.handleEvent(getJobEvent("a"));
        assertTrue("No event received in the given time.", cb.block(5));
        cb.reset();

        // get the queue
        final Queue q = this.jobManager.getQueue(QUEUE_NAME);
        assertNotNull("Queue should exist!", q);
        // suspend it
        q.suspend();
        // set new event admin
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger parallelCount = new AtomicInteger(0);
        setEventAdmin(new SimpleEventAdmin(new String[] {TOPIC + '*',
                JobUtil.TOPIC_JOB_FINISHED},
                new EventHandler[] {
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            JobUtil.processJob(event, new JobProcessor() {

                                public boolean process(Event job) {
                                    if ( parallelCount.incrementAndGet() > 1 ) {
                                        parallelCount.decrementAndGet();
                                        return false;
                                    }
                                    final String topic = (String)job.getProperty(JobUtil.PROPERTY_JOB_TOPIC);
                                    if ( topic.endsWith("sub1") ) {
                                        final int i = (Integer)job.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT);
                                        if ( i == 0 ) {
                                            parallelCount.decrementAndGet();
                                            return false;
                                        }
                                    }
                                    try {
                                        Thread.sleep(30);
                                    } catch (InterruptedException ie) {
                                        // ignore
                                    }
                                    parallelCount.decrementAndGet();
                                    return true;
                                }
                            });
                        }
                    },
                    new EventHandler() {
                        public void handleEvent(final Event event) {
                            count.incrementAndGet();
                        }
                    }}));
        // we start "some" jobs:
        for(int i = 0; i < NUM_JOBS; i++ ) {
            final String subTopic = "sub" + (i % 10);
            jeh.handleEvent(getJobEvent(subTopic));
        }
        // start the queue
        q.resume();
        while ( count.get() < NUM_JOBS ) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
        assertEquals("Finished count", NUM_JOBS, count.get());
        // we started one event before the test, so add one
        assertEquals("Finished count", NUM_JOBS + 1, this.jobManager.getStatistics().getNumberOfFinishedJobs());
        assertEquals("Finished count", NUM_JOBS + 1, q.getStatistics().getNumberOfFinishedJobs());
        assertEquals("Failed count", NUM_JOBS / 10, q.getStatistics().getNumberOfFailedJobs());
        assertEquals("Cancelled count", 0, q.getStatistics().getNumberOfCancelledJobs());
    }
}
