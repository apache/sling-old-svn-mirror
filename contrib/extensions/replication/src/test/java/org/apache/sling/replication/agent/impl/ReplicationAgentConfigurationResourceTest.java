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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link ReplicationAgentConfigurationResource}
 */
public class ReplicationAgentConfigurationResourceTest {

    @Test
    public void testResourceCreationWithNullConfiguration() throws Exception {
        try {
            ReplicationAgentConfiguration configuration = null;
            ResourceResolver resourceResolver = mock(ResourceResolver.class);
            new ReplicationAgentConfigurationResource(configuration, resourceResolver);
            fail("it should not be possible to create a configuration resource out of a null configuration");
        } catch (Throwable t) {
            // expected
        }
    }

    @Test
    public void testResourceCreationWithAgentConfiguration() throws Exception {
        ReplicationAgentConfiguration agentConfiguration = mock(ReplicationAgentConfiguration.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationAgentConfigurationResource replicationAgentConfigurationResource = new ReplicationAgentConfigurationResource(agentConfiguration, resourceResolver);
        assertNotNull(replicationAgentConfigurationResource.getPath());
        assertEquals(ReplicationAgentConfigurationResource.BASE_PATH + "/null", replicationAgentConfigurationResource.getPath());
        assertNotNull(replicationAgentConfigurationResource.getResourceResolver());
        assertEquals(resourceResolver, replicationAgentConfigurationResource.getResourceResolver());
        assertNotNull(replicationAgentConfigurationResource.getResourceType());
        assertEquals(ReplicationAgentConfigurationResource.RESOURCE_TYPE, replicationAgentConfigurationResource.getResourceType());
        assertNotNull(replicationAgentConfigurationResource.getResourceMetadata());
        assertEquals(ReplicationAgentConfigurationResource.BASE_PATH + "/null", replicationAgentConfigurationResource.getResourceMetadata().getResolutionPath());
        assertNull(replicationAgentConfigurationResource.getResourceSuperType());
    }
}
