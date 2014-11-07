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
package org.apache.sling.distribution.it;

import org.apache.sling.testing.tools.sling.SlingClient;
import org.apache.sling.testing.tools.sling.SlingInstance;
import org.apache.sling.testing.tools.sling.SlingInstanceManager;
import org.junit.BeforeClass;

import static org.apache.sling.distribution.it.DistributionUtils.agentConfigUrl;
import static org.apache.sling.distribution.it.DistributionUtils.agentUrl;
import static org.apache.sling.distribution.it.DistributionUtils.assertExists;
import static org.apache.sling.distribution.it.DistributionUtils.exporterUrl;
import static org.apache.sling.distribution.it.DistributionUtils.importerUrl;
import static org.apache.sling.distribution.it.DistributionUtils.setAgentProperties;

/**
 * Integration test base class for distribution
 */
public abstract class DistributionIntegrationTestBase {

    protected static SlingInstance author;
    protected static SlingInstance publish;

    protected static SlingClient authorClient;
    protected static SlingClient publishClient;

    @BeforeClass
    public static void setUpBefore() {
        SlingInstanceManager slingInstances = new SlingInstanceManager("author", "publish");
        author = slingInstances.getInstance("author");
        publish = slingInstances.getInstance("publish");

        authorClient = new SlingClient(author.getServerBaseUrl(), author.getServerUsername(), author.getServerPassword());
        publishClient = new SlingClient(publish.getServerBaseUrl(), publish.getServerUsername(), publish.getServerPassword());

        try {
            assertExists(authorClient, agentConfigUrl("publish"));
            assertExists(authorClient, agentConfigUrl("publish-reverse"));

            // change the url for publish agent and wait for it to start
            String remoteImporterUrl = publish.getServerBaseUrl() + importerUrl("default");

            setAgentProperties(author, "publish",
                    "packageImporter", "type=remote",
                    "packageImporter", "endpoints[0]=" + remoteImporterUrl,
                    "packageImporter", "packageBuilder/type=vlt");

            Thread.sleep(3000);

            assertExists(authorClient, agentUrl("publish"));

            assertExists(authorClient, agentConfigUrl("publish-reverse"));

            String remoteExporterUrl = publish.getServerBaseUrl() + exporterUrl("reverse");
            setAgentProperties(author, "publish-reverse",
                    "packageExporter", "type=remote",
                    "packageExporter", "endpoints[0]=" + remoteExporterUrl,

                    "packageExporter", "packageBuilder/type=vlt");

            Thread.sleep(3000);
            assertExists(authorClient, agentUrl("publish-reverse"));

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

}
