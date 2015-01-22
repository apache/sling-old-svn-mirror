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

import java.io.IOException;
import java.util.Iterator;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.tools.sling.SlingClient;
import org.apache.sling.testing.tools.sling.SlingInstance;
import org.apache.sling.testing.tools.sling.SlingInstanceManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.apache.sling.distribution.it.DistributionUtils.agentUrl;
import static org.apache.sling.distribution.it.DistributionUtils.assertExists;
import static org.apache.sling.distribution.it.DistributionUtils.assertPostResourceWithParameters;
import static org.apache.sling.distribution.it.DistributionUtils.authorAgentConfigUrl;
import static org.apache.sling.distribution.it.DistributionUtils.exporterUrl;
import static org.apache.sling.distribution.it.DistributionUtils.getResource;
import static org.apache.sling.distribution.it.DistributionUtils.importerUrl;
import static org.apache.sling.distribution.it.DistributionUtils.setArrayProperties;
import static org.junit.Assert.assertFalse;

/**
 * Integration test base class for distribution
 */
public abstract class DistributionIntegrationTestBase {

    protected static SlingInstance author;
    protected static SlingInstance publish;

    protected static SlingClient authorClient;
    protected static SlingClient publishClient;

     static {
        SlingInstanceManager slingInstances = new SlingInstanceManager("author", "publish");
        author = slingInstances.getInstance("author");
        publish = slingInstances.getInstance("publish");

        authorClient = new SlingClient(author.getServerBaseUrl(), author.getServerUsername(), author.getServerPassword());
        publishClient = new SlingClient(publish.getServerBaseUrl(), publish.getServerUsername(), publish.getServerPassword());

        try {
            assertExists(authorClient, authorAgentConfigUrl("publish"));

            // change the url for publish agent and wait for it to start
            String remoteImporterUrl = publish.getServerBaseUrl() + importerUrl("default");


            authorClient.setProperties(authorAgentConfigUrl("publish"),
                    "packageImporter.endpoints", remoteImporterUrl);


            Thread.sleep(3000);

            assertExists(authorClient, agentUrl("publish"));


            assertExists(authorClient, authorAgentConfigUrl("publish-multiple"));
            setArrayProperties(author, authorAgentConfigUrl("publish-multiple"),
                    "packageImporter.endpoints", remoteImporterUrl, remoteImporterUrl + "badaddress");


            Thread.sleep(3000);

            assertExists(authorClient, agentUrl("publish-multiple"));

            assertExists(authorClient, authorAgentConfigUrl("publish-reverse"));

            String remoteExporterUrl = publish.getServerBaseUrl() + exporterUrl("reverse");

            authorClient.setProperties(authorAgentConfigUrl("publish-reverse"), "packageExporter.endpoints", remoteExporterUrl);

            Thread.sleep(3000);
            assertExists(authorClient, agentUrl("publish-reverse"));

            assertExists(publishClient, exporterUrl("reverse"));
            assertExists(publishClient, exporterUrl("default"));
            assertExists(publishClient, importerUrl("default"));


        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }


    @AfterClass
    public static void checkNoPackagesLeft() throws IOException, JSONException {
        if (authorClient.exists("/var/sling/distribution/packages")) {
            JSONObject authorJson = getResource(author, "/var/sling/distribution/packages.1.json");
            Iterator<String> it = authorJson.keys();
            while (it.hasNext()) {
                String key = it.next();
                assertFalse(key.startsWith("distrpackage"));
            }
        }

        if (publishClient.exists("/var/sling/distribution/packages")) {
            JSONObject authorJson = getResource(publish, "/var/sling/distribution/packages.1.json");
            Iterator<String> it = authorJson.keys();
            while (it.hasNext()) {
                String key = it.next();
                assertFalse(key.startsWith("distrpackage"));
            }
        }


    }
    
}
