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
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.UUID;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.impl.cluster.helpers.AcceptsMultiple;
import org.apache.sling.discovery.impl.cluster.helpers.AssertingTopologyEventListener;
import org.apache.sling.discovery.impl.setup.Instance;
import org.apache.sling.discovery.impl.setup.PropertyProviderImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    Instance instance1;
    Instance instance2;
    Instance instance3;

    private String property1Value;

    protected String property2Value;

    private String property1Name;

    private String property2Name;

    @Before
    public void setup() throws Exception {
        logger.debug("here we are");
        instance1 = Instance.newStandaloneInstance("firstInstance", true);
        instance2 = Instance.newClusterInstance("secondInstance", instance1,
                false);
    }

    @After
    public void tearDown() throws Exception {
        if (instance3 != null) {
            instance3.stop();
        }
        instance2.stop();
        instance1.stop();
        instance1 = null;
        instance2 = null;
        instance3 = null;
    }

    @Test
    public void testClusterView() throws Exception {
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNull(instance3);
        instance3 = Instance.newClusterInstance("thirdInstance", instance1,
                false);
        assertNotNull(instance3);

        assertEquals(instance1.getSlingId(), instance1.getClusterViewService()
                .getSlingId());
        assertEquals(instance2.getSlingId(), instance2.getClusterViewService()
                .getSlingId());
        assertEquals(instance3.getSlingId(), instance3.getClusterViewService()
                .getSlingId());

        int numC1 = instance1.getClusterViewService().getClusterView()
                .getInstances().size();
        assertEquals(1, numC1);
        int numC2 = instance2.getClusterViewService().getClusterView()
                .getInstances().size();
        assertEquals(1, numC2);
        int numC3 = instance3.getClusterViewService().getClusterView()
                .getInstances().size();
        assertEquals(1, numC3);

        instance1.dumpRepo();

        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();

        instance1.dumpRepo();
        Thread.sleep(2000);

        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        Thread.sleep(2000);

        instance1.dumpRepo();
        String clusterId1 = instance1.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId1=" + clusterId1);
        String clusterId2 = instance2.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId2=" + clusterId2);
        String clusterId3 = instance3.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId3=" + clusterId3);
        assertEquals(clusterId1, clusterId2);
        assertEquals(clusterId1, clusterId3);

        assertEquals(3, instance1.getClusterViewService().getClusterView()
                .getInstances().size());
        assertEquals(3, instance2.getClusterViewService().getClusterView()
                .getInstances().size());
        assertEquals(3, instance3.getClusterViewService().getClusterView()
                .getInstances().size());
    }

    @Test
    public void testAdditionalInstance() throws Throwable {
        assertNotNull(instance1);
        assertNotNull(instance2);

        assertEquals(instance1.getSlingId(), instance1.getClusterViewService()
                .getSlingId());
        assertEquals(instance2.getSlingId(), instance2.getClusterViewService()
                .getSlingId());

        int numC1 = instance1.getClusterViewService().getClusterView()
                .getInstances().size();
        assertEquals(1, numC1);
        int numC2 = instance2.getClusterViewService().getClusterView()
                .getInstances().size();
        assertEquals(1, numC2);

        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();

        instance1.dumpRepo();
        Thread.sleep(2000);

        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(2000);

        instance1.dumpRepo();
        String clusterId1 = instance1.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId1=" + clusterId1);
        String clusterId2 = instance2.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId2=" + clusterId2);
        assertEquals(clusterId1, clusterId2);

        assertEquals(2, instance1.getClusterViewService().getClusterView()
                .getInstances().size());
        assertEquals(2, instance2.getClusterViewService().getClusterView()
                .getInstances().size());

        AssertingTopologyEventListener assertingTopologyEventListener = new AssertingTopologyEventListener();
        assertingTopologyEventListener.addExpected(Type.TOPOLOGY_INIT);
        assertEquals(1, assertingTopologyEventListener.getRemainingExpectedCount());
        instance1.bindTopologyEventListener(assertingTopologyEventListener);
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());

        // startup instance 3
        AcceptsMultiple acceptsMultiple = new AcceptsMultiple(
                Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED);
        assertingTopologyEventListener.addExpected(acceptsMultiple);
        instance3 = Instance.newClusterInstance("thirdInstance", instance1,
                false);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        Thread.sleep(2000);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        Thread.sleep(2000);
        assertEquals(1, acceptsMultiple.getEventCnt(Type.TOPOLOGY_CHANGED));
    }

    @Test
    public void testPropertyProviders() throws Throwable {
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        assertNull(instance3);
        instance3 = Instance.newClusterInstance("thirdInstance", instance1,
                false);
        instance3.runHeartbeatOnce();
        Thread.sleep(2000);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        Thread.sleep(2000);

        property1Value = UUID.randomUUID().toString();
        property1Name = UUID.randomUUID().toString();
        PropertyProviderImpl pp1 = new PropertyProviderImpl();
        pp1.setProperty(property1Name, property1Value);
        instance1.bindPropertyProvider(pp1, property1Name);

        property2Value = UUID.randomUUID().toString();
        property2Name = UUID.randomUUID().toString();
        PropertyProviderImpl pp2 = new PropertyProviderImpl();
        pp2.setProperty(property2Name, property2Value);
        instance2.bindPropertyProvider(pp2, property2Name);

        assertPropertyValues();

        property1Value = UUID.randomUUID().toString();
        pp1.setProperty(property1Name, property1Value);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();

        assertPropertyValues();
        assertNull(instance1.getClusterViewService().getClusterView()
                .getInstances().get(0)
                .getProperty(UUID.randomUUID().toString()));
        assertNull(instance2.getClusterViewService().getClusterView()
                .getInstances().get(0)
                .getProperty(UUID.randomUUID().toString()));
    }

    private void assertPropertyValues() {
        assertPropertyValues(instance1.getSlingId(), property1Name,
                property1Value);
        assertPropertyValues(instance2.getSlingId(), property2Name,
                property2Value);
    }

    private void assertPropertyValues(String slingId, String name, String value) {
        assertEquals(value, getInstance(instance1, slingId).getProperty(name));
        assertEquals(value, getInstance(instance2, slingId).getProperty(name));
    }

    private InstanceDescription getInstance(Instance instance, String slingId) {
        Iterator<InstanceDescription> it = instance.getClusterViewService()
                .getClusterView().getInstances().iterator();
        while (it.hasNext()) {
            InstanceDescription id = it.next();
            if (id.getSlingId().equals(slingId)) {
                return id;
            }
        }
        throw new IllegalStateException("instance not found: instance="
                + instance + ", slingId=" + slingId);
    }
}
