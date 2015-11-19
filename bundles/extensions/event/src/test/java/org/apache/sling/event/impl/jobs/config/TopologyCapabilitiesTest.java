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
package org.apache.sling.event.impl.jobs.config;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TopologyCapabilitiesTest {

    private TopologyCapabilities caps;

    @Before
    public void setup() {
        // local cluster view
        final ClusterView cv = Mockito.mock(ClusterView.class);
        Mockito.when(cv.getId()).thenReturn("cluster");

        // local description
        final InstanceDescription local = Mockito.mock(InstanceDescription.class);
        Mockito.when(local.isLeader()).thenReturn(true);
        Mockito.when(local.getSlingId()).thenReturn("local");
        Mockito.when(local.getProperty(TopologyCapabilities.PROPERTY_TOPICS)).thenReturn("foo,bar/*,a/**,d/1/2,d/1/*,d/**");
        Mockito.when(local.getClusterView()).thenReturn(cv);

        // topology view
        final TopologyView tv = Mockito.mock(TopologyView.class);
        Mockito.when(tv.getInstances()).thenReturn(Collections.singleton(local));
        Mockito.when(tv.getLocalInstance()).thenReturn(local);

        final JobManagerConfiguration config = Mockito.mock(JobManagerConfiguration.class);

        caps = new TopologyCapabilities(tv, config);
    }

    @Test public void testMatching() {
        assertEquals(1, caps.getPotentialTargets("foo").size());
        assertEquals(0, caps.getPotentialTargets("foo/a").size());
        assertEquals(0, caps.getPotentialTargets("bar").size());
        assertEquals(1, caps.getPotentialTargets("bar/foo").size());
        assertEquals(0, caps.getPotentialTargets("bar/foo/a").size());
        assertEquals(1, caps.getPotentialTargets("a/b").size());
        assertEquals(1, caps.getPotentialTargets("a/b(c").size());
        assertEquals(0, caps.getPotentialTargets("x").size());
        assertEquals(0, caps.getPotentialTargets("x/y").size());
        assertEquals(1, caps.getPotentialTargets("d/1/2").size());
    }
}
