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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.EventPropertiesMap;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ClassloadingTest extends AbstractJobHandlingTest {

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


    @org.junit.Test public void testSimpleClassloading() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final List<Event> finishedEvents = Collections.synchronizedList(new ArrayList<Event>());
        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC,
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        count.incrementAndGet();
                        return JobResult.OK;
                    }
                });
        final ServiceRegistration ehReg = this.registerEventHandler(JobUtil.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        finishedEvents.add(event);
                    }
                });
        try {
            final JobManager jobManager = this.getJobManager();

            final List<String> list = new ArrayList<String>();
            list.add("1");
            list.add("2");

            final EventPropertiesMap map = new EventPropertiesMap();
            map.put("a", "a1");
            map.put("b", "b2");

            // we start a single job
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put("string", "Hello");
            props.put("int", new Integer(5));
            props.put("long", new Long(7));
            props.put("list", list);
            props.put("map", map);

            jobManager.addJob(TOPIC, null, props);

            while ( finishedEvents.size() < 1 ) {
                // we wait a little bit
                Thread.sleep(100);
            }
            Thread.sleep(100);

            // no jobs queued, none processed and no available
            assertEquals(0, jobManager.getStatistics().getNumberOfQueuedJobs());
            assertEquals(1, count.get());
            assertEquals(0, jobManager.findJobs(JobManager.QueryType.ALL, TOPIC, -1, (Map<String, Object>[])null).size());

            final String jobTopic = (String)finishedEvents.get(0).getProperty(JobUtil.NOTIFICATION_PROPERTY_JOB_TOPIC);
            assertNotNull(jobTopic);
            assertEquals("Hello", finishedEvents.get(0).getProperty("string"));
            assertEquals(new Integer(5), Integer.valueOf(finishedEvents.get(0).getProperty("int").toString()));
            assertEquals(new Long(7), Long.valueOf(finishedEvents.get(0).getProperty("long").toString()));
            assertEquals(list, finishedEvents.get(0).getProperty("list"));
            assertEquals(map, finishedEvents.get(0).getProperty("map"));
        } finally {
            jcReg.unregister();
            ehReg.unregister();
        }
    }

    @org.junit.Test public void testFailedClassloading() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final List<Event> finishedEvents = Collections.synchronizedList(new ArrayList<Event>());
        final ServiceRegistration jcReg = this.registerJobConsumer(TOPIC + "/failed",
                new JobConsumer() {

                    @Override
                    public JobResult process(Job job) {
                        count.incrementAndGet();
                        return JobResult.OK;
                    }
                });
        final ServiceRegistration ehReg = this.registerEventHandler(JobUtil.TOPIC_JOB_FINISHED,
                new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        finishedEvents.add(event);
                    }
                });
        try {
            final JobManager jobManager = this.getJobManager();

            // dao is an invisible class for the dynamic class loader as it is not public
            // therefore scheduling this job should fail!
            final DataObject dao = new DataObject();

            // we start a single job
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put("dao", dao);

            jobManager.addJob(TOPIC + "/failed", null, props);

            // we simply wait a little bit
            sleep(2000);

            assertEquals(0, count.get());
            assertEquals(0, finishedEvents.size());
            assertEquals(1, jobManager.findJobs(JobManager.QueryType.ALL, TOPIC + "/failed", -1, (Map<String, Object>[])null).size());
            assertEquals(0, jobManager.getStatistics().getNumberOfQueuedJobs());
            assertEquals(0, jobManager.getStatistics().getNumberOfActiveJobs());

        } finally {
            jcReg.unregister();
            ehReg.unregister();
        }
    }

    private static final class DataObject implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
