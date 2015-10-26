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
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.DummyTopologyView;
import org.apache.sling.discovery.commons.providers.EventHelper;
import org.apache.sling.discovery.commons.providers.spi.ClusterSyncService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestViewStateManager {

    private static final Logger logger = LoggerFactory.getLogger(TestViewStateManager.class);

    private class ClusterSyncServiceWithSemaphore implements ClusterSyncService {

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
    
    private ViewStateManagerImpl mgr;
    
    private Random defaultRandom;

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
    }
    
    @After
    public void teardown() throws Exception {
        if (mgr != null) {
            // release any async event sender ..
            mgr.handleDeactivated();
        }
        mgr = null;
        defaultRandom= null;
    }
    
    void assertEvents(DummyListener listener, TopologyEvent... events) {
        TestHelper.assertEvents(mgr, listener, events);
    }
    
    /** does couple loops randomly calling handleChanging() (or not) and then handleNewView().
     * Note: random is passed to allow customizing and not hardcoding this method to a particular random 
     * @throws InterruptedException **/
    private void randomEventLoop(final Random random, DummyListener... listeners) throws InterruptedException {
        TestHelper.randomEventLoop(mgr, null, 100, -1, random, listeners);
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

    private void async(Runnable runnable) {
        new Thread(runnable).start();
    }

    @Test
    public void testClusterSyncService_withConcurrency() throws Exception {
        final org.apache.log4j.Logger commonsLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery.commons.providers");
        final org.apache.log4j.Level logLevel = commonsLogger.getLevel();
        commonsLogger.setLevel(Level.INFO); // change here to DEBUG in case of issues with this test
        final Semaphore serviceSemaphore = new Semaphore(0);
        final Semaphore testSemaphore = new Semaphore(0);
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
        final String slingId3 = UUID.randomUUID().toString();
        final String clusterId = UUID.randomUUID().toString();
        final DefaultClusterView cluster = new DefaultClusterView(clusterId);
        final DummyTopologyView view1 = new DummyTopologyView()
                .addInstance(slingId1, cluster, true, true)
                .addInstance(slingId2, cluster, false, false)
                .addInstance(slingId3, cluster, false, false);
        final DummyTopologyView view2 = DummyTopologyView.clone(view1).removeInstance(slingId2);
        final DummyTopologyView view3 = DummyTopologyView.clone(view1).removeInstance(slingId2).removeInstance(slingId3);
        async(new Runnable() {

            public void run() {
                mgr.handleNewView(view1);
            }
            
        });
        Thread.sleep(1000);
        TestHelper.assertNoEvents(listener);
        assertEquals("should have one thread now waiting", 1, serviceSemaphore.getQueueLength());
        serviceSemaphore.release(1); // release the first one only
        Thread.sleep(1000);
        assertEvents(listener, EventHelper.newInitEvent(view1));
        mgr.handleChanging();
        assertEquals(0, mgr.waitForAsyncEvents(500));
        assertEvents(listener, EventHelper.newChangingEvent(view1));
        async(new Runnable() {

            public void run() {
                mgr.handleNewView(view2);
            }
            
        });
        logger.debug("run: waiting 1sec");
        Thread.sleep(1000);
        logger.debug("run: asserting no events");
        TestHelper.assertNoEvents(listener);
        assertEquals("should have one thread now waiting", 1, serviceSemaphore.getQueueLength());
        assertFalse("should not be locked", lock.isLocked());

        logger.debug("run: issuing a second event");
        // before releasing, issue another event, lets do a combination of changing/changed
        async(new Runnable() {

            public void run() {
                logger.debug("run2: calling handleChanging...");
                mgr.handleChanging();
                try {
                    logger.debug("run2: done with handleChanging, acquiring testSemaphore...");
                    testSemaphore.acquire();
                    logger.debug("run2: calling handleNewView...");
                    mgr.handleNewView(view3);
                    logger.debug("run2: done with handleNewView...");
                } catch (InterruptedException e) {
                    // fail
                    logger.error("interrupted: "+e, e);
                }
            }
            
        });
        logger.debug("run: waiting 1sec");
        Thread.sleep(1000);
        int remainingAsyncEvents = mgr.waitForAsyncEvents(2000);
        logger.info("run: result of waitForAsyncEvent is: "+remainingAsyncEvents);
        assertEquals("should have one thread now waiting", 1, serviceSemaphore.getQueueLength());
        assertEquals("should be acquiring (by thread2)", 1, testSemaphore.getQueueLength());
        // releasing the testSemaphore
        testSemaphore.release();
        logger.debug("run: waiting 1sec");
        Thread.sleep(1000);
        assertEquals("should have two async events now in the queue or being sent", 2, mgr.waitForAsyncEvents(500));
        assertEquals("but should only have 1 thread actually sitting on the semaphore waiting", 1, serviceSemaphore.getQueueLength());
        logger.debug("run: releasing consistencyService");
        serviceSemaphore.release(1); // release the first one only
        logger.debug("run: waiting 1sec");
        Thread.sleep(1000);
        assertFalse("should not be locked", lock.isLocked());
        TestHelper.assertNoEvents(listener); // this should not have triggered any event 
        serviceSemaphore.release(1); // then release the 2nd one
        logger.debug("run: waiting 1sec");
        Thread.sleep(1000);
        logger.debug("run: asserting 1 event");
        final TopologyEvent changedEvent = EventHelper.newChangedEvent(view1, view3);
        assertEvents(listener, changedEvent);
        commonsLogger.setLevel(Level.INFO); // back to default
    }

}
