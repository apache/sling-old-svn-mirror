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

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.commons.testing.junit.categories.Slow;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DummyTopologyView;
import org.apache.sling.discovery.commons.providers.EventHelper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Slow.class)
public class TestSlowViewStateManager extends TestViewStateManager {

    /** does couple loops randomly calling handleChanging() (or not) and then handleNewView().
     * Note: random is passed to allow customizing and not hardcoding this method to a particular random 
     * @throws InterruptedException **/
    protected void randomEventLoop(final Random random, DummyListener... listeners) throws InterruptedException {
        TestHelper.randomEventLoop(mgr, null, 100, -1, random, listeners);
    }
    
    @Category(Slow.class) //TODO test takes env 10sec
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
