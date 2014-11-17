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
package org.apache.sling.distribution.agent.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.impl.DefaultDistributionComponentFactoryConstants;
import org.apache.sling.distribution.component.impl.DistributionComponentFactoryManager;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link CoordinatingDistributionAgentFactory}
 */
public class CoordinatingDistributionAgentFactoryTest {

    @Test
    public void testActivationWithoutConfig() throws Exception {
        CoordinatingDistributionAgentFactory coordinatingdistributionAgentFactory = new CoordinatingDistributionAgentFactory();
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> config = new HashMap<String, Object>();
        try {
            config.put(CoordinatingDistributionAgentFactory.NAME, "agentName");
            coordinatingdistributionAgentFactory.activate(context, config);
            fail("cannot activate a coordinate agents without exporters/importers");
        } catch (IllegalArgumentException e) {
            // expected to fail
        }
    }

    @Test
    public void testActivationWithEmptyImportersAndExporters() throws Exception {
        CoordinatingDistributionAgentFactory coordinatingdistributionAgentFactory = new CoordinatingDistributionAgentFactory();
        BundleContext context = mock(BundleContext.class);
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(CoordinatingDistributionAgentFactory.NAME, "agentName");
        config.put(CoordinatingDistributionAgentFactory.PACKAGE_IMPORTER, new String[]{});
        config.put(CoordinatingDistributionAgentFactory.PACKAGE_EXPORTER, new String[]{});
        try {
            coordinatingdistributionAgentFactory.activate(context, config);
            fail("cannot activate a coordinate agents with empty exporters/importers");
        } catch (IllegalArgumentException e) {
            // expected to fail
        }
    }

    @Test
    public void testActivationWithImportersAndExporters() throws Exception {


        BundleContext context = mock(BundleContext.class);
        Map<String, Object> config = new HashMap<String, Object>();

        config.put(CoordinatingDistributionAgentFactory.NAME, "agentName");
        config.put(CoordinatingDistributionAgentFactory.PACKAGE_IMPORTER, new String[]{"packageBuilder/type=vlt",
                "endpoints[0]=http://host:101/libs/sling/distribution/services/exporters/reverse-101",
                "endpoints[1]=http://host:102/libs/sling/distribution/services/exporters/reverse-102",
                "endpoints[2]=http://host:103/libs/sling/distribution/services/exporters/reverse-103",
                "endpoints.strategy=All"});
        config.put(CoordinatingDistributionAgentFactory.PACKAGE_EXPORTER, new String[]{"packageBuilder/type=vlt",
                "endpoints[0]=http://localhost:101/libs/sling/distribution/services/importers/default",
                "endpoints[1]=http://localhost:102/libs/sling/distribution/services/importers/default",
                "endpoints[2]=http://localhost:103/libs/sling/distribution/services/importers/default",
                "endpoints.strategy=All"});
        DistributionComponentFactoryManager componentManager = mock(DistributionComponentFactoryManager.class);
        DistributionAgent distributionAgent = mock(DistributionAgent.class);
        CoordinatingDistributionAgentFactory coordinatingdistributionAgentFactory = new CoordinatingDistributionAgentFactory();

        config.put(DefaultDistributionComponentFactoryConstants.COMPONENT_PROVIDER, coordinatingdistributionAgentFactory);
        when(componentManager.createComponent(DistributionAgent.class, config)).
                thenReturn(distributionAgent);

        Field componentManagerField= coordinatingdistributionAgentFactory.getClass().getDeclaredField("componentManager");
        componentManagerField.setAccessible(true);
        componentManagerField.set(coordinatingdistributionAgentFactory, componentManager);

        coordinatingdistributionAgentFactory.activate(context, config);
    }

}