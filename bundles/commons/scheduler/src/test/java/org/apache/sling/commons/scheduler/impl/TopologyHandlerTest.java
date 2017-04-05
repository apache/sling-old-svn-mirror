/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.scheduler.impl;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.common.DefaultInstanceDescriptionImpl;
import org.apache.sling.discovery.impl.topology.TopologyViewImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TopologyHandlerTest {
    private TopologyHandler underTest;

    @Before
    public void init() {
        underTest = new TopologyHandler();
    }

    @Test
    public void testActivate() {
        underTest.activate();
        assertTrue(QuartzJobExecutor.DISCOVERY_AVAILABLE.get());
    }

    @Test
    public void testDeactivate() {
        underTest.deactivate();
        assertFalse(QuartzJobExecutor.DISCOVERY_AVAILABLE.get());
    }

    @Test
    public void testHandleTopologyEvent() {
        TopologyView oldView = new TopologyViewImpl();
        TopologyView newView = newViewWithInstanceDescription(true);

        TopologyEvent event = new TopologyEvent(TopologyEvent.Type.PROPERTIES_CHANGED, oldView, newView);
        underTest.handleTopologyEvent(event);
        assertTrue(QuartzJobExecutor.IS_LEADER.get());
        assertFalse(QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.get());

        event = new TopologyEvent(TopologyEvent.Type.TOPOLOGY_INIT, null, newView);
        underTest.handleTopologyEvent(event);
        assertTrue(QuartzJobExecutor.IS_LEADER.get());
        assertTrue(QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.get());

        event = new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGED, oldView, newView);
        underTest.handleTopologyEvent(event);
        assertTrue(QuartzJobExecutor.IS_LEADER.get());
        assertTrue(QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.get());

        newView = newViewWithInstanceDescription(false);
        event = new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGED, oldView, newView);
        underTest.handleTopologyEvent(event);
        assertFalse(QuartzJobExecutor.IS_LEADER.get());
        assertTrue(QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.get());

        event = new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGING, oldView, null);
        underTest.handleTopologyEvent(event);
        assertFalse(QuartzJobExecutor.IS_LEADER.get());
        assertFalse(QuartzJobExecutor.DISCOVERY_INFO_AVAILABLE.get());
    }

    private TopologyView newViewWithInstanceDescription(boolean isLeader) {
        InstanceDescription description = new DefaultInstanceDescriptionImpl(null, isLeader, true, "anystring", null);
        return new TopologyViewImpl(Arrays.asList(description));
    }
}
