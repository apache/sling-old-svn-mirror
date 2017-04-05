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
package org.apache.sling.discovery.base.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.spi.RootLogger;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.mock.AssertingTopologyEventListener;
import org.apache.sling.discovery.base.its.setup.mock.PropertyProviderImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSingleInstanceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    VirtualInstance instance;

    String propertyValue;

    private Level logLevel;

    protected abstract VirtualInstanceBuilder newBuilder();

    @Before
    public void setup() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = RootLogger.getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.DEBUG);
        logger.info("setup: creating new standalone instance");
        instance = newBuilder().setDebugName("standaloneInstance")
                .newRepository("/var/discovery/impl/", true)
                .setConnectorPingTimeout(20)
                .setConnectorPingInterval(999)/*long enough heartbeat interval to prevent them to disturb the explicit heartbeats during the test*/
                .setMinEventDelay(3).build();
        logger.info("setup: creating new standalone instance done.");
    }

    @After
    public void tearDown() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = RootLogger.getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);
        logger.info("tearDown: stopping standalone instance");
        if (instance!=null) {
            instance.stop();
            instance = null;
        }
        logger.info("tearDown: stopping standalone instance done");
    }

    @Test
    public void testGetters() throws UndefinedClusterViewException, InterruptedException {
        logger.info("testGetters: start");
        assertNotNull(instance);
        logger.info("sling id=" + instance.getSlingId());
        try{
            instance.getClusterViewService().getLocalClusterView();
            fail("should complain"); // SLING-5030
        } catch(UndefinedClusterViewException e) {
            // ok
        }

        instance.heartbeatsAndCheckView();
        // wait 4000ms for the vote to happen
        Thread.sleep(4000);

        assertNotNull(instance.getClusterViewService().getLocalClusterView());
        ClusterView cv = instance.getClusterViewService().getLocalClusterView();
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
        logger.info("testGetters: end");
    }

    @Test
    public void testPropertyProviders() throws Throwable {
        logger.info("testPropertyProviders: start");
        final String propertyName = UUID.randomUUID().toString();
        propertyValue = UUID.randomUUID().toString();
        PropertyProviderImpl pp = new PropertyProviderImpl();
        pp.setProperty(propertyName, propertyValue);
        instance.bindPropertyProvider(pp, propertyName);

        instance.heartbeatsAndCheckView();
        // wait 4000ms for the vote to happen
        Thread.sleep(4000);
        assertEquals(propertyValue,
                instance.getClusterViewService().getLocalClusterView()
                        .getInstances().get(0).getProperty(propertyName));

        propertyValue = UUID.randomUUID().toString();
        pp.setProperty(propertyName, propertyValue);
        instance.heartbeatsAndCheckView();

        assertEquals(propertyValue,
                instance.getClusterViewService().getLocalClusterView()
                        .getInstances().get(0).getProperty(propertyName));
        assertNull(instance.getClusterViewService().getLocalClusterView()
                .getInstances().get(0)
                .getProperty(UUID.randomUUID().toString()));
        logger.info("testPropertyProviders: end");
    }

    @Test
    public void testInvalidProperties() throws Throwable {
        logger.info("testInvalidProperties: start");

        instance.heartbeatsAndCheckView();
        instance.heartbeatsAndCheckView();

        final String propertyValue = UUID.randomUUID().toString();
        Thread.sleep(2000);
        doTestProperty(UUID.randomUUID().toString(), propertyValue, propertyValue);

        doTestProperty("", propertyValue, null);
        doTestProperty("-", propertyValue, propertyValue);
        doTestProperty("_", propertyValue, propertyValue);
        doTestProperty("jcr:" + UUID.randomUUID().toString(), propertyValue, null);
        doTestProperty("var/" + UUID.randomUUID().toString(), propertyValue, null);
        doTestProperty(UUID.randomUUID().toString() + "@test", propertyValue, null);
        doTestProperty(UUID.randomUUID().toString() + "!test", propertyValue, null);
        logger.info("testInvalidProperties: end");
    }

	private void doTestProperty(final String propertyName,
			final String propertyValue,
			final String expectedPropertyValue) throws Throwable {
		PropertyProviderImpl pp = new PropertyProviderImpl();
        pp.setProperty(propertyName, propertyValue);
        instance.bindPropertyProvider(pp, propertyName);
        assertEquals(expectedPropertyValue,
                instance.getClusterViewService().getLocalClusterView()
                        .getInstances().get(0).getProperty(propertyName));
	}

    @Test
    public void testTopologyEventListeners() throws Throwable {
        logger.info("testTopologyEventListeners: start");
        instance.heartbeatsAndCheckView();
        logger.info("testTopologyEventListeners: 1st sleep 2s");
        Thread.sleep(2000);
        instance.heartbeatsAndCheckView();
        logger.info("testTopologyEventListeners: 2nd sleep 2s");
        Thread.sleep(2000);

        AssertingTopologyEventListener assertingTopologyEventListener = new AssertingTopologyEventListener();
        assertingTopologyEventListener.addExpected(Type.TOPOLOGY_INIT);
        logger.info("testTopologyEventListeners: binding the event listener");
        instance.bindTopologyEventListener(assertingTopologyEventListener);
        Thread.sleep(1000); // SLING-4755: async event sending requires some minimal wait time nowadays
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());

        final String propertyName = UUID.randomUUID().toString();
        propertyValue = UUID.randomUUID().toString();
        PropertyProviderImpl pp = new PropertyProviderImpl();
        pp.setProperty(propertyName, propertyValue);

        assertingTopologyEventListener.addExpected(Type.PROPERTIES_CHANGED);

        assertEquals(1, assertingTopologyEventListener.getRemainingExpectedCount());
        assertEquals(0, pp.getGetCnt());
        instance.bindPropertyProvider(pp, propertyName);
        logger.info("testTopologyEventListeners: 3rd sleep 1.5s");
        Thread.sleep(1500);
        logger.info("testTopologyEventListeners: dumping due to failure: ");
        assertingTopologyEventListener.dump();
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
        instance.heartbeatsAndCheckView();
        logger.info("testTopologyEventListeners: 4th sleep 2s");
        Thread.sleep(2000);
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());
        assertEquals(2, pp.getGetCnt());

        // a heartbeat repeat should not result in another call though
        instance.heartbeatsAndCheckView();
        logger.info("testTopologyEventListeners: 5th sleep 2s");
        Thread.sleep(2000);
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());
        assertEquals(3, pp.getGetCnt());
        logger.info("testTopologyEventListeners: done");
    }

    @Test
    public void testBootstrap() throws Throwable {
        logger.info("testBootstrap: start");
        try{
            instance.getClusterViewService().getLocalClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // SLING-5030 : isolated mode is gone, replaced with exception
            // ok
        }

        // SLING-3750 : with delaying the init event, we now should NOT get any events
        // before we let the view establish (which happens via heartbeats below)
        AssertingTopologyEventListener ada = new AssertingTopologyEventListener();
        instance.bindTopologyEventListener(ada);
        assertEquals(0, ada.getEvents().size());
        assertEquals(0, ada.getUnexpectedCount());

        try{
            instance.getClusterViewService().getLocalClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }

        ada.addExpected(Type.TOPOLOGY_INIT);
        instance.heartbeatsAndCheckView();
        Thread.sleep(1000);
        instance.heartbeatsAndCheckView();
        Thread.sleep(1000);
        logger.info("testBoostrap: dumping repo...");
        instance.dumpRepo();
        logger.info("testBoostrap: dumping listener...");
        ada.dump();
        assertEquals(0, ada.getUnexpectedCount());
        assertEquals(1, ada.getEvents().size());
        TopologyEvent initEvent = ada.getEvents().remove(0);
        assertNotNull(initEvent);
        assertNotNull(initEvent.getNewView());
        assertNotNull(initEvent.getNewView().getClusterViews());

        // after the view was established though, we expect it to be a normal
        // EstablishedInstanceDescription
        instance.assertEstablishedView();
        logger.info("testBootstrap: end");
    }

}
