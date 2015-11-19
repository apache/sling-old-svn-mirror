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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.DummyTopologyView;
import org.apache.sling.discovery.commons.providers.EventHelper;
import org.apache.sling.discovery.commons.providers.base.ViewStateManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);

    public static void assertEvents(ViewStateManagerImpl mgr, DummyListener listener, TopologyEvent... events) {
        waitForAsyncEvents(mgr);
        assertEquals(events.length, listener.countEvents());
        for (int i = 0; i < events.length; i++) {
            TopologyEvent e = events[i];
            assertEquals(e.getType(), listener.getEvents().get(i).getType());
            switch(e.getType()) {
            case TOPOLOGY_INIT: {
                assertNull(listener.getEvents().get(i).getOldView());
                assertEquals(e.getNewView(), listener.getEvents().get(i).getNewView());
                break;
            }
            case TOPOLOGY_CHANGING: {
                assertEquals(e.getOldView(), listener.getEvents().get(i).getOldView());
                assertNull(listener.getEvents().get(i).getNewView());
                break;
            }
            case PROPERTIES_CHANGED:
            case TOPOLOGY_CHANGED: {
                assertEquals(e.getOldView(), listener.getEvents().get(i).getOldView());
                assertEquals(e.getNewView(), listener.getEvents().get(i).getNewView());
                break;
            }
            default: {
                fail("no other type supported yet");
            }
            }
        }
        listener.clearEvents();
    }

    public static void waitForAsyncEvents(ViewStateManagerImpl mgr) {
        int sleep = 1;
        while(true) {
            if (!mgr.getAsyncEventSender().hasInFlightEvent()) {
                return;
            }
            
            // sleep outside of synchronized to keep test-influence
            // to a minimum
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                logger.error("waitForFlush: got interrupted: "+e, e);
            }
            // minor back-off up until 20ms
            sleep = Math.min(20, sleep+1);
        }
    }

    public static void assertNoEvents(DummyListener listener) {
        assertEquals(0, listener.countEvents());
    }

    /** does couple loops randomly calling handleChanging() (or not) and then handleNewView().
     * Note: random is passed to allow customizing and not hardcoding this method to a particular random 
     * @throws InterruptedException **/
    public static void randomEventLoop(ViewStateManagerImpl mgr, DummyDiscoveryService sds, int loopSize, int delayInMillis, final Random random, DummyListener... listeners) throws InterruptedException {
        for(int i=0; i<loopSize; i++) {
            final boolean shouldCallChanging = random.nextBoolean();
            if (shouldCallChanging) {
                // dont always do a changing
                logger.debug("randomEventLoop: calling handleChanging...");
                mgr.handleChanging();
                // must first wait for async events to have been processed - as otherwise
                // the 'getLastView()' might not return the correct view
                logger.debug("randomEventLoop: waiting for async events....");
                waitForAsyncEvents(mgr);
                logger.debug("randomEventLoop: asserting CHANGING event was sent...");
                for(int j=0; j<listeners.length; j++) {
                    assertEvents(mgr, listeners[j], EventHelper.newChangingEvent(listeners[j].getLastView()));
                }
            } else {
                logger.debug("randomEventLoop: asserting no events...");
                for(int j=0; j<listeners.length; j++) {
                    assertNoEvents(listeners[j]);
                }
            }
            final DummyTopologyView view = new DummyTopologyView().addInstance();
            BaseTopologyView[] lastViews = new BaseTopologyView[listeners.length];
            for(int j=0; j<listeners.length; j++) {
                lastViews[j] = listeners[j].getLastView();
            }
            logger.debug("randomEventLoop: calling handleNewView");
            if (sds!=null) {
                sds.setTopoology(view);
            }
            DummyTopologyView clonedView = view.clone();
            mgr.handleNewView(view);
            if (delayInMillis>0) {
                logger.debug("randomEventLoop: waiting "+delayInMillis+"ms ...");
                Thread.sleep(delayInMillis);
                logger.debug("randomEventLoop: waiting "+delayInMillis+"ms done.");
            }
            assertEquals(0, mgr.waitForAsyncEvents(500));
            if (!shouldCallChanging) {
                // in that case I should still get a CHANGING - by contract
                logger.debug("randomEventLoop: asserting CHANGING, CHANGED events were sent");
                for(int j=0; j<listeners.length; j++) {
                    assertEvents(mgr, listeners[j], EventHelper.newChangingEvent(lastViews[j]), EventHelper.newChangedEvent(lastViews[j], view));
                }
            } else {
                logger.debug("randomEventLoop: asserting CHANGED event was sent");
                for(int j=0; j<listeners.length; j++) {
                    assertEvents(mgr, listeners[j], EventHelper.newChangedEvent(lastViews[j], clonedView));
                }
            }
        }
    }

    public static DummyTopologyView newView(boolean isCurrent, String leaderId, String localId, String... slingIds) {
        return newView(UUID.randomUUID().toString(), UUID.randomUUID().toString(), isCurrent, leaderId, localId, slingIds);
    }

    public static DummyTopologyView newView(String syncId, String clusterId, boolean isCurrent, String leaderId, String localId, String... slingIds) {
        DummyTopologyView topology = new DummyTopologyView(syncId);
        DefaultClusterView cluster = new DefaultClusterView(clusterId);
        for (String slingId : slingIds) {
            DefaultInstanceDescription id = new DefaultInstanceDescription(cluster,
                    slingId.equals(leaderId), slingId.equals(localId), slingId, new HashMap<String, String>());
            topology.addInstanceDescription(id);
        }
        if (!isCurrent) {
            topology.setNotCurrent();
        }
        return topology;
    }
}
