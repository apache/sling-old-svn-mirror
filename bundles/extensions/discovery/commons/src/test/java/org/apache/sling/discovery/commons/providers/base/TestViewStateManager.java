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
package org.apache.sling.discovery.commons.providers.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.commons.testing.junit.categories.Slow;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.DummyTopologyView;
import org.apache.sling.discovery.commons.providers.EventHelper;
import org.apache.sling.discovery.commons.providers.spi.ClusterSyncService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestViewStateManager {

    protected static final Logger logger = LoggerFactory.getLogger(TestViewStateManager.class);

    class ClusterSyncServiceWithSemaphore implements ClusterSyncService {

        private final Semaphore semaphore;
        private final Lock lock;

        public ClusterSyncServiceWithSemaphore(Lock lock, Semaphore semaphore) {
            this.lock = lock;
            this.semaphore = semaphore;
        }
        
        public void sync(BaseTopologyView view, Runnable callback) {
            try {
                lock.unlock();
                try{
                    logger.info("ClusterSyncServiceWithSemaphore.sync: acquiring lock ...");
                    semaphore.acquire();
                    logger.info("ClusterSyncServiceWithSemaphore.sync: lock acquired.");
                } finally {
                    lock.lock();
                }
                callback.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void cancelSync() {
            // TODO not implemented yet
        }
        
    }
    
    protected ViewStateManagerImpl mgr;
    
    private Random defaultRandom;

    private Level logLevel;

    @Before
    public void setup() throws Exception {
        mgr = new ViewStateManagerImpl(new ReentrantLock(), new ClusterSyncService() {
            
            public void sync(BaseTopologyView view, Runnable callback) {
                callback.run();
            }
            
            @Override
            public void cancelSync() {
                // nothing to cancel, we're auto-run
            }
        });
        defaultRandom = new Random(1234123412); // I want randomness yes, but deterministic, for some methods at least
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.INFO);
    }
    
    @After
    public void teardown() throws Exception {
        if (mgr != null) {
            // release any async event sender ..
            mgr.handleDeactivated();
        }
        mgr = null;
        defaultRandom= null;
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);
    }
    
    void assertEvents(DummyListener listener, TopologyEvent... events) {
        TestHelper.assertEvents(mgr, listener, events);
    }
    
    /** does couple loops randomly calling handleChanging() (or not) and then handleNewView().
     * Note: random is passed to allow customizing and not hardcoding this method to a particular random 
     * @throws InterruptedException **/
    protected void randomEventLoop(final Random random, DummyListener... listeners) throws InterruptedException {
        TestHelper.randomEventLoop(mgr, null, 5, -1, random, listeners);
    }
    
    @Test
    public void testChangedPropertiesChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.installMinEventDelayHandler(new DiscoveryService() {
            
            @Override
            public TopologyView getTopology() {
                throw new IllegalStateException("not yet impl");
            }
        }, new DummyScheduler(), 1);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new DummyTopologyView().addInstance();
        InstanceDescription instance1 = view1.getInstances().iterator().next();
        ClusterView cluster1 = instance1.getClusterView();
        mgr.handleNewView(view1);
        assertEvents(listener, EventHelper.newInitEvent(view1));
        DefaultClusterView cluster2 = new DefaultClusterView(new String(cluster1.getId()));
        final BaseTopologyView view2 = new DummyTopologyView(view1.getLocalClusterSyncTokenId()).addInstance(instance1.getSlingId(), cluster2, instance1.isLeader(), instance1.isLocal());
        DefaultInstanceDescription instance2 = (DefaultInstanceDescription) view2.getLocalInstance();
        instance2.setProperty("foo", "bar");
        mgr.handleNewView(view2);
        assertEvents(listener, EventHelper.newPropertiesChangedEvent(view1, view2));
    }

    @Test
    public void testDuplicateListeners() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        mgr.bind(listener); // we should be generous and allow duplicate registration
        assertTrue(mgr.unbind(listener));
        assertFalse(mgr.unbind(listener));
        
        mgr.handleActivated();
        assertFalse(mgr.unbind(listener));
        mgr.bind(listener);
        mgr.bind(listener); // we should be generous and allow duplicate registration
        assertTrue(mgr.unbind(listener));
        assertFalse(mgr.unbind(listener));
    }
    
    @Test
    public void testBindActivateChangingChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventHelper.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingActivateChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventHelper.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingChangedActivate() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        assertEvents(listener, EventHelper.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingChangedChangingActivate() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventHelper.newInitEvent(view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangedChangingActivate() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventHelper.newInitEvent(view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testCancelSync() throws Exception {
        final List<Runnable> syncCallbacks = new LinkedList<Runnable>();
        mgr = new ViewStateManagerImpl(new ReentrantLock(), new ClusterSyncService() {
            
            public void sync(BaseTopologyView view, Runnable callback) {
                synchronized(syncCallbacks) {
                    syncCallbacks.add(callback);
                }
            }
            
            @Override
            public void cancelSync() {
                synchronized(syncCallbacks) {
                    syncCallbacks.clear();
                }
            }
        });
        mgr.handleActivated();
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        mgr.handleChanging();
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEquals(0, mgr.waitForAsyncEvents(1000));
        TestHelper.assertNoEvents(listener);
        synchronized(syncCallbacks) {
            assertEquals(1, syncCallbacks.size());
        }
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        final BaseTopologyView view2 = TestHelper.newView(true, id1, id1, id1, id2); 
        mgr.handleNewView(view2);
        assertEquals(0, mgr.waitForAsyncEvents(1000));
        TestHelper.assertNoEvents(listener);
        synchronized(syncCallbacks) {
            assertEquals(1, syncCallbacks.size());
            syncCallbacks.get(0).run();
            syncCallbacks.clear();
        }
        assertEquals(0, mgr.waitForAsyncEvents(1000));
        assertEvents(listener, EventHelper.newInitEvent(view2));
    }
    
    @Test
    public void testActivateBindChangingChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        // first activate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener); // paranoia
        // then bind
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener); // there was no changing or changed yet
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventHelper.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }

    @Test
    public void testPropertiesChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.handleActivated();
        mgr.bind(listener);
        mgr.handleChanging();
        DummyTopologyView oldView = new DummyTopologyView().addInstance();
        DefaultInstanceDescription localInstance = 
                (DefaultInstanceDescription) oldView.getLocalInstance();
        localInstance.setProperty("foo", "bar1");
        mgr.handleNewView(oldView);
        TopologyEvent initEvent = EventHelper.newInitEvent(oldView.clone());
        assertEvents(listener, initEvent);
        DummyTopologyView newView = oldView.clone();
        oldView.setNotCurrent();
        localInstance = (DefaultInstanceDescription) newView.getLocalInstance();
        localInstance.setProperty("foo", "bar2");
        mgr.handleNewView(newView);
        Thread.sleep(2000);
        TopologyEvent propertiesChangedEvent = EventHelper.newPropertiesChangedEvent(oldView.clone(), newView.clone());
        assertEvents(listener, propertiesChangedEvent);
    }

    @Test
    public void testActivateChangingBindChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        // first activate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener); // paranoia
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener); // no listener yet
        // then bind
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener); // no changed event yet
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventHelper.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }

    @Test
    public void testActivateChangingChangedBind() throws Exception {
        final DummyListener listener = new DummyListener();
        // first activate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener); // paranoia
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener); // no listener yet
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener); // no listener yet
        // then bind
        mgr.bind(listener);
        assertEvents(listener, EventHelper.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateBindChangingChanged() throws Exception {
        final DummyListener listener1 = new DummyListener();
        final DummyListener listener2 = new DummyListener();
        
        mgr.bind(listener1);
        TestHelper.assertNoEvents(listener1);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener1);
        mgr.bind(listener2);
        TestHelper.assertNoEvents(listener1);
        TestHelper.assertNoEvents(listener2);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener1);
        TestHelper.assertNoEvents(listener2);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, EventHelper.newInitEvent(view));
        assertEvents(listener2, EventHelper.newInitEvent(view));
        
        randomEventLoop(defaultRandom, listener1, listener2);
    }

    @Test
    public void testBindActivateChangingBindChanged() throws Exception {
        final DummyListener listener1 = new DummyListener();
        final DummyListener listener2 = new DummyListener();
        
        mgr.bind(listener1);
        TestHelper.assertNoEvents(listener1);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener1);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener1);
        mgr.bind(listener2);
        TestHelper.assertNoEvents(listener1);
        TestHelper.assertNoEvents(listener2);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, EventHelper.newInitEvent(view));
        assertEvents(listener2, EventHelper.newInitEvent(view));

        randomEventLoop(defaultRandom, listener1, listener2);
    }
    
    @Test
    public void testActivateBindChangingDuplicateHandleNewView() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.handleActivated();
        mgr.bind(listener);
        mgr.handleChanging();
        final DummyTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventHelper.newInitEvent(view));
        mgr.handleNewView(DummyTopologyView.clone(view));
        TestHelper.assertNoEvents(listener);
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testActivateBindChangingChangedBindDuplicateHandleNewView() throws Exception {
        final DummyListener listener1 = new DummyListener();
        mgr.handleActivated();
        mgr.bind(listener1);
        mgr.handleChanging();
        final DummyTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, EventHelper.newInitEvent(view));
        
        final DummyListener listener2 = new DummyListener();
        mgr.bind(listener2);
        mgr.handleNewView(DummyTopologyView.clone(view));
        TestHelper.assertNoEvents(listener1);
        assertEvents(listener2, EventHelper.newInitEvent(view));
        randomEventLoop(defaultRandom, listener1, listener2);
    }
    
    @Test
    public void testActivateChangedBindDuplicateHandleNewView() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final DummyTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener);
        assertEvents(listener, EventHelper.newInitEvent(view));
        mgr.handleNewView(DummyTopologyView.clone(view));
        TestHelper.assertNoEvents(listener);
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateChangedChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, EventHelper.newInitEvent(view1));
        final BaseTopologyView view2 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventHelper.newChangingEvent(view1), EventHelper.newChangedEvent(view1, view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateChangedDeactivateChangingActivateChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, EventHelper.newInitEvent(view1));
        mgr.handleDeactivated();
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventHelper.newInitEvent(view2));
    }

    @Test
    public void testBindActivateChangedDeactivateChangedActivateChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, EventHelper.newInitEvent(view1));
        mgr.handleDeactivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view2);
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        assertEvents(listener, EventHelper.newInitEvent(view2));
        final BaseTopologyView view3 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view3);
        assertEvents(listener, EventHelper.newChangingEvent(view2), EventHelper.newChangedEvent(view2, view3));
    }

    @Test
    public void testBindActivateChangedChangingDeactivateActivateChangingChanged() throws Exception {
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, EventHelper.newInitEvent(view1));
        mgr.handleChanging();
        assertEvents(listener, EventHelper.newChangingEvent(view1));
        mgr.handleDeactivated();
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new DummyTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventHelper.newInitEvent(view2));
    }
    
    @Test
    public void testClusterSyncService_noConcurrency() throws Exception {
        final org.apache.log4j.Logger commonsLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery.commons.providers");
        final org.apache.log4j.Level logLevel = commonsLogger.getLevel();
        commonsLogger.setLevel(Level.INFO); // change here to DEBUG in case of issues with this test
        final Semaphore serviceSemaphore = new Semaphore(0);
        final ReentrantLock lock = new ReentrantLock();
        final ClusterSyncServiceWithSemaphore cs = new ClusterSyncServiceWithSemaphore(lock, serviceSemaphore );
        mgr = new ViewStateManagerImpl(lock, cs);
        final DummyListener listener = new DummyListener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final String slingId1 = UUID.randomUUID().toString();
        final String slingId2 = UUID.randomUUID().toString();
        final String clusterId = UUID.randomUUID().toString();
        final DefaultClusterView cluster = new DefaultClusterView(clusterId);
        final DummyTopologyView view1 = new DummyTopologyView()
                .addInstance(slingId1, cluster, true, true)
                .addInstance(slingId2, cluster, false, false);
        async(new Runnable() {

            public void run() {
                mgr.handleNewView(view1);
            }
            
        });
        Thread.sleep(1000);
        TestHelper.assertNoEvents(listener);
        serviceSemaphore.release(1);
        Thread.sleep(1000);
        assertEvents(listener, EventHelper.newInitEvent(view1));
        final DummyTopologyView view2 = view1.clone();
        mgr.handleChanging();
        assertEvents(listener, EventHelper.newChangingEvent(view1));
        view2.removeInstance(slingId2);
        async(new Runnable() {

            public void run() {
                mgr.handleNewView(view2);
            }
            
        });
        logger.debug("run: waiting for 1sec");
        Thread.sleep(1000);
        logger.debug("run: asserting no events");
        TestHelper.assertNoEvents(listener);
        logger.debug("run: releasing consistencyService");
        serviceSemaphore.release(1);
        logger.debug("run: waiting 1sec");
        Thread.sleep(1000);
        logger.debug("run: asserting 1 event");
        assertEvents(listener, EventHelper.newChangedEvent(view1, view2));
        commonsLogger.setLevel(Level.INFO); // back to default
    }

    protected void async(Runnable runnable) {
        new Thread(runnable).start();
    }

    @Test
    public void testOnlyDiffersInProperties() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(Level.INFO); // changed from Level.DEBUG
        logger.info("testOnlyDiffersInProperties: start");
        final String slingId1 = UUID.randomUUID().toString();
        final String slingId2 = UUID.randomUUID().toString();
        final String slingId3 = UUID.randomUUID().toString();
        final String clusterId = UUID.randomUUID().toString();
        final DefaultClusterView cluster = new DefaultClusterView(clusterId);
        final DummyTopologyView view1 = new DummyTopologyView()
                .addInstance(slingId1, cluster, true, true)
                .addInstance(slingId2, cluster, false, false)
                .addInstance(slingId3, cluster, false, false);
        final DummyTopologyView view2 = DummyTopologyView.clone(view1).removeInstance(slingId2);
        final DummyTopologyView view3 = DummyTopologyView.clone(view1).removeInstance(slingId2).removeInstance(slingId3);
        DummyTopologyView view1Cloned = DummyTopologyView.clone(view1);
        
        logger.info("testOnlyDiffersInProperties: handleNewView(view1)");
        mgr.handleNewView(view1);
        logger.info("testOnlyDiffersInProperties: handleActivated()");
        mgr.handleActivated();
        assertEquals(0, mgr.waitForAsyncEvents(5000));
        assertFalse(mgr.onlyDiffersInProperties(view1));
        assertFalse(mgr.onlyDiffersInProperties(view2));
        assertFalse(mgr.onlyDiffersInProperties(view3));
        logger.info("testOnlyDiffersInProperties: handleNewView(view2)");
        mgr.handleNewView(view2);
        assertEquals(0, mgr.waitForAsyncEvents(5000));
        assertFalse(mgr.onlyDiffersInProperties(view1));
        assertFalse(mgr.onlyDiffersInProperties(view2));
        assertFalse(mgr.onlyDiffersInProperties(view3));
        logger.info("testOnlyDiffersInProperties: handleNewView(view3)");
        mgr.handleNewView(view3);
        assertEquals(0, mgr.waitForAsyncEvents(5000));
        assertFalse(mgr.onlyDiffersInProperties(view1));
        assertFalse(mgr.onlyDiffersInProperties(view2));
        assertFalse(mgr.onlyDiffersInProperties(view3));

        final DummyTopologyView view4 = DummyTopologyView.clone(view1Cloned);
        final DummyTopologyView view5 = DummyTopologyView.clone(view1Cloned);
        final DummyTopologyView view6 = DummyTopologyView.clone(view1Cloned);
        logger.info("testOnlyDiffersInProperties: handleNewView(view1cloned)");
        mgr.handleNewView(view1Cloned);
        assertEquals(0, mgr.waitForAsyncEvents(5000));
        DefaultInstanceDescription i4_1 = (DefaultInstanceDescription) view4.getInstance(slingId1);
        i4_1.setProperty("a", "b");
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view4)");
        assertTrue(mgr.onlyDiffersInProperties(view4));
    
        DefaultInstanceDescription i5_1 = (DefaultInstanceDescription) view5.getInstance(slingId1);
        i5_1.setProperty("a", "b");
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view5)");
        assertTrue(mgr.onlyDiffersInProperties(view5));
        DummyTopologyView view4Cloned = DummyTopologyView.clone(view4);
        mgr.handleNewView(view4);
        assertEquals(0, mgr.waitForAsyncEvents(5000));
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view4Cloned)");
        assertFalse(mgr.onlyDiffersInProperties(view4Cloned));
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view5)");
        assertFalse(mgr.onlyDiffersInProperties(view5));

        DefaultInstanceDescription i6_1 = (DefaultInstanceDescription) view6.getInstance(slingId1);
        i6_1.setProperty("a", "c");
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view6)");
        assertTrue(mgr.onlyDiffersInProperties(view6));
        String originalId = view6.getLocalClusterSyncTokenId();
        view6.setId(UUID.randomUUID().toString());
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view6) [2]");
        assertFalse(mgr.onlyDiffersInProperties(view6));
        view6.setId(originalId);
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view6) [3]");
        assertTrue(mgr.onlyDiffersInProperties(view6));
        view6.setId(null);
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view6) [4]");
        assertFalse(mgr.onlyDiffersInProperties(view6));
        view6.setId(originalId);
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view6) [5]");
        assertTrue(mgr.onlyDiffersInProperties(view6));
        
        // hack: we're modifying the view *in the ViewStateManagerImpl* here!!:
        view4.setId(null);

        view6.setId(null);
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view6) [6]");
        assertTrue(mgr.onlyDiffersInProperties(view6));
        view6.setId(originalId);
        logger.info("testOnlyDiffersInProperties: onlyDiffersInProperties(view6) [7]");
        assertFalse(mgr.onlyDiffersInProperties(view6));
    }

}