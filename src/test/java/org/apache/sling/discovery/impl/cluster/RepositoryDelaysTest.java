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
package org.apache.sling.discovery.impl.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.mock.AssertingTopologyEventListener;
import org.apache.sling.discovery.impl.setup.FullJR2VirtualInstance;
import org.apache.sling.discovery.impl.setup.FullJR2VirtualInstanceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryDelaysTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Level logLevel;

    private FullJR2VirtualInstance instance1;

    private FullJR2VirtualInstance instance2;

    protected FullJR2VirtualInstanceBuilder newBuilder() {
        return new FullJR2VirtualInstanceBuilder();
    }

    @Before
    public void setUp() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.TRACE);        
    }
    
    @After
    public void teartDown() throws Throwable {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);
        if (instance1!=null) {
            instance1.stopViewChecker();
            instance1.stop();
            instance1 = null;
        }
        if (instance2!=null) {
            instance2.stopViewChecker();
            instance2.stop();
            instance2 = null;
        }
    }
    
    /**
     * SLING-5195 : simulate slow session.saves that block
     * the calling thread for non-trivial amounts of time,
     * typically for longer than the configured heartbeat
     * timeout
     */
    @Test
    public void testSlowSessionSaves() throws Exception {
        VirtualInstanceBuilder builder1 = newBuilder();
        VirtualInstance instance1 = builder1
                .setDebugName("firstInstance")
                .newRepository("/var/discovery/impl/", true)
                .setMinEventDelay(0)
                .setConnectorPingInterval(1)
                .setConnectorPingTimeout(3)
                .build();
        VirtualInstanceBuilder builder2 = newBuilder();
        VirtualInstance instance2 = builder2
                .setDebugName("secondInstance")
                .useRepositoryOf(instance1)
                .setMinEventDelay(0)
                .setConnectorPingInterval(1)
                .setConnectorPingTimeout(3)
                .build();
        
        instance1.setDelay("pre.commit", 12000);
        instance1.startViewChecker(1);
        instance2.startViewChecker(1);
        Thread.sleep(5000);
        // after 3 sec - without the 7sec pre-commit delay
        // the view would normally be established - but this time
        // round instance1 should still be pre-init and instance2
        // should be init but alone
        TopologyView t1 = instance1.getDiscoveryService().getTopology();
        assertFalse(t1.isCurrent());
        
        TopologyView t2 = instance2.getDiscoveryService().getTopology();
        assertTrue(t2.isCurrent());
        assertEquals(1, t2.getInstances().size());
        
        instance1.setDelay("pre.commit", -1);
        Thread.sleep(3000);

        TopologyView t1b = instance1.getDiscoveryService().getTopology();
        assertTrue(t1b.isCurrent());
        assertEquals(2, t1b.getInstances().size());
        
        TopologyView t2b = instance2.getDiscoveryService().getTopology();
        assertTrue(t2b.isCurrent());
        assertEquals(2, t2b.getInstances().size());
        
        instance1.setDelay("pre.commit", 59876);
        instance2.setDelay("pre.commit", 60000);
        logger.info("<main> both instances marked as delaying 1min - but with new background checks we should go changing within 3sec");
        Thread.sleep(8000);
        
        TopologyView t1c = instance1.getDiscoveryService().getTopology();
        assertFalse(t1c.isCurrent());
        
        TopologyView t2c = instance2.getDiscoveryService().getTopology();
        assertFalse(t2c.isCurrent());
    }
    
}
