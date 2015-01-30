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
package org.apache.sling.discovery.impl.topology;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.setup.Instance;
import org.apache.sling.testing.tools.sling.TimeoutsProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LargeTopologyWithHubTest {

    private static final Logger logger = LoggerFactory.getLogger(LargeTopologyWithHubTest.class);

    private static List<Instance> instances;
    private static Instance hub;
    private static List<String> slingIds;
    private static final int TEST_SIZE = 100;
    
    @Rule
    public final RetryRule retryRule = new RetryRule();

    @BeforeClass
    public static void setup() throws Throwable {
        instances = new LinkedList<Instance>();
        hub = TopologyTestHelper.createInstance(instances, "hub");
        final int defaultHeartbeatTimeout = 1500;
        final int heartbeatTimeout = TimeoutsProvider.getInstance().getTimeout(defaultHeartbeatTimeout);
        hub.getConfig().setHeartbeatTimeout(heartbeatTimeout);
        
        slingIds = new LinkedList<String>();
        slingIds.add(hub.getSlingId());
        logger.info("setUp: using heartbeatTimeout of "+heartbeatTimeout+"sec "
                + "(default: "+defaultHeartbeatTimeout+")");
        for(int i=0; i<TEST_SIZE; i++) {
            Instance instance = TopologyTestHelper.createInstance(instances, "instance"+i);
            instance.getConfig().setHeartbeatTimeout(heartbeatTimeout);
            new Connector(instance, hub);
            slingIds.add(instance.getSlingId());
        }
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
            final Instance instance = it.next();
            instance.stop();
        }
    }
    
    @Test
    @Retry(timeoutMsec=30000, intervalMsec=500)
    public void testLargeTopologyWithHub() throws Exception {
        hub.dumpRepo();
        final TopologyView tv = hub.getDiscoveryService().getTopology();
        logger.info(
                "testLargeTopologyWithHub: checking if all connectors are registered, TopologyView has {} Instances", 
                tv.getInstances().size());
        TopologyTestHelper.assertTopologyConsistsOf(tv, slingIds.toArray(new String[slingIds.size()]));
        logger.info("testLargeTopologyWithHub: test passed");
    }
}