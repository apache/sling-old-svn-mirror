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
package org.apache.sling.discovery.base.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.sling.commons.testing.junit.categories.Slow;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.testing.tools.retry.RetryLoop;
import org.apache.sling.testing.tools.retry.RetryLoop.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This base tests for discovery.impl and .oak simulates instances
 * that are coming and going, have a slow repository (and other bad-weather
 * scenarios), and make assumptions on both getTopology() to always be correct
 * as well as handleTopologyEvent to be properly delivered to.
 */
public abstract class AbstractDiscoveryServiceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    class Tester implements TopologyEventListener, Runnable {
        
        private final VirtualInstance instance;
        private final Thread thread;

        private TopologyEvent previousEvent;
        
        private List<String> failures = new LinkedList<String>();
        
        private volatile boolean stopped = false;
        private final long pollingSleep;
        
        private boolean running = true;
        
        Tester(VirtualInstance instance, long pollingSleep) throws Throwable {
            this.instance = instance;
            this.pollingSleep = pollingSleep;
            instance.bindTopologyEventListener(this);
            thread = new Thread(this);
            thread.setName("Tester-"+instance.getDebugName()+"-thread");
            thread.setDaemon(true);
            thread.start();
        }

        private synchronized void assertNoFailures() {
            if (failures.size()==0) {
                return;
            }
            fail("got "+failures.size()+" failures, the first one thereof: "+failures.get(0));
        }
        
        private synchronized boolean hasFailures() {
            return failures.size()!=0;
        }
        
        private synchronized void asyncFail(String msg) {
            failures.add(msg);
        }

        @Override
        public void run() {
            while(!stopped) {
                try{
                    TopologyView topo = instance.getDiscoveryService().getTopology();
                    Thread.sleep(pollingSleep);
                } catch(Throwable th) {
                    asyncFail("Got a Throwable: "+th);
                    return;
                }
            }
        }

        @Override
        public void handleTopologyEvent(TopologyEvent event) {
            if (hasFailures()) {
                // then stop
                return;
            }
            logger.info("handleTopologyEvent["+instance.getDebugName()+"]: "+event);
            if (previousEvent == null) {
                // then we're expecting a TOPOLOGY_INIT
                if (event.getType()!=Type.TOPOLOGY_INIT) {
                    asyncFail("expected an INIT as the first, but got: "+event);
                    return;
                }
            } else if (previousEvent.getType() == Type.TOPOLOGY_CHANGED
                    || previousEvent.getType() == Type.PROPERTIES_CHANGED) {
                // then expecting a TOPOLOGY_CHANGING or PROPERTIES_CHANGED
                if (event.getType()==Type.TOPOLOGY_CHANGING) {
                    // perfect
                } else if (event.getType()==Type.PROPERTIES_CHANGED) {
                    // perfect
                } else {
                    asyncFail("expected a CHANGING or PROPERTIES_CHANGED, but got: "+event);
                    return;
                }
            } else if (previousEvent.getType() == Type.TOPOLOGY_CHANGING) {
                // then expecting a TOPOLOGY_CHANGED
                if (event.getType()!=Type.TOPOLOGY_CHANGED) {
                    asyncFail("expected a CHANGED after CHANGING, but got: "+event);
                    return;
                }
            }
            previousEvent = event;
        }

        public void shutdown() throws Exception {
            stopped = true;
            instance.stop();
            running = false;
        }

        public void restart() throws Exception {
            instance.startViewChecker((int) instance.getConfig().getConnectorPingInterval());
            running = true;
        }

        public void pause() throws Throwable {
            instance.stopViewChecker();
            running = false;
        }
        
    }
    
    List<Tester> testers = new LinkedList<Tester>();
    
    public abstract VirtualInstanceBuilder newBuilder();
    
    @Before
    public void setUp() throws Exception {
        testers.clear();
    }
    
    @After
    public void tearDown() throws Exception {
        for (Tester tester : testers) {
            tester.shutdown();
        }
    }
    
    Tester newInstance(String debugName, int interval, int timeout, long pollingSleep, VirtualInstance base) throws Throwable {
        VirtualInstanceBuilder builder = newBuilder();
        builder.setDebugName(debugName);
        if (base == null) {
            builder.newRepository("/var/discovery/testing/", true);
        } else {
            builder.useRepositoryOf(base);
        }
        builder.setConnectorPingInterval(interval);
        builder.setConnectorPingTimeout(timeout);
        builder.setMinEventDelay(1);
        VirtualInstance instance = builder.build();
        Tester t = new Tester(instance, pollingSleep);
        testers.add(t);
        instance.startViewChecker(interval);
        return t;
    }
    
    private void assertStableTopology(Tester... instances) {
        for (Tester tester : instances) {
            logger.info("asserting tester: "+tester.instance.getDebugName());
            TopologyEvent lastEvent = tester.previousEvent;
            Type type = lastEvent.getType();
            if (type == Type.TOPOLOGY_CHANGED || type == Type.TOPOLOGY_INIT) {
                // fine
            } else {
                fail("wrong type, expected CHANGED or INIT, got: "+type);
            }
            assertNotNull(lastEvent.getNewView());
            assertEquals(instances.length, lastEvent.getNewView().getInstances().size());
            TopologyView t = tester.instance.getDiscoveryService().getTopology();
            assertTrue(t.isCurrent());
            assertEquals(instances.length, t.getInstances().size());
        }
    }

    private boolean isStableTopology(Tester... instances) {
        for (Tester tester : instances) {
            TopologyEvent lastEvent = tester.previousEvent;
            if (lastEvent == null) {
                return false;
            }
            Type type = lastEvent.getType();
            if (type == Type.TOPOLOGY_CHANGED || type == Type.TOPOLOGY_INIT) {
                // fine
            } else {
                return false;
            }
            TopologyView newView = lastEvent.getNewView();
            if (newView == null) {
                return false;
            }
            if (instances.length != newView.getInstances().size()) {
                return false;
            }
            TopologyView t = tester.instance.getDiscoveryService().getTopology();
            if (!t.isCurrent()) {
                return false;
            }
            if (instances.length != t.getInstances().size()) {
                return false;
            }
            for (Tester t2 : instances) {
                boolean foundMatch = false;
                for (InstanceDescription id : t.getInstances()) {
                    if (t2.instance.getSlingId().equals(id.getSlingId())) {
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testSingleInstance() throws Throwable {
        logger.info("testSingleInstances: start");
        Tester single = newInstance("single", 1, 5, 50, null);
        single.instance.dumpRepo();
        logger.info("testSingleInstances: starting retry loop (10sec max)");
        startRetryLoop(testers, 10);
        single.assertNoFailures();
        assertStableTopology(single);
        logger.info("testSingleInstances: end");
    }
    
    @Test
    public void testTwoInstances() throws Throwable {
        logger.info("testTwoInstances: start");
        Tester i1 = newInstance("i1", 1, 10, 100, null);
        Tester i2 = newInstance("i2", 1, 10, 100, i1.instance);
        logger.info("testTwoInstances: starting retry loop (15sec max)");
        startRetryLoop(testers, 15);
        i1.instance.dumpRepo();
        i1.assertNoFailures();
        i2.assertNoFailures();
        assertStableTopology(i1, i2);
        logger.info("testTwoInstances: end");
    }

    @Test
    public void testFiveInstances() throws Throwable {
        logger.info("testFiveInstances: start");
        Tester i1 = newInstance("i1", 1, 30, 250, null);
        for(int i=2; i<=5; i++) {
            Tester in = newInstance("i"+i, 1, 30, 250, i1.instance);
        }
        logger.info("testFiveInstances: starting retry loop (40sec max)");
        startRetryLoop(testers, 40);
        i1.instance.dumpRepo();
        i1.assertNoFailures();
        assertStableTopology(testers.toArray(new Tester[0]));
        logger.info("testFiveInstances: end");
    }

    @Category(Slow.class) //TODO: this takes env 10sec
    @Test
    public void testTenInstances() throws Throwable {
        logger.info("testTenInstances: start");
        Tester i1 = newInstance("i1", 1, 30, 250, null);
        for(int i=2; i<=10; i++) {
            Tester in = newInstance("i"+i, 1, 30, 250, i1.instance);
        }
        logger.info("testTenInstances: starting retry loop (60sec max)");
        startRetryLoop(testers, 60);
        i1.instance.dumpRepo();
        i1.assertNoFailures();
        assertStableTopology(testers.toArray(new Tester[0]));
        logger.info("testTenInstances: end");
    }

    @Category(Slow.class) //TODO: this takes env 15sec
    @Test
    public void testTwentyInstances() throws Throwable {
        logger.info("testTwentyInstances: start");
        Tester i1 = newInstance("i1", 1, 60, 1000, null);
        for(int i=2; i<=20; i++) {
            Tester in = newInstance("i"+i, 1, 60, 1000, i1.instance);
        }
        logger.info("testThirtyInstances: starting retry loop (80 sec max)");
        startRetryLoop(testers, 80);
        i1.instance.dumpRepo();
        i1.assertNoFailures();
        assertStableTopology(testers.toArray(new Tester[0]));
        logger.info("testTwentyInstances: end");
    }

    @Category(Slow.class) //TODO: this takes env 40sec
    @Test
    public void testTwentyFourInstances() throws Throwable {
        logger.info("testTwentyFourInstances: start");
        Tester i1 = newInstance("i1", 4, 120, 1000, null);
        for(int i=2; i<=24; i++) {
            Tester in = newInstance("i"+i, 4, 120, 2000, i1.instance);
            Thread.sleep(1000);
        }
        logger.info("testTwentyFourInstances: starting retry loop (180sec max)");
        startRetryLoop(testers, 180);
        i1.instance.dumpRepo();
        i1.assertNoFailures();
        assertStableTopology(testers.toArray(new Tester[0]));
        logger.info("testTwentyFourInstances: end");
    }
    
    private void startRetryLoop(final List<Tester> testers, int retryTimeoutSeconds) {
        startRetryLoop(retryTimeoutSeconds, testers.toArray(new Tester[0]));
    }
    
    private void startRetryLoop(int retryTimeoutSeconds, final Tester... testers) {
        new RetryLoop(new Condition() {

            @Override
            public String getDescription() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean isTrue() throws Exception {
                if (!isStableTopology(testers)) {
                    return false;
                }
                for (Tester tester : testers) {
                    tester.assertNoFailures();
                }
                logger.info("retryLoop: declaring stable topology with "+testers.length);
                return true;
            }
            
        }, retryTimeoutSeconds /*seconds*/, 1000/*millis*/);        
    }
    
    @Category(Slow.class) //TODO: this takes env 120sec
    @Test
    public void testStartStopFiesta() throws Throwable {
        final Tester[] instances = new Tester[8];
        instances[0] = newInstance("i1", 1, 10, 1000, null);
        for(int i=2; i<=8; i++) {
            instances[i-1] = newInstance("i"+i, 1, 10, 1000, instances[0].instance);
            Thread.sleep(600);
        }
        startRetryLoop(15, instances);
        for (Tester tester : instances) {
            tester.assertNoFailures();
        }
        assertStableTopology(testers.toArray(new Tester[0]));
        Random r = new Random(123432141);
        for(int i=0; i<10; i++) {
            logger.info("testStartStopFiesta : loop "+i);
            final List<Tester> alive = new LinkedList<Tester>();
            int keepAlive = r.nextInt(instances.length);
            for(int j=0; j<instances.length; j++) {
                if (j == keepAlive || r.nextBoolean()) {
                    instances[j].restart();
                    alive.add(instances[j]);
                } else {
                    instances[j].pause();
                }
            }
            logger.info("testStartStopFiesta : loop "+i+", alive-cnt: "+alive.size());
            startRetryLoop(alive, 30);
            for (Tester tester : instances) {
                tester.assertNoFailures();
            }
            
            assertStableTopology(alive.toArray(new Tester[0]));
        }
    }
}
