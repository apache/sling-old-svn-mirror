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
package org.apache.sling.discovery.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.impl.setup.FullJR2VirtualInstanceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryWithSyncTokenTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    List<VirtualInstance> instances;
    
    @Before
    public void setup() throws Exception {
        logger.info("setup: start");
        instances = new LinkedList<VirtualInstance>();
    }
    
    @After
    public void tearDown() throws Exception {
        logger.info("teardown: start");
        for (VirtualInstance virtualInstance : instances) {
            virtualInstance.stop();
        }
        logger.info("teardown: end");
    }
    
    @Test
    public void testTwoNodes() throws Throwable {
        logger.info("testTwoNodes: start");
        
        FullJR2VirtualInstanceBuilder b1 = new FullJR2VirtualInstanceBuilder();
        b1.setDebugName("i1").newRepository("/var/twon/", true);
        b1.setConnectorPingInterval(1).setMinEventDelay(1).setConnectorPingTimeout(60);
        VirtualInstance i1 = b1.build();
        instances.add(i1);
        i1.bindTopologyEventListener(new TopologyEventListener() {
            
            @Override
            public void handleTopologyEvent(TopologyEvent event) {
                logger.info("GOT EVENT: "+event);
            }
        });

        FullJR2VirtualInstanceBuilder b2 = new FullJR2VirtualInstanceBuilder();
        b2.setDebugName("i2").useRepositoryOf(i1);
        b2.setConnectorPingInterval(1).setMinEventDelay(1).setConnectorPingTimeout(60);
        VirtualInstance i2 = b2.build();
        instances.add(i2);
        
        i1.heartbeatsAndCheckView();
        i2.heartbeatsAndCheckView();
        i1.heartbeatsAndCheckView();
        i2.heartbeatsAndCheckView();
        
        Thread.sleep(999);
        //TODO: finalize test
    }
}
