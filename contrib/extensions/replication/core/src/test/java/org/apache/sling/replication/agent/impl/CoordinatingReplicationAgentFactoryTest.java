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

import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.component.ReplicationComponentFactory;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.agent.impl.CoordinatingReplicationAgentFactory}
 */
public class CoordinatingReplicationAgentFactoryTest {

    @Test
    public void testActivationWithoutConfig() throws Exception {
        CoordinatingReplicationAgentFactory coordinatingReplicationAgentFactory = new CoordinatingReplicationAgentFactory();
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> config = new HashMap<String, Object>();
        try {
            config.put(CoordinatingReplicationAgentFactory.NAME, "agentName");
            coordinatingReplicationAgentFactory.activate(context, config);
            fail("cannot activate a coordinate agents without exporters/importers");
        } catch (IllegalArgumentException e) {
            // expected to fail
        }
    }

    @Test
    public void testActivationWithEmptyImportersAndExporters() throws Exception {
        CoordinatingReplicationAgentFactory coordinatingReplicationAgentFactory = new CoordinatingReplicationAgentFactory();
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(CoordinatingReplicationAgentFactory.NAME, "agentName");
        config.put(CoordinatingReplicationAgentFactory.PACKAGE_IMPORTER, new String[]{});
        config.put(CoordinatingReplicationAgentFactory.PACKAGE_EXPORTER, new String[]{});
        try {
            coordinatingReplicationAgentFactory.activate(context, config);
            fail("cannot activate a coordinate agents with empty exporters/importers");
        } catch (IllegalArgumentException e) {
            // expected to fail
        }
    }

    @Test
    public void testActivationWithImportersAndExporters() throws Exception {


        BundleContext context = mock(BundleContext.class);
        Map<String, Object> config = new HashMap<String, Object>();

        config.put(CoordinatingReplicationAgentFactory.NAME, "agentName");
        config.put(CoordinatingReplicationAgentFactory.PACKAGE_IMPORTER, new String[]{"packageBuilder/type=vlt",
                "endpoints[0]=http://host:101/libs/sling/replication/services/exporters/reverse-101",
                "endpoints[1]=http://host:102/libs/sling/replication/services/exporters/reverse-102",
                "endpoints[2]=http://host:103/libs/sling/replication/services/exporters/reverse-103",
                "endpoints.strategy=All"});
        config.put(CoordinatingReplicationAgentFactory.PACKAGE_EXPORTER, new String[]{"packageBuilder/type=vlt",
                "endpoints[0]=http://localhost:101/libs/sling/replication/services/importers/default",
                "endpoints[1]=http://localhost:102/libs/sling/replication/services/importers/default",
                "endpoints[2]=http://localhost:103/libs/sling/replication/services/importers/default",
                "endpoints.strategy=All"});
        ReplicationComponentFactory replicationComponentFactory = mock(ReplicationComponentFactory.class);
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        CoordinatingReplicationAgentFactory coordinatingReplicationAgentFactory = new CoordinatingReplicationAgentFactory();
        when(replicationComponentFactory.createComponent(ReplicationAgent.class, config, coordinatingReplicationAgentFactory)).
                thenReturn(replicationAgent);

        Field componentFactory = coordinatingReplicationAgentFactory.getClass().getDeclaredField("componentFactory");
        componentFactory.setAccessible(true);
        componentFactory.set(coordinatingReplicationAgentFactory, replicationComponentFactory);

        coordinatingReplicationAgentFactory.activate(context, config);
    }

}