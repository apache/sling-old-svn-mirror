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
package org.apache.sling.replication.it;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import static org.apache.sling.replication.it.ReplicationUtils.*;
/**
 * Integration test for {@link org.apache.sling.replication.agent.ReplicationAgent} resources
 */
public class ReplicationAgentResourcesIntegrationTest extends ReplicationIntegrationTestBase {

    @Test
    public void testDefaultAgentConfigurationResources() throws Exception {
        String[] defaultAgentNames = new String[]{
                "publish",
                "publish-reverse",
                "reverserepo",
                "author",
                "cache-flush"
        };
        for (String agentName : defaultAgentNames) {
            assertExits(authorClient, agentConfigUrl(agentName));
        }

    }

    @Test
    public void testDefaultPublishAgentResources() throws Exception {
        // these agents do not exist as they are bundled to publish runMode
        String[] defaultPublishAgentNames = new String[]{
                "reverserepo",
                "author",
                "cache-flush"
        };
        for (String agentName : defaultPublishAgentNames) {
            assertNotExits(authorClient, agentUrl(agentName));
        }
    }

    @Test
    public void testDefaultAuthorAgentResources() throws Exception {
        // these agents exist as they are bundled to author runMode
        String[] defaultAuthorAgentNames = new String[]{
                "publish",
                "publish-reverse"
        };
        for (String agentName : defaultAuthorAgentNames) {
            assertExits(authorClient, agentUrl(agentName));
        }
    }

    @Test
    public void testDefaultPublishAgentQueueResources() throws Exception {
        // these agent queues do not exist as they are bundled to publish runMode
        String[] defaultPublishAgentNames = new String[]{
                "reverserepo",
                "author",
                "cache-flush"
        };
        for (String agentName : defaultPublishAgentNames) {
            assertNotExits(authorClient, queueUrl(agentName));
        }
    }

    @Test
    public void testDefaultAuthorAgentQueueResources() throws Exception {
        // these agent queues exist as they are bundled to author runMode
        String[] defaultAuthorAgentNames = new String[]{
                "publish",
                "publish-reverse"
        };
        for (String agentName : defaultAuthorAgentNames) {
            assertExits(authorClient, queueUrl(agentName));
        }
    }

    @Test
    public void testDefaultAgentsRootResource() throws Exception {
        assertExits(authorClient, agentRootUrl());
        assertResponseContains(author, agentRootUrl(),
                "sling:resourceType", "replication/agents",
                "items", "publish-reverse","publish");
    }

    @Test
    public void testAgentConfigurationResourceCreate() throws Exception {
        String agentName = "sample-create-config" + UUID.randomUUID();
        String newConfigResource = agentConfigUrl(agentName);

        authorClient.createNode(newConfigResource, "name", agentName, "transportHandler", "(name=author)");
        assertExits(authorClient, newConfigResource);
        assertResponseContains(author, newConfigResource,
                "sling:resourceType", "replication/config/agent",
                "name", agentName,
                "transportHandler", "(name=author)");
    }

    @Test
    public void testAgentConfigurationResourceDelete() throws Exception {
        String agentName = "sample-delete-config" + UUID.randomUUID();
        String newConfigResource = agentConfigUrl(agentName);
        authorClient.createNode(newConfigResource, "name", agentName, "transportHandler", "(name=author)");
        assertExits(authorClient, newConfigResource);

        deleteNode(author, newConfigResource);
        // authorClient.delete does not work for some reason
        assertNotExits(authorClient, newConfigResource);
    }


    @Test
    public void testAgentConfigurationResourceUpdate() throws Exception {
        String agentName = "sample-create-config" + UUID.randomUUID();
        String newConfigResource = agentConfigUrl(agentName);

        authorClient.createNode(newConfigResource, "name", agentName, "transportHandler", "(name=author)");
        assertExits(authorClient, newConfigResource);
        authorClient.setProperties(newConfigResource, "transportHandler", "(name=updated)");
        assertResponseContains(author, newConfigResource,
                "sling:resourceType", "replication/config/agent",
                "name", agentName,
                "transportHandler", "(name=updated)");
    }

}