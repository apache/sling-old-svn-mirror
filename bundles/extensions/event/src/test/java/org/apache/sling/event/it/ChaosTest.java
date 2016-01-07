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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ChaosTest extends AbstractJobHandlingTest {

    /** Duration for firing jobs in seconds. */
    private static final long DURATION = 4 * 60;

    private static final int NUM_ORDERED_THREADS = 3;
    private static final int NUM_PARALLEL_THREADS = 6;
    private static final int NUM_ROUND_THREADS = 6;

    private static final int NUM_ORDERED_TOPICS = 2;
    private static final int NUM_PARALLEL_TOPICS = 8;
    private static final int NUM_ROUND_TOPICS = 8;

    private static final String ORDERED_TOPIC_PREFIX = "sling/chaos/ordered/";
    private static final String PARALLEL_TOPIC_PREFIX = "sling/chaos/parallel/";
    private static final String ROUND_TOPIC_PREFIX = "sling/chaos/round/";

    private static final String[] ORDERED_TOPICS = new String[NUM_ORDERED_TOPICS];
    private static final String[] PARALLEL_TOPICS = new String[NUM_PARALLEL_TOPICS];
    private static final String[] ROUND_TOPICS = new String[NUM_ROUND_TOPICS];

    static {
        for(int i=0; i<NUM_ORDERED_TOPICS; i++) {
            ORDERED_TOPICS[i] = ORDERED_TOPIC_PREFIX + String.valueOf(i);
        }
        for(int i=0; i<NUM_PARALLEL_TOPICS; i++) {
            PARALLEL_TOPICS[i] = PARALLEL_TOPIC_PREFIX + String.valueOf(i);
        }
        for(int i=0; i<NUM_ROUND_TOPICS; i++) {
            ROUND_TOPICS[i] = ROUND_TOPIC_PREFIX + String.valueOf(i);
        }
    }

    private String orderedQueueConfPid;

    private String topicRRQueueConfPid;


    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        // create ordered test queue
        final org.osgi.service.cm.Configuration orderedConfig = this.configAdmin.createFactoryConfiguration("org.apache.sling.event.jobs.QueueConfiguration", null);
        final Dictionary<String, Object> orderedProps = new Hashtable<String, Object>();
        orderedProps.put(ConfigurationConstants.PROP_NAME, "chaos-ordered");
        orderedProps.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.ORDERED.name());
        orderedProps.put(ConfigurationConstants.PROP_TOPICS, ORDERED_TOPICS);
        orderedProps.put(ConfigurationConstants.PROP_RETRIES, 2);
        orderedProps.put(ConfigurationConstants.PROP_RETRY_DELAY, 2000L);
        orderedConfig.update(orderedProps);

        orderedQueueConfPid = orderedConfig.getPid();

        // create round robin test queue
        final org.osgi.service.cm.Configuration rrConfig = this.configAdmin.createFactoryConfiguration("org.apache.sling.event.jobs.QueueConfiguration", null);
        final Dictionary<String, Object> rrProps = new Hashtable<String, Object>();
        rrProps.put(ConfigurationConstants.PROP_NAME, "chaos-roundrobin");
        rrProps.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.TOPIC_ROUND_ROBIN.name());
        rrProps.put(ConfigurationConstants.PROP_TOPICS, ROUND_TOPICS);
        rrProps.put(ConfigurationConstants.PROP_RETRIES, 2);
        rrProps.put(ConfigurationConstants.PROP_RETRY_DELAY, 2000L);
        rrProps.put(ConfigurationConstants.PROP_MAX_PARALLEL, 5);
        rrConfig.update(rrProps);

        topicRRQueueConfPid = rrConfig.getPid();

        this.sleep(1000L);
    }

    @After
    public void cleanUp() throws IOException {
        this.removeConfiguration(this.orderedQueueConfPid);
        this.removeConfiguration(this.topicRRQueueConfPid);
        super.cleanup();
    }

    /**
     * Setup consumers
     */
    private void setupJobConsumers(final List<ServiceRegistration<?>> registrations) {
        for(int i=0; i<NUM_ORDERED_TOPICS; i++) {
            registrations.add(this.registerJobConsumer(ORDERED_TOPICS[i],

                new JobConsumer() {

                    @Override
                    public JobResult process(final Job job) {
                        return JobResult.OK;
                    }
                }));
        }
        for(int i=0; i<NUM_PARALLEL_TOPICS; i++) {
            registrations.add(this.registerJobConsumer(PARALLEL_TOPICS[i],

                new JobConsumer() {

                    @Override
                    public JobResult process(final Job job) {
                        return JobResult.OK;
                    }
                }));
        }
        for(int i=0; i<NUM_ROUND_TOPICS; i++) {
            registrations.add(this.registerJobConsumer(ROUND_TOPICS[i],

                new JobConsumer() {

                    @Override
                    public JobResult process(final Job job) {
                        return JobResult.OK;
                    }
                }));
        }
    }

    private static final class CreateJobThread extends Thread {

        private final String[] topics;

        private final JobManager jobManager;

        private final Random random = new Random();

        final Map<String, AtomicLong> created;

        final AtomicLong finishedThreads;

        public CreateJobThread(final JobManager jobManager,
                final String[] topics,
                final Map<String, AtomicLong> created,
                final AtomicLong finishedThreads) {
            this.topics = topics;
            this.jobManager = jobManager;
            this.created = created;
            this.finishedThreads = finishedThreads;
        }

        @Override
        public void run() {
            int index = 0;
            final long startTime = System.currentTimeMillis();
            final long endTime = startTime + DURATION * 1000;
            while ( System.currentTimeMillis() < endTime ) {
                final String topic = topics[index];
                if ( jobManager.addJob(topic, null) != null ) {
                    created.get(topic).incrementAndGet();

                    index++;
                    if ( index == topics.length ) {
                        index = 0;
                    }
                }
                final int sleepTime = random.nextInt(200);
                try {
                    Thread.sleep(sleepTime);
                } catch ( final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            finishedThreads.incrementAndGet();
        }

    }

    /**
     * Setup job creation threads
     */
    private void setupJobCreationThreads(final List<Thread> threads,
            final JobManager jobManager,
            final Map<String, AtomicLong> created,
            final AtomicLong finishedThreads) {
        for(int i=0;i<NUM_ORDERED_THREADS;i++) {
            threads.add(new CreateJobThread(jobManager, ORDERED_TOPICS, created, finishedThreads));
        }
        for(int i=0;i<NUM_PARALLEL_THREADS;i++) {
            threads.add(new CreateJobThread(jobManager, PARALLEL_TOPICS, created, finishedThreads));
        }
        for(int i=0;i<NUM_ROUND_THREADS;i++) {
            threads.add(new CreateJobThread(jobManager, ROUND_TOPICS, created, finishedThreads));
        }
    }

    /**
     * Setup chaos thread(s)
     *
     * Chaos is right now created by sending topology changing/changed events randomly
     */
    private void setupChaosThreads(final List<Thread> threads,
            final AtomicLong finishedThreads) {
        final List<TopologyView> views = new ArrayList<TopologyView>();
        // register topology listener
        final ServiceRegistration<TopologyEventListener> reg = this.bc.registerService(TopologyEventListener.class, new TopologyEventListener() {

            @Override
            public void handleTopologyEvent(final TopologyEvent event) {
                if ( event.getType() == Type.TOPOLOGY_INIT ) {
                    views.add(event.getNewView());
                }
            }
        }, null);
        while ( views.isEmpty() ) {
            this.sleep(10);
        }
        reg.unregister();
        final TopologyView view = views.get(0);

        try {
            final Collection<ServiceReference<TopologyEventListener>> refs = this.bc.getServiceReferences(TopologyEventListener.class, null);
            assertNotNull(refs);
            assertFalse(refs.isEmpty());
            TopologyEventListener found = null;
            for(final ServiceReference<TopologyEventListener> ref : refs) {
                final TopologyEventListener listener = this.bc.getService(ref);
                if ( listener != null && listener.getClass().getName().equals("org.apache.sling.event.impl.jobs.config.TopologyHandler") ) {
                    found = listener;
                    break;
                }
                bc.ungetService(ref);
            }
            assertNotNull(found);
            final TopologyEventListener tel = found;

            threads.add(new Thread() {

                private final Random random = new Random();

                @Override
                public void run() {
                    final long startTime = System.currentTimeMillis();
                    // this thread runs 30 seconds longer than the job creation thread
                    final long endTime = startTime + (DURATION +30) * 1000;
                    while ( System.currentTimeMillis() < endTime ) {
                        final int sleepTime = random.nextInt(25) + 15;
                        try {
                            Thread.sleep(sleepTime * 1000);
                        } catch ( final InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        tel.handleTopologyEvent(new TopologyEvent(Type.TOPOLOGY_CHANGING, view, null));
                        final int changingTime = random.nextInt(20) + 3;
                        try {
                            Thread.sleep(changingTime * 1000);
                        } catch ( final InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        tel.handleTopologyEvent(new TopologyEvent(Type.TOPOLOGY_CHANGED, view, view));
                    }
                    tel.getClass().getName();
                    finishedThreads.incrementAndGet();
                }
            });
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test(timeout=DURATION * 4000)
    public void testDoChaos() throws Exception {
        final JobManager jobManager = this.getJobManager();

        // setup added, created and finished map
        // added and finished are filled by notifications
        // created is filled by the threads starting jobs
        final Map<String, AtomicLong> added = new HashMap<String, AtomicLong>();
        final Map<String, AtomicLong> created = new HashMap<String, AtomicLong>();
        final Map<String, AtomicLong> finished = new HashMap<String, AtomicLong>();
        final List<String> topics = new ArrayList<String>();
        for(int i=0;i<NUM_ORDERED_TOPICS;i++) {
            added.put(ORDERED_TOPICS[i], new AtomicLong());
            created.put(ORDERED_TOPICS[i], new AtomicLong());
            finished.put(ORDERED_TOPICS[i], new AtomicLong());
            topics.add(ORDERED_TOPICS[i]);
        }
        for(int i=0;i<NUM_PARALLEL_TOPICS;i++) {
            added.put(PARALLEL_TOPICS[i], new AtomicLong());
            created.put(PARALLEL_TOPICS[i], new AtomicLong());
            finished.put(PARALLEL_TOPICS[i], new AtomicLong());
            topics.add(PARALLEL_TOPICS[i]);
        }
        for(int i=0;i<NUM_ROUND_TOPICS;i++) {
            added.put(ROUND_TOPICS[i], new AtomicLong());
            created.put(ROUND_TOPICS[i], new AtomicLong());
            finished.put(ROUND_TOPICS[i], new AtomicLong());
            topics.add(ROUND_TOPICS[i]);
        }

        final List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
        final List<Thread> threads = new ArrayList<Thread>();
        final AtomicLong finishedThreads = new AtomicLong();

        final ServiceRegistration<EventHandler> eventHandler = this.registerEventHandler("org/apache/sling/event/notification/job/*",
                new EventHandler() {

                    @Override
                    public void handleEvent(final Event event) {
                        final String topic = (String) event.getProperty(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC);
                        if ( NotificationConstants.TOPIC_JOB_FINISHED.equals(event.getTopic())) {
                            finished.get(topic).incrementAndGet();
                        } else if ( NotificationConstants.TOPIC_JOB_ADDED.equals(event.getTopic())) {
                            added.get(topic).incrementAndGet();
                        }
                    }
                });
        try {
            // setup job consumers
            this.setupJobConsumers(registrations);

            // setup job creation tests
            this.setupJobCreationThreads(threads, jobManager, created, finishedThreads);

            this.setupChaosThreads(threads, finishedThreads);

            System.out.println("Starting threads...");
            // start threads
            for(final Thread t : threads) {
                t.setDaemon(true);
                t.start();
            }

            System.out.println("Sleeping for " + DURATION + " seconds to wait for threads to finish...");
            // for sure we can sleep for the duration
            this.sleep(DURATION * 1000);

            System.out.println("Polling for threads to finish...");
            // wait until threads are finished
            while ( finishedThreads.get() < threads.size() ) {
                this.sleep(100);
            }

            System.out.println("Waiting for job handling to finish...");
            final Set<String> allTopics = new HashSet<String>(topics);
            while ( !allTopics.isEmpty() ) {
                final Iterator<String> iter = allTopics.iterator();
                while ( iter.hasNext() ) {
                    final String topic = iter.next();
                    if ( finished.get(topic).get() == created.get(topic).get() ) {
                        iter.remove();
                    }
                }
                this.sleep(100);
            }
/* We could try to enable this with Oak again - but right now JR observation handler is too
 * slow.
            System.out.println("Checking notifications...");
            for(final String topic : topics) {
                assertEquals("Checking topic " + topic, created.get(topic).get(), added.get(topic).get());
            }
 */

        } finally {
            eventHandler.unregister();
            for(final ServiceRegistration<?> reg : registrations) {
                reg.unregister();
            }
        }

    }
}
