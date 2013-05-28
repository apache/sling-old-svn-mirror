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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.impl.cluster.helpers.AssertingTopologyEventListener;
import org.apache.sling.discovery.impl.common.resource.EstablishedInstanceDescription;
import org.apache.sling.discovery.impl.common.resource.IsolatedInstanceDescription;
import org.apache.sling.discovery.impl.setup.Instance;
import org.apache.sling.discovery.impl.setup.PropertyProviderImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleInstanceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    Instance instance;

    String propertyValue;

    @Before
    public void setup() throws Exception {
        instance = Instance.newStandaloneInstance("standaloneInstance", true);
    }

    @After
    public void tearDown() throws Exception {
        instance.stop();
    }

    @Test
    public void testGetters() {
        assertNotNull(instance);
        logger.info("sling id=" + instance.getSlingId());
        assertNotNull(instance.getClusterViewService().getClusterView());

        ClusterView cv = instance.getClusterViewService().getClusterView();
        logger.info("cluster view: id=" + cv.getId());
        assertNotNull(cv.getId());
        assertNotSame(cv.getId(), "");

        List<InstanceDescription> instances = cv.getInstances();
        assertNotNull(instances);
        assertTrue(instances.size() == 1);

        InstanceDescription myInstance = instances.get(0);
        assertNotNull(myInstance);
        assertTrue(myInstance.getClusterView() == cv);
        logger.info("instance id: " + myInstance.getSlingId());
        assertEquals(instance.getSlingId(), myInstance.getSlingId());

        Map<String, String> properties = myInstance.getProperties();
        assertNotNull(properties);

        assertNull(myInstance.getProperty("foo"));

        assertTrue(myInstance.isLeader());

        assertTrue(myInstance.isLocal());
    }

    @Test
    public void testPropertyProviders() throws Throwable {
        final String propertyName = UUID.randomUUID().toString();
        propertyValue = UUID.randomUUID().toString();
        PropertyProviderImpl pp = new PropertyProviderImpl();
        pp.setProperty(propertyName, propertyValue);
        instance.bindPropertyProvider(pp, propertyName);

        assertEquals(propertyValue,
                instance.getClusterViewService().getClusterView()
                        .getInstances().get(0).getProperty(propertyName));

        propertyValue = UUID.randomUUID().toString();
        pp.setProperty(propertyName, propertyValue);
        instance.runHeartbeatOnce();

        assertEquals(propertyValue,
                instance.getClusterViewService().getClusterView()
                        .getInstances().get(0).getProperty(propertyName));
        assertNull(instance.getClusterViewService().getClusterView()
                .getInstances().get(0)
                .getProperty(UUID.randomUUID().toString()));
    }
    
    @Test
    public void testInvalidProperties() throws Throwable {
        final String propertyValue = UUID.randomUUID().toString();
        doTestProperty(UUID.randomUUID().toString(), propertyValue, propertyValue);

        doTestProperty("", propertyValue, null);
        doTestProperty("-", propertyValue, propertyValue);
        doTestProperty("_", propertyValue, propertyValue);
        doTestProperty("jcr:" + UUID.randomUUID().toString(), propertyValue, null);
        doTestProperty("var/" + UUID.randomUUID().toString(), propertyValue, null);
        doTestProperty(UUID.randomUUID().toString() + "@test", propertyValue, null);
        doTestProperty(UUID.randomUUID().toString() + "!test", propertyValue, null);
    }

	private void doTestProperty(final String propertyName,
			final String propertyValue,
			final String expectedPropertyValue) throws Throwable {
		PropertyProviderImpl pp = new PropertyProviderImpl();
        pp.setProperty(propertyName, propertyValue);
        instance.bindPropertyProvider(pp, propertyName);
        assertEquals(expectedPropertyValue,
                instance.getClusterViewService().getClusterView()
                        .getInstances().get(0).getProperty(propertyName));
	}
    
    @Test
    public void testTopologyEventListeners() throws Throwable {
        instance.runHeartbeatOnce();
        Thread.sleep(2000);
        instance.runHeartbeatOnce();
        Thread.sleep(2000);

        AssertingTopologyEventListener assertingTopologyEventListener = new AssertingTopologyEventListener();
        assertingTopologyEventListener.addExpected(Type.TOPOLOGY_INIT);
        instance.bindTopologyEventListener(assertingTopologyEventListener);
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());

        final String propertyName = UUID.randomUUID().toString();
        propertyValue = UUID.randomUUID().toString();
        PropertyProviderImpl pp = new PropertyProviderImpl();
        pp.setProperty(propertyName, propertyValue);

        assertingTopologyEventListener.addExpected(Type.PROPERTIES_CHANGED);

        assertEquals(1, assertingTopologyEventListener.getRemainingExpectedCount());
        assertEquals(0, pp.getGetCnt());
        instance.bindPropertyProvider(pp, propertyName);
        Thread.sleep(1500);
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());
        // we can only assume that the getProperty was called at least once - it
        // could be called multiple times though..
        assertTrue(pp.getGetCnt() > 0);

        assertingTopologyEventListener.addExpected(Type.PROPERTIES_CHANGED);

        assertEquals(1, assertingTopologyEventListener.getRemainingExpectedCount());
        pp.setGetCnt(0);
        propertyValue = UUID.randomUUID().toString();
        pp.setProperty(propertyName, propertyValue);
        assertEquals(0, pp.getGetCnt());
        instance.runHeartbeatOnce();
        Thread.sleep(2000);
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());
        assertEquals(2, pp.getGetCnt());

        // a heartbeat repeat should not result in another call though
        instance.runHeartbeatOnce();
        Thread.sleep(2000);
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());
        assertEquals(3, pp.getGetCnt());

    }

    @Test
    public void testBootstrap() throws Throwable {
        ClusterView initialClusterView = instance.getClusterViewService()
                .getClusterView();
        assertNotNull(initialClusterView);

        AssertingTopologyEventListener ada = new AssertingTopologyEventListener();
        ada.addExpected(Type.TOPOLOGY_INIT);
        instance.bindTopologyEventListener(ada);
        assertEquals(1, ada.getEvents().size());
        TopologyEvent initEvent = ada.getEvents().remove(0);
        assertNotNull(initEvent);

        assertEquals(initialClusterView.getId(), initEvent.getNewView()
                .getClusterViews().iterator().next().getId());
        assertEquals(initialClusterView.getInstances().get(0).getSlingId(),
                initEvent.getNewView().getLocalInstance().getSlingId());

        // hard assumption that the class we get is an
        // IsolatedInstanceDescription
        // this is because we dont have any established clusterview yet - hence
        // still entirely isolated
        assertEquals(IsolatedInstanceDescription.class, initialClusterView
                .getInstances().get(0).getClass());
        assertEquals(IsolatedInstanceDescription.class, instance
                .getClusterViewService().getClusterView().getInstances().get(0)
                .getClass());
        instance.runHeartbeatOnce();
        Thread.sleep(1000);
        instance.runHeartbeatOnce();
        Thread.sleep(1000);
        assertEquals(0, ada.getEvents().size());

        // after the view was established though, we expect it to be a normal
        // ResourceInstanceDescription
        assertEquals(EstablishedInstanceDescription.class, instance
                .getClusterViewService().getClusterView().getInstances().get(0)
                .getClass());
    }

}
