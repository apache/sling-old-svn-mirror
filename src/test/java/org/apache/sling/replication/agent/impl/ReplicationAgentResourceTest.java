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
import org.apache.sling.replication.agent.ReplicationAgent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link ReplicationAgentResource}
 */
public class ReplicationAgentResourceTest {

    @Test
    public void testResourceCreationWithNullAgent() throws Exception {
        try {
            ReplicationAgent agent = null;
            ResourceResolver resourceResolver = mock(ResourceResolver.class);
            new ReplicationAgentResource(agent, resourceResolver);
            fail("it should not be possible to create an agent resource out of a null agent");
        } catch (Throwable t) {
            // expected
        }
    }

    @Test
    public void testResourceCreationWithAgent() throws Exception {
        ReplicationAgent agent = mock(ReplicationAgent.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationAgentResource replicationAgentResource = new ReplicationAgentResource(agent, resourceResolver);
        assertNotNull(replicationAgentResource.getPath());
        assertEquals(ReplicationAgentResource.BASE_PATH +"/null", replicationAgentResource.getPath());
        assertNotNull(replicationAgentResource.getResourceResolver());
        assertEquals(resourceResolver, replicationAgentResource.getResourceResolver());
        assertNotNull(replicationAgentResource.getResourceType());
        assertEquals(ReplicationAgentResource.RESOURCE_TYPE, replicationAgentResource.getResourceType());
        assertNotNull(replicationAgentResource.getResourceMetadata());
        assertEquals(ReplicationAgentResource.BASE_PATH +"/null", replicationAgentResource.getResourceMetadata().getResolutionPath());
        assertNull(replicationAgentResource.getResourceSuperType());
    }
}
