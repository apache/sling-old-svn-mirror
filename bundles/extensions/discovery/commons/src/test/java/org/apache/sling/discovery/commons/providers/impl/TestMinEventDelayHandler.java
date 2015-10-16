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

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.EventFactory;
import org.apache.sling.discovery.commons.providers.spi.ConsistencyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMinEventDelayHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ViewStateManagerImpl mgr;
    
    private Random defaultRandom;
    
    private SimpleDiscoveryService sds;

    private Level logLevel;

    private SimpleScheduler scheduler;

    @Before
    public void setup() throws Exception {
        mgr = new ViewStateManagerImpl(new ReentrantLock(), new ConsistencyService() {
            
            public void sync(BaseTopologyView view, Runnable callback) {
                callback.run();
            }
        });
        defaultRandom = new Random(1234123412); // I want randomness yes, but deterministic, for some methods at least
        
        scheduler = new SimpleScheduler();
        sds = new SimpleDiscoveryService();
        mgr.installMinEventDelayHandler(sds, scheduler, 1);

        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.DEBUG);
    }
    
    @After
    public void teardown() throws Exception {
        mgr = null;
        defaultRandom= null;
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);
    }
    
    private void assertNoEvents(Listener listener) {
        assertEquals(0, listener.countEvents());
    }

    @Test
    public void testNormalDelaying() throws Exception {
        final Listener listener = new Listener();
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
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        logger.info("testNormalDelaying: calling handleNewView...");
        mgr.handleNewView(view);
        TestHelper.assertEvents(mgr, listener, EventFactory.newInitEvent(view));
        for(int i=0; i<7; i++) {
            logger.info("testNormalDelaying: calling randomEventLoop...");
            TestHelper.randomEventLoop(mgr, sds, 4, 1500, defaultRandom, listener);
            Thread.sleep(1000);
        }
    }

    @Test
    public void testFailedDelaying() throws Exception {
        scheduler.failMode();
        final Listener listener = new Listener();
        // first activate
        mgr.handleActivated();
        assertNoEvents(listener); // paranoia
        // then bind
        mgr.bind(listener);
        assertNoEvents(listener); // there was no changing or changed yet
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view = new SimpleTopologyView().addInstance();
        mgr.handleNewView(view);
        TestHelper.assertEvents(mgr, listener, EventFactory.newInitEvent(view));
        for(int i=0; i<7; i++) {
            TestHelper.randomEventLoop(mgr, sds, 100, -1, defaultRandom, listener);
            Thread.sleep(1000);
        }
    }
}
