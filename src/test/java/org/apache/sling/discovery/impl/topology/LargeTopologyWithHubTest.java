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

import org.apache.sling.discovery.impl.setup.Instance;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LargeTopologyWithHubTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<Instance> instances = new LinkedList<Instance>();
    
    @After
    public void tearDown() throws Exception {
        for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
            final Instance instance = it.next();
            instance.stop();
        }
    }
    
    @Test
    public void testLargeTopologyWithHub() throws Throwable {
        logger.info("testLargeTopologyWithHub: start");
        final int TEST_SIZE = 100;
        Instance hub = TopologyTestHelper.createInstance(instances, "hub");
        
        List<String> slingIds = new LinkedList<String>();
        slingIds.add(hub.getSlingId());
        for(int i=0; i<TEST_SIZE; i++) {
//            logger.info("testLargeTopologyWithHub: adding instance "+i);
            Instance instance = TopologyTestHelper.createInstance(instances, "instance"+i);
//            logger.info("testLargeTopologyWithHub: adding connector "+i);
            new Connector(instance, hub);
            slingIds.add(instance.getSlingId());
        }
        logger.info("testLargeTopologyWithHub: checking if all connectors are registered");
        TopologyTestHelper.assertTopologyConsistsOf(hub.getDiscoveryService().getTopology(), slingIds.toArray(new String[slingIds.size()]));
        logger.info("testLargeTopologyWithHub: end");
    }

}
