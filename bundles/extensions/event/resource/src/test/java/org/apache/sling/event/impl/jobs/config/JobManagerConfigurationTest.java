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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.commons.InitDelayingTopologyEventListener;
import org.apache.sling.event.impl.TestUtil;
import org.junit.Test;
import org.mockito.Mockito;

public class JobManagerConfigurationTest {

    private TopologyView createView() {
        final TopologyView view = Mockito.mock(TopologyView.class);
        Mockito.when(view.isCurrent()).thenReturn(true);
        final InstanceDescription local = Mockito.mock(InstanceDescription.class);
        Mockito.when(local.isLeader()).thenReturn(true);
        Mockito.when(local.isLocal()).thenReturn(true);
        Mockito.when(local.getSlingId()).thenReturn("id");

        Mockito.when(view.getLocalInstance()).thenReturn(local);
        final ClusterView localView = Mockito.mock(ClusterView.class);
        Mockito.when(localView.getId()).thenReturn("1");
        Mockito.when(localView.getInstances()).thenReturn(Collections.singletonList(local));
        Mockito.when(view.getClusterViews()).thenReturn(Collections.singleton(localView));
        Mockito.when(local.getClusterView()).thenReturn(localView);

        return view;
    }

    private static class ChangeListener implements ConfigurationChangeListener {

        public final List<Boolean> events = new ArrayList<>();
        private volatile CountDownLatch latch;

        public void init(final int count) {
            events.clear();
            latch = new CountDownLatch(count);
        }

        public void await() throws Exception {
            if ( !latch.await(8000, TimeUnit.MILLISECONDS) ) {
                throw new Exception("No configuration event within 8 seconds.");
            }
        }

        @Override
        public void configurationChanged(boolean active) {
            events.add(active);
            latch.countDown();
        }
    }

    @Test public void testTopologyChange() throws Exception {
        // mock scheduler
        final ChangeListener ccl = new ChangeListener();

        // add change listener and verify
        ccl.init(1);
        final JobManagerConfiguration config = new JobManagerConfiguration();
        ((AtomicBoolean)TestUtil.getFieldValue(config, "active")).set(true);
        InitDelayingTopologyEventListener startupDelayListener = new InitDelayingTopologyEventListener(1, new TopologyEventListener() {

            @Override
            public void handleTopologyEvent(TopologyEvent event) {
                config.doHandleTopologyEvent(event);
            }
        });
        TestUtil.setFieldValue(config, "startupDelayListener", startupDelayListener);

        config.addListener(ccl);
        ccl.await();

        assertEquals(1, ccl.events.size());
        assertFalse(ccl.events.get(0));

        // create init view
        ccl.init(1);
        final TopologyView initView = createView();
        final TopologyEvent init = new TopologyEvent(TopologyEvent.Type.TOPOLOGY_INIT, null, initView);
        config.handleTopologyEvent(init);
        ccl.await();

        assertEquals(1, ccl.events.size());
        assertTrue(ccl.events.get(0));

        // change view, followed by change props
        ccl.init(2);
        final TopologyView view2 = createView();
        Mockito.when(initView.isCurrent()).thenReturn(false);
        final TopologyEvent change1 = new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGED, initView, view2);
        final TopologyView view3 = createView();
        final TopologyEvent change2 = new TopologyEvent(TopologyEvent.Type.PROPERTIES_CHANGED, view2, view3);

        config.handleTopologyEvent(change1);
        Mockito.when(view2.isCurrent()).thenReturn(false);
        config.handleTopologyEvent(change2);

        ccl.await();
        assertEquals(2, ccl.events.size());
        assertFalse(ccl.events.get(0));
        assertTrue(ccl.events.get(1));

        // we wait another 4 secs to see if there is no another event
        Thread.sleep(4000);
        assertEquals(2, ccl.events.size());

    }
}
