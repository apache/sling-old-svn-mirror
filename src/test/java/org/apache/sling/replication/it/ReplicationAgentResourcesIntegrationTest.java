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
import org.junit.Test;

/**
 * Integration test for {@link org.apache.sling.replication.agent.ReplicationAgent} resources
 */
public class ReplicationAgentResourcesIntegrationTest extends ReplicationITBase {

    @Test
    public void testDefaultAgentConfigurationResources() throws IOException {
        String[] defaultAgentNames = new String[]{
                "publish",
                "publish-reverse",
                "reverserepo",
                "author",
                "cache-flush"
        };
        for (String agentName : defaultAgentNames) {
            assertResourceExists(getAgentConfigUrl(agentName));
        }

    }

    @Test
    public void testDefaultPublishAgentResources() throws IOException {
        // these agents do not exist as they are bundled to publish runMode
        String[] defaultPublishAgentNames = new String[]{
                "reverserepo",
                "author",
                "cache-flush"
        };
        for (String agentName : defaultPublishAgentNames) {
            assertResourceDoesNotExist(getAgentUrl(agentName));
        }
    }

    @Test
    public void testDefaultAuthorAgentResources() throws IOException {
        // these agents exist as they are bundled to author runMode
        String[] defaultAuthorAgentNames = new String[]{
                "publish",
                "publish-reverse"
        };
        for (String agentName : defaultAuthorAgentNames) {
            assertResourceExists(getAgentUrl(agentName));
        }
    }

    @Test
    public void testDefaultPublishAgentQueueResources() throws IOException {
        // these agent queues do not exist as they are bundled to publish runMode
        String[] defaultPublishAgentNames = new String[]{
                "reverserepo",
                "author",
                "cache-flush"
        };
        for (String agentName : defaultPublishAgentNames) {
            assertResourceDoesNotExist(getAgentUrl(agentName)+"/queue");
        }
    }

    @Test
    public void testDefaultAuthorAgentQueueResources() throws IOException {
        // these agent queues exist as they are bundled to author runMode
        String[] defaultAuthorAgentNames = new String[]{
                "publish",
                "publish-reverse"
        };
        for (String agentName : defaultAuthorAgentNames) {
            assertResourceExists(getAgentUrl(agentName)+"/queue");
        }
    }

    @Test
    public void testDefaultAgentsRootResource() throws Exception {
        String rootResource = getAgentRootUrl();
        assertResourceExists(rootResource);
        assertJsonResponseContains(rootResource,
                "sling:resourceType", "replication/agents",
                "items", "[\"publish-reverse\",\"publish\"]");
    }

    @Test
    public void testAgentConfigurationResourceCreate() throws Exception {
        String agentName = "sample-create-config";
        String newConfigResource = getAgentConfigUrl(agentName);

        assertPostResourceWithParameters(201, newConfigResource, "name", agentName);
        assertResourceExists(newConfigResource);
        assertJsonResponseContains(newConfigResource,
                "sling:resourceType", "replication/config/agent",
                "name", agentName);
    }

    @Test
    public void testAgentConfigurationResourceDelete() throws Exception {
        String agentName = "sample-delete-config";
        String newConfigResource = getAgentConfigUrl(agentName);
        assertPostResourceWithParameters(201, newConfigResource, "name", agentName);
        assertResourceExists(newConfigResource);
        assertPostResourceWithParameters(200, newConfigResource, ":operation", "delete");
        assertResourceDoesNotExist(newConfigResource);
    }


    @Test
    public void testAgentConfigurationResourceUpdate() throws Exception {
        String agentName = "sample-update-config";
        String newConfigResource = getAgentConfigUrl(agentName);
        assertPostResourceWithParameters(201, newConfigResource, "name", agentName);
        assertResourceExists(newConfigResource);
        assertJsonResponseContains(newConfigResource,
                "sling:resourceType", "replication/config/agent",
                "name", agentName);
    }
}