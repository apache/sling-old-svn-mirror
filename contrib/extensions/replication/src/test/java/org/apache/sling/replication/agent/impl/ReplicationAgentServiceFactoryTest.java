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
package org.apache.sling.replication.agent.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.transport.TransportHandler;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link ReplicationAgentServiceFactory}
 */
public class ReplicationAgentServiceFactoryTest {

    @Test
    public void testActivationWithAllServicesAndPropertiesBound() throws Exception {
        ReplicationAgentServiceFactory serviceFactory = new ReplicationAgentServiceFactory();

        Field packageBuilderField = serviceFactory.getClass().getDeclaredField("packageBuilder");
        packageBuilderField.setAccessible(true);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        packageBuilderField.set(serviceFactory, packageBuilder);

        Field distributionField = serviceFactory.getClass().getDeclaredField("queueDistributionStrategy");
        distributionField.setAccessible(true);
        ReplicationQueueDistributionStrategy distributionStrategy = mock(ReplicationQueueDistributionStrategy.class);
        distributionField.set(serviceFactory, distributionStrategy);

        Field queueField = serviceFactory.getClass().getDeclaredField("queueProvider");
        queueField.setAccessible(true);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        queueField.set(serviceFactory, queueProvider);

        Field transportField = serviceFactory.getClass().getDeclaredField("transportHandler");
        transportField.setAccessible(true);
        TransportHandler transportHandler = mock(TransportHandler.class);
        transportField.set(serviceFactory, transportHandler);


        Map<String, Object> dictionary = new HashMap<String, Object>();
        dictionary.put("endpoint", "http://somewhere.com");
        dictionary.put("name", "agent1");
        BundleContext context = mock(BundleContext.class);
        serviceFactory.activate(context, dictionary);
    }

    @Test
    public void testActivationWithNoServicesBound() throws Exception {
        try {
            ReplicationAgentServiceFactory serviceFactory = new ReplicationAgentServiceFactory();

            Map<String, Object> dictionary = new HashMap<String, Object>();
            BundleContext context = mock(BundleContext.class);
            serviceFactory.activate(context, dictionary);
            fail("missing / incomplete configuration should trigger agent configuration exception");
        } catch (AgentConfigurationException e) {
            // expected
        }
    }
}
