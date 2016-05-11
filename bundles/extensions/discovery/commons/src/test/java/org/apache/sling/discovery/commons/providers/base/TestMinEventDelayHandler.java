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
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.commons.testing.junit.categories.Slow;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DummyTopologyView;
import org.apache.sling.discovery.commons.providers.EventHelper;
import org.apache.sling.discovery.commons.providers.base.ViewStateManagerImpl;
import org.apache.sling.discovery.commons.providers.spi.ClusterSyncService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMinEventDelayHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ViewStateManagerImpl mgr;
    
    private Random defaultRandom;
    
    private DummyDiscoveryService sds;

    private Level logLevel;

    private DummyScheduler scheduler;

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
        
        scheduler = new DummyScheduler();
        sds = new DummyDiscoveryService();
        mgr.installMinEventDelayHandler(sds, scheduler, 1);

        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.INFO); // changed from Level.DEBUG
    }
    
    @After
    public void teardown() throws Exception {
        mgr = null;
        defaultRandom= null;
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);
    }
    
    
    @Test
    public void testReactivate() throws Exception {
        logger.info("testReactivate: start");
        // install a minEventDelayHandler with a longer delay of 2sec
        mgr.installMinEventDelayHandler(sds, scheduler, 2);

        final DummyListener listener = new DummyListener();
        logger.info("testReactivate: calling handleActivated");
        mgr.bind(listener);
        mgr.handleActivated();
        TestHelper.assertNoEvents(listener);
        final DummyTopologyView view1 = new DummyTopologyView().addInstance();
        final DummyTopologyView view2 = DummyTopologyView.clone(view1).addInstance(UUID.randomUUID().toString(), 
                (DefaultClusterView) view1.getLocalInstance().getClusterView(), false, false);
        final DummyTopologyView view3 = DummyTopologyView.clone(view1).addInstance(UUID.randomUUID().toString(), 
                (DefaultClusterView) view1.getLocalInstance().getClusterView(), false, false);
        logger.info("testReactivate: calling handleNewView...");
        mgr.handleNewView(view1);
        logger.info("testReactivate: asserting init event");
        TestHelper.assertEvents(mgr, listener, EventHelper.newInitEvent(view1));
        logger.info("testReactivate: calling handleChanging...");
        mgr.handleChanging();
        TestHelper.assertEvents(mgr, listener, EventHelper.newChangingEvent(view1));
        logger.info("testReactivate: calling handleNewView 2nd time...");
        mgr.handleNewView(view2);
        TestHelper.assertNoEvents(listener);
        // make sure the MinEventDelayHandler finds a topology when coming back from the delaying, so:
        sds.setTopoology(view2);
        logger.info("testReactivate: waiting for async events to have been processed - 4sec");
        Thread.sleep(4000);
        logger.info("testReactivate: waiting for async events to have been processed - max another 2sec");
        assertEquals(0, mgr.waitForAsyncEvents(2000));
        logger.info("testReactivate: asserting CHANGED event");
        TestHelper.assertEvents(mgr, listener, EventHelper.newChangedEvent(view1, view2));
        
        // now do the above again, but this time do a handleDeactivated before receiving another changed event
        logger.info("testReactivate: calling handleChanging...");
        mgr.handleChanging();
        TestHelper.assertEvents(mgr, listener, EventHelper.newChangingEvent(view2));
        logger.info("testReactivate: calling handleNewView 2nd time...");
        mgr.handleNewView(view3);
        TestHelper.assertNoEvents(listener);
        // make sure the MinEventDelayHandler finds a topology when coming back from the delaying, so:
        sds.setTopoology(view3);
        
        logger.info("testReactivate: doing handleDeactivated");
        final AsyncEventSender asyncEventSender = mgr.getAsyncEventSender();
        Field field = mgr.getClass().getDeclaredField("minEventDelayHandler");
        field.setAccessible(true);
        MinEventDelayHandler minEventDelayHandler = (MinEventDelayHandler) field.get(mgr);
        assertNotNull(minEventDelayHandler);
        
        // marking view3 as not current
        view3.setNotCurrent();
        sds.setTopoology(view3);
        
        mgr.handleDeactivated();
        TestHelper.assertNoEvents(listener);
        
        logger.info("testReactivate: now waiting 5 sec to make sure the MinEventDelayHandler would be finished");
        TestHelper.assertNoEvents(listener);
        Thread.sleep(5000);
        logger.info("testReactivate: after those 5 sec there should however still not be any new event");
        TestHelper.assertNoEvents(listener);

        int cnt = asyncEventSender.getInFlightEventCnt();
        if (minEventDelayHandler!=null && minEventDelayHandler.isDelaying()) {
            cnt++;
        }
        assertEquals(0, cnt);
    }

    private void assertNoEvents(DummyListener listener) {
        assertEquals(0, listener.countEvents());
    }

    @Category(Slow.class) //TODO test takes env 50sec
    @Test
    public void testNormalDelaying() throws Exception {
        final DummyListener listener = new DummyListener();
        // first activate
        logger.info("testNormalDelaying: calling handleActivated...");
        mgr.handleActivated();
        assertNoEvents(listener); // paranoia
        // then bind
        logger.info("testNormalDelaying: calling bind...");
        mgr.bind(listener);
        assertNoEvents(listener); // there was no changing or changed yet
        logger.info("testNormalDelaying: calling handleChanging...");
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        logger.info("testNormalDelaying: calling handleNewView...");
        mgr.handleNewView(view);
        TestHelper.assertEvents(mgr, listener, EventHelper.newInitEvent(view));
        for(int i=0; i<7; i++) {
            logger.info("testNormalDelaying: calling randomEventLoop...");
            TestHelper.randomEventLoop(mgr, sds, 4, 1500, defaultRandom, listener);
            Thread.sleep(1000);
        }
    }

    @Category(Slow.class) //TODO test takes env 45sec
    @Test
    public void testFailedDelaying() throws Exception {
        scheduler.failMode();
        final DummyListener listener = new DummyListener();
        // first activate
        mgr.handleActivated();
        assertNoEvents(listener); // paranoia
        // then bind
        mgr.bind(listener);
        assertNoEvents(listener); // there was no changing or changed yet
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view = new DummyTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertEvents(mgr, listener, EventHelper.newInitEvent(view));
        for(int i=0; i<7; i++) {
            TestHelper.randomEventLoop(mgr, sds, 100, -1, defaultRandom, listener);
            Thread.sleep(1000);
        }
    }
    
    @Test
    public void testLongMinDelay() throws Exception {
        mgr.installMinEventDelayHandler(sds, scheduler, 5);
        final DummyListener listener = new DummyListener();
        // first activate
        logger.info("testLongMinDelay: calling handleActivated...");
        mgr.handleActivated();
        assertNoEvents(listener); // paranoia
        // then bind
        logger.info("testLongMinDelay: calling bind...");
        mgr.bind(listener);
        assertNoEvents(listener); // there was no changing or changed yet
        logger.info("testLongMinDelay: calling handleChanging...");
        mgr.handleChanging();
        assertNoEvents(listener);
        final DummyTopologyView view = new DummyTopologyView().addInstance();
        DummyTopologyView clonedView = view.clone();
        logger.info("testLongMinDelay: calling handleNewView...");
        mgr.handleNewView(view);
        TestHelper.assertEvents(mgr, listener, EventHelper.newInitEvent(view));
        final DummyTopologyView view2 = new DummyTopologyView().addInstance();
        view2.addInstance(UUID.randomUUID().toString(), (DefaultClusterView) view2.getLocalInstance().getClusterView(), false, false);
        logger.info("testLongMinDelay: calling handleNewView...");
        clonedView.setNotCurrent();
        mgr.handleNewView(view2);
        TestHelper.assertEvents(mgr, listener, EventHelper.newChangingEvent(clonedView));
        assertFalse(view.isCurrent());
    }
}
