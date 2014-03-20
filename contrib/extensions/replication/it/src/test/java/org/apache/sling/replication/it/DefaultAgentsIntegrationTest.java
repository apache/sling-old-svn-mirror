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
 * Integration test for default replication agents.
 */
public class DefaultAgentsIntegrationTest extends ReplicationITBase {

    @Test
    public void testDefaultAgentConfigurationResources() throws IOException {
        String[] defaultAgentConfigPaths = new String[]{
                "/libs/sling/replication/config/agent/publish.json",
                "/libs/sling/replication/config/agent/publish-reverse.json",
                "/libs/sling/replication/config/agent/reverserepo.json",
                "/libs/sling/replication/config/agent/author.json",
                "/libs/sling/replication/config/agent/cache-flush.json"
        };
        for (String path : defaultAgentConfigPaths) {
            assertResourceExists(path);
        }

    }

    @Test
    public void testDefaultPublishAgentResources() throws IOException {
        // these agents do not exist as they are bundled to publish runMode
        String[] defaultPublishAgents = new String[]{
                "/libs/sling/replication/agent/reverserepo.json",
                "/libs/sling/replication/agent/author.json",
                "/libs/sling/replication/agent/cache-flush.json"
        };
        for (String path : defaultPublishAgents) {
            assertResourceDoesNotExist(path);
        }
    }

    @Test
    public void testDefaultAuthorAgentResources() throws IOException {
        // these agents exist as they are bundled to author runMode
        String[] defaultAuthorAgents = new String[]{
                "/libs/sling/replication/agent/publish.json",
                "/libs/sling/replication/agent/publish-reverse.json",
        };
        for (String path : defaultAuthorAgents) {
            assertResourceExists(path);
        }
    }

    @Test
    public void testDefaultPublishAgentQueueResources() throws IOException {
        // these agent queues do not exist as they are bundled to publish runMode
        String[] defaultPublishAgents = new String[]{
                "/libs/sling/replication/agent/reverserepo/queue.json",
                "/libs/sling/replication/agent/author/queue.json",
                "/libs/sling/replication/agent/cache-flush/queue.json"
        };
        for (String path : defaultPublishAgents) {
            assertResourceDoesNotExist(path);
        }
    }

    @Test
    public void testDefaultAuthorAgentQueueResources() throws IOException {
        // these agent queues exist as they are bundled to author runMode
        String[] defaultAuthorAgents = new String[]{
                "/libs/sling/replication/agent/publish/queue.json",
                "/libs/sling/replication/agent/publish-reverse/queue.json",
        };
        for (String path : defaultAuthorAgents) {
            assertResourceExists(path);
        }
    }

    @Test
    public void testDefaultAgentsRootResource() throws Exception {
        assertResourceExists("/libs/sling/replication/agent.json");
    }
}