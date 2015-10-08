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
package org.apache.sling.discovery.commons.providers.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.EventFactory;
import org.apache.sling.discovery.commons.providers.spi.ConsistencyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestViewStateManager {

    private static final Logger logger = LoggerFactory.getLogger(TestViewStateManager.class);

    private class ConsistencyServiceWithSemaphore implements ConsistencyService {

        private final Semaphore semaphore;
        private final Lock lock;

        public ConsistencyServiceWithSemaphore(Lock lock, Semaphore semaphore) {
            this.lock = lock;
            this.semaphore = semaphore;
        }
        
        public void sync(BaseTopologyView view, Runnable callback) {
            try {
                lock.unlock();
                try{
                    semaphore.acquire();
                } finally {
                    lock.lock();
                }
                callback.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
    }
    
    private ViewStateManagerImpl mgr;
    
    private Random defaultRandom;

    @Before
    public void setup() throws Exception {
        mgr = new ViewStateManagerImpl(new ReentrantLock(), new ConsistencyService() {
            
            public void sync(BaseTopologyView view, Runnable callback) {
                callback.run();
            }
        });
        defaultRandom = new Random(1234123412); // I want randomness yes, but deterministic, for some methods at least
    }
    
    @After
    public void teardown() throws Exception {
        mgr = null;
        defaultRandom= null;
    }
    
    void assertEvents(Listener listener, TopologyEvent... events) {
        TestHelper.assertEvents(mgr, listener, events);
    }
    
    /** does couple loops randomly calling handleChanging() (or not) and then handleNewView().
     * Note: random is passed to allow customizing and not hardcoding this method to a particular random 
     * @throws InterruptedException **/
    private void randomEventLoop(final Random random, Listener... listeners) throws InterruptedException {
        TestHelper.randomEventLoop(mgr, null, 100, -1, random, listeners);
    }
    
    @Test
    public void testDuplicateListeners() throws Exception {
        final Listener listener = new Listener();
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
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventFactory.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingActivateChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventFactory.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingChangedActivate() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        assertEvents(listener, EventFactory.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingChangedChangingActivate() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventFactory.newInitEvent(view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangedChangingActivate() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventFactory.newInitEvent(view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testActivateBindChangingChanged() throws Exception {
        final Listener listener = new Listener();
        // first activate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener); // paranoia
        // then bind
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener); // there was no changing or changed yet
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventFactory.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }

    @Test
    public void testPropertiesChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.handleActivated();
        mgr.bind(listener);
        mgr.handleChanging();
        SimpleTopologyView oldView = new SimpleTopologyView().addInstance();
        SimpleInstanceDescription localInstance = 
                (SimpleInstanceDescription) oldView.getLocalInstance();
        localInstance.setProperty("foo", "bar1");
        mgr.handleNewView(oldView);
        TopologyEvent initEvent = EventFactory.newInitEvent(oldView.clone());
        assertEvents(listener, initEvent);
        SimpleTopologyView newView = oldView.clone();
        oldView.setNotCurrent();
        localInstance = (SimpleInstanceDescription) newView.getLocalInstance();
        localInstance.setProperty("foo", "bar2");
        mgr.handleNewView(newView);
        Thread.sleep(2000);
        TopologyEvent propertiesChangedEvent = EventFactory.newPropertiesChangedEvent(oldView.clone(), newView.clone());
        assertEvents(listener, propertiesChangedEvent);
    }

    @Test
    public void testActivateChangingBindChanged() throws Exception {
        final Listener listener = new Listener();
        // first activate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener); // paranoia
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener); // no listener yet
        // then bind
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener); // no changed event yet
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventFactory.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }

    @Test
    public void testActivateChangingChangedBind() throws Exception {
        final Listener listener = new Listener();
        // first activate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener); // paranoia
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener); // no listener yet
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener); // no listener yet
        // then bind
        mgr.bind(listener);
        assertEvents(listener, EventFactory.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateBindChangingChanged() throws Exception {
        final Listener listener1 = new Listener();
        final Listener listener2 = new Listener();
        
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
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, EventFactory.newInitEvent(view));
        assertEvents(listener2, EventFactory.newInitEvent(view));
        
        randomEventLoop(defaultRandom, listener1, listener2);
    }

    @Test
    public void testBindActivateChangingBindChanged() throws Exception {
        final Listener listener1 = new Listener();
        final Listener listener2 = new Listener();
        
        mgr.bind(listener1);
        TestHelper.assertNoEvents(listener1);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener1);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener1);
        mgr.bind(listener2);
        TestHelper.assertNoEvents(listener1);
        TestHelper.assertNoEvents(listener2);
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, EventFactory.newInitEvent(view));
        assertEvents(listener2, EventFactory.newInitEvent(view));

        randomEventLoop(defaultRandom, listener1, listener2);
    }
    
    @Test
    public void testActivateBindChangingDuplicateHandleNewView() throws Exception {
        final Listener listener = new Listener();
        mgr.handleActivated();
        mgr.bind(listener);
        mgr.handleChanging();
        final SimpleTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, EventFactory.newInitEvent(view));
        mgr.handleNewView(SimpleTopologyView.clone(view));
        TestHelper.assertNoEvents(listener);
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testActivateBindChangingChangedBindDuplicateHandleNewView() throws Exception {
        final Listener listener1 = new Listener();
        mgr.handleActivated();
        mgr.bind(listener1);
        mgr.handleChanging();
        final SimpleTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, EventFactory.newInitEvent(view));
        
        final Listener listener2 = new Listener();
        mgr.bind(listener2);
        mgr.handleNewView(SimpleTopologyView.clone(view));
        TestHelper.assertNoEvents(listener1);
        assertEvents(listener2, EventFactory.newInitEvent(view));
        randomEventLoop(defaultRandom, listener1, listener2);
    }
    
    @Test
    public void testActivateChangedBindDuplicateHandleNewView() throws Exception {
        final Listener listener = new Listener();
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final SimpleTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener);
        assertEvents(listener, EventFactory.newInitEvent(view));
        mgr.handleNewView(SimpleTopologyView.clone(view));
        TestHelper.assertNoEvents(listener);
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateChangedChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, EventFactory.newInitEvent(view1));
        final BaseTopologyView view2 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventFactory.newChangingEvent(view1), EventFactory.newChangedEvent(view1, view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateChangedDeactivateChangingActivateChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, EventFactory.newInitEvent(view1));
        mgr.handleDeactivated();
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventFactory.newInitEvent(view2));
    }

    @Test
    public void testBindActivateChangedDeactivateChangedActivateChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, EventFactory.newInitEvent(view1));
        mgr.handleDeactivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view2);
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        assertEvents(listener, EventFactory.newInitEvent(view2));
        final BaseTopologyView view3 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view3);
        assertEvents(listener, EventFactory.newChangingEvent(view2), EventFactory.newChangedEvent(view2, view3));
    }

    @Test
    public void testBindActivateChangedChangingDeactivateActivateChangingChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view1 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, EventFactory.newInitEvent(view1));
        mgr.handleChanging();
        assertEvents(listener, EventFactory.newChangingEvent(view1));
        mgr.handleDeactivated();
        TestHelper.assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        mgr.handleChanging();
        TestHelper.assertNoEvents(listener);
        final BaseTopologyView view2 = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, EventFactory.newInitEvent(view2));
    }
    
    @Test
    public void testConsistencyService_noConcurrency() throws Exception {
        final org.apache.log4j.Logger commonsLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery.commons.providers");
        final org.apache.log4j.Level logLevel = commonsLogger.getLevel();
        commonsLogger.setLevel(Level.INFO); // change here to DEBUG in case of issues with this test
        final Semaphore serviceSemaphore = new Semaphore(0);
        final ReentrantLock lock = new ReentrantLock();
        final ConsistencyServiceWithSemaphore cs = new ConsistencyServiceWithSemaphore(lock, serviceSemaphore );
        mgr = new ViewStateManagerImpl(lock, cs);
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final String slingId1 = UUID.randomUUID().toString();
        final String slingId2 = UUID.randomUUID().toString();
        final String clusterId = UUID.randomUUID().toString();
        final SimpleClusterView cluster = new SimpleClusterView(clusterId);
        final SimpleTopologyView view1 = new SimpleTopologyView()
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
        assertEvents(listener, EventFactory.newInitEvent(view1));
        final SimpleTopologyView view2 = view1.clone();
        mgr.handleChanging();
        assertEvents(listener, EventFactory.newChangingEvent(view1));
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
        assertEvents(listener, EventFactory.newChangedEvent(view1, view2));
        commonsLogger.setLevel(Level.INFO); // back to default
    }

    private void async(Runnable runnable) {
        new Thread(runnable).start();
    }

    @Test
    public void testConsistencyService_withConcurrency() throws Exception {
        final org.apache.log4j.Logger commonsLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery.commons.providers");
        final org.apache.log4j.Level logLevel = commonsLogger.getLevel();
        commonsLogger.setLevel(Level.INFO); // change here to DEBUG in case of issues with this test
        final Semaphore serviceSemaphore = new Semaphore(0);
        final Semaphore testSemaphore = new Semaphore(0);
        final ReentrantLock lock = new ReentrantLock();
        final ConsistencyServiceWithSemaphore cs = new ConsistencyServiceWithSemaphore(lock, serviceSemaphore );
        mgr = new ViewStateManagerImpl(lock, cs);
        final Listener listener = new Listener();
        mgr.bind(listener);
        TestHelper.assertNoEvents(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final String slingId1 = UUID.randomUUID().toString();
        final String slingId2 = UUID.randomUUID().toString();
        final String slingId3 = UUID.randomUUID().toString();
        final String clusterId = UUID.randomUUID().toString();
        final SimpleClusterView cluster = new SimpleClusterView(clusterId);
        final SimpleTopologyView view1 = new SimpleTopologyView()
                .addInstance(slingId1, cluster, true, true)
                .addInstance(slingId2, cluster, false, false)
                .addInstance(slingId3, cluster, false, false);
        final SimpleTopologyView view2 = SimpleTopologyView.clone(view1).removeInstance(slingId2);
        final SimpleTopologyView view3 = SimpleTopologyView.clone(view1).removeInstance(slingId2).removeInstance(slingId3);
        async(new Runnable() {

            public void run() {
                mgr.handleNewView(view1);
            }
            
        });
        Thread.sleep(1000);
        TestHelper.assertNoEvents(listener);
        serviceSemaphore.release(1); // release the first one only
        Thread.sleep(1000);
        assertEvents(listener, EventFactory.newInitEvent(view1));
        mgr.handleChanging();
        assertEvents(listener, EventFactory.newChangingEvent(view1));
        async(new Runnable() {

            public void run() {
                mgr.handleNewView(view2);
            }
            
        });
        logger.debug("run: waiting 1sec");
        Thread.sleep(1000);
        logger.debug("run: asserting no events");
        TestHelper.assertNoEvents(listener);
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
        assertEquals("should be acquiring (by thread2)", 1, testSemaphore.getQueueLength());
        // releasing the testSemaphore
        testSemaphore.release();
        logger.debug("run: waiting 1sec");
        Thread.sleep(1000);
        assertEquals("should have both threads now waiting", 2, serviceSemaphore.getQueueLength());
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
        final TopologyEvent changedEvent = EventFactory.newChangedEvent(view1, view3);
        assertEvents(listener, changedEvent);
        commonsLogger.setLevel(Level.INFO); // back to default
    }

}
