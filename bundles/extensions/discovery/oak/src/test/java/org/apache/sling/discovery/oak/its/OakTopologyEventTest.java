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
package org.apache.sling.discovery.oak.its;

import static org.junit.Assert.assertNotNull;

import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.base.its.AbstractTopologyEventTest;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.oak.its.setup.OakVirtualInstanceBuilder;

public class OakTopologyEventTest extends AbstractTopologyEventTest {

    @Override
    public VirtualInstanceBuilder newBuilder() {
        return new OakVirtualInstanceBuilder();
    }

    @Override
    public void assertEarlyAndFirstClusterViewIdMatches(TopologyView earlyTopo, TopologyView secondTopo) {
        // for the oak discovery-lite variant there's nothing we can assert here
        // except perhaps that they shouldn't be null..
        assertNotNull(earlyTopo);
        assertNotNull(secondTopo);
    }
    
}
