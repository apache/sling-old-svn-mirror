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

import org.apache.sling.testing.tools.sling.SlingClient;
import org.apache.sling.testing.tools.sling.SlingInstance;
import org.apache.sling.testing.tools.sling.SlingInstanceManager;

import static org.apache.sling.replication.it.ReplicationUtils.assertExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration test base class for replication
 */
public abstract class ReplicationIntegrationTestBase {

    static SlingInstance author;
    static SlingInstance publish;

    static SlingClient authorClient;
    static SlingClient publishClient;

    static {
        SlingInstanceManager slingInstances = new SlingInstanceManager("author", "publish");
        author = slingInstances.getInstance("author");
        publish = slingInstances.getInstance("publish");

        authorClient = new SlingClient(author.getServerBaseUrl(), author.getServerUsername(), author.getServerPassword());
        publishClient = new SlingClient(publish.getServerBaseUrl(), publish.getServerUsername(), publish.getServerPassword());


        try {
            // change the url for publish agent and wait for it to start

            String receiverUrl = "http://localhost:4503/libs/sling/replication/importer/default"
                    .replace("http://localhost:4503", publish.getServerBaseUrl());
            authorClient.setProperties("/libs/sling/replication/config/transport/http/http-publish-receive",
                    "endpoints", receiverUrl);
            assertExists(authorClient, "/libs/sling/replication/agent/publish");
        }
        catch (Exception ex) {

        }

    }

}
