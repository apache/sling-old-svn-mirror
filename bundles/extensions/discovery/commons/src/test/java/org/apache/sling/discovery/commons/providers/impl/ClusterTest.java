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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.spi.ConsistencyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterTest {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<ViewStateManagerImpl> mgrList;
    
    private Random defaultRandom;

    @Before
    public void setup() throws Exception {
        mgrList = new LinkedList<ViewStateManagerImpl>();
        defaultRandom = new Random(1234123412); // I want randomness yes, but deterministic, for some methods at least
    }
    
    @After
    public void teardown() throws Exception {
        mgrList = null;
        defaultRandom= null;
    }
    
    private ViewStateManagerImpl newMgr() {
        ViewStateManagerImpl mgr = new ViewStateManagerImpl(new ReentrantLock(), new ConsistencyService() {
            
            public void sync(BaseTopologyView view, Runnable callback) {
                callback.run();
            }
        });
        mgrList.add(mgr);
        return mgr;
    }
    
    private void waitForInflightEvents(ViewStateManagerImpl mgr) throws InterruptedException {
        if (mgr==null) {
            throw new IllegalArgumentException("mgr must not be null");
        }
        if (mgr.getAsyncEventSender()==null) {
            logger.info("waitForInflightEvents: mgr not yet activated...");
            return;
        }
        while(mgr.getAsyncEventSender().hasInFlightEvent()) {
            logger.info("waitForInflightEvents: waiting 10ms...");
            Thread.sleep(10);
        }
    }
    
    private void assertCountEvents(ViewStateManagerImpl mgr, Listener l, TopologyEvent.Type... types) throws InterruptedException {
        waitForInflightEvents(mgr);
        assertEquals(types.length, l.countEvents());
        Iterator<TopologyEvent> it = l.getEvents().iterator();
        int i=0;
        while(it.hasNext() && (i<types.length)) {
            TopologyEvent expectedEvent = it.next();
            Type gotType = types[i++];
            assertEquals(expectedEvent.getType(), gotType);
        }
        if (it.hasNext()) {
            StringBuffer additionalTypes = new StringBuffer();
            while(it.hasNext()) {
                additionalTypes.append(",");
                additionalTypes.append(it.next().getType());
            }
            fail("got more events than expected : "+additionalTypes);
        }
        if (i<types.length) {
            StringBuffer additionalTypes = new StringBuffer();
            while(i<types.length) {
                additionalTypes.append(",");
                additionalTypes.append(types[i++]);
            }
            fail("did not get all events, also expected : "+additionalTypes);
        }
    }

    private void fail(String string) {
        // TODO Auto-generated method stub
        
    }

    @Test
    public void testTwoNodes() throws Exception {
        final ViewStateManagerImpl mgr1 = newMgr();
        final String slingId1 = UUID.randomUUID().toString();
        final ViewStateManagerImpl mgr2 = newMgr();
        final String slingId2 = UUID.randomUUID().toString();
        
        // bind l1
        Listener l1 = new Listener();
        mgr1.bind(l1);
        assertCountEvents(mgr1, l1);
        
        // bind l2
        Listener l2 = new Listener();
        mgr2.bind(l2);
        assertCountEvents(mgr2, l2);
        
        // fiddle with l1 - without any events expected to be sent
        mgr1.handleChanging();
        assertCountEvents(mgr1, l1);
        mgr1.handleActivated();
        assertCountEvents(mgr1, l1);
        mgr1.handleChanging();
        assertCountEvents(mgr1, l1);

        // fiddle with l2 - without any events expected to be sent
        mgr2.handleChanging();
        assertCountEvents(mgr2, l2);
        mgr2.handleActivated();
        assertCountEvents(mgr2, l2);
        mgr2.handleChanging();
        assertCountEvents(mgr2, l2);
        
        // call handleNewView with not-current views first...
        BaseTopologyView vA1 = TestHelper.newView(false, slingId1, slingId1, slingId1, slingId2);
        mgr1.handleNewView(vA1);
        assertCountEvents(mgr1, l1);
        assertCountEvents(mgr2, l2);
        BaseTopologyView vB1 = TestHelper.newView(false, slingId1, slingId2, slingId1, slingId2);
        mgr2.handleNewView(vB1);
        assertCountEvents(mgr1, l1);
        assertCountEvents(mgr2, l2);
        
        // then call handleNewView with a current view - that should now sent the INIT
        BaseTopologyView vA2 = TestHelper.newView(true, slingId1, slingId1, slingId1, slingId2);
        mgr1.handleNewView(vA2);
        assertCountEvents(mgr1, l1, Type.TOPOLOGY_INIT);
        assertCountEvents(mgr2, l2);
        BaseTopologyView vB2 = TestHelper.newView(true, slingId1, slingId2, slingId1, slingId2);
        mgr2.handleNewView(vB2);
        assertCountEvents(mgr1, l1, Type.TOPOLOGY_INIT);
        assertCountEvents(mgr2, l2, Type.TOPOLOGY_INIT);
        
        // now let instance1 get decoupled from the cluster (pseudo-network-partitioning)
        BaseTopologyView vB3 = TestHelper.newView(true, slingId2, slingId2, slingId2);
        mgr2.handleNewView(vB3);
        assertCountEvents(mgr1, l1, Type.TOPOLOGY_INIT);
        assertCountEvents(mgr2, l2, Type.TOPOLOGY_INIT, Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED);
    
        // now let instance1 take note of this decoupling
        mgr1.handleChanging();
        assertCountEvents(mgr1, l1, Type.TOPOLOGY_INIT, Type.TOPOLOGY_CHANGING);
        assertCountEvents(mgr2, l2, Type.TOPOLOGY_INIT, Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED);
        
        // and now let instance1 rejoin
        BaseTopologyView vA4 = TestHelper.newView(true, slingId2, slingId1, slingId1, slingId2);
        mgr1.handleNewView(vA4);
        assertCountEvents(mgr1, l1, Type.TOPOLOGY_INIT, Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED);
        assertCountEvents(mgr2, l2, Type.TOPOLOGY_INIT, Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED);
        BaseTopologyView vB4 = TestHelper.newView(true, slingId2, slingId2, slingId1, slingId2);
        mgr2.handleNewView(vA4);
        assertCountEvents(mgr1, l1, Type.TOPOLOGY_INIT, Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED);
        assertCountEvents(mgr2, l2, Type.TOPOLOGY_INIT, Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED, Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED);
    }

}
