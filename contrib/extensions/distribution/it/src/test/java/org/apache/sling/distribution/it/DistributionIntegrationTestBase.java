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
import static org.apache.sling.distribution.it.DistributionUtils.assertEmptyFolder;
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

            String remoteImporterUrl = publish.getServerBaseUrl() + importerUrl("default");
            String remoteExporterUrl = publish.getServerBaseUrl() + exporterUrl("reverse");



            {
                assertExists(authorClient, authorAgentConfigUrl("publish"));

                authorClient.setProperties(authorAgentConfigUrl("publish"),
                        "packageImporter.endpoints", remoteImporterUrl);


                Thread.sleep(1000);

                assertExists(authorClient, agentUrl("publish"));
            }


            {
                assertExists(authorClient, authorAgentConfigUrl("publish-multiple"));
                setArrayProperties(author, authorAgentConfigUrl("publish-multiple"),
                        "packageImporter.endpoints", remoteImporterUrl, remoteImporterUrl + "badaddress");


                Thread.sleep(1000);

                assertExists(authorClient, agentUrl("publish-multiple"));
                assertExists(authorClient, exporterUrl("publish-multiple-passivequeue1"));

            }

            {
                assertExists(authorClient, authorAgentConfigUrl("publish-selective"));

                setArrayProperties(author, authorAgentConfigUrl("publish-selective"),
                        "packageImporter.endpoints", "publisher1=" + remoteImporterUrl);

                Thread.sleep(1000);
                assertExists(authorClient, agentUrl("publish-selective"));
            }

            {
                assertExists(authorClient, authorAgentConfigUrl("publish-reverse"));

                authorClient.setProperties(authorAgentConfigUrl("publish-reverse"), "packageExporter.endpoints", remoteExporterUrl);

                Thread.sleep(1000);
                assertExists(authorClient, agentUrl("publish-reverse"));

                assertExists(publishClient, exporterUrl("reverse"));
                assertExists(publishClient, exporterUrl("default"));
                assertExists(publishClient, importerUrl("default"));
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }


    @AfterClass
    public static void checkNoPackagesLeft() throws IOException, JSONException {


        assertEmptyFolder(author, authorClient, "/var/sling/distribution/packages");
        assertEmptyFolder(author, authorClient, "/etc/packages/sling/distribution");
        assertEmptyFolder(author, authorClient, "/var/sling/distribution/jcrpackages");


        assertEmptyFolder(publish, publishClient, "/var/sling/distribution/packages");
        assertEmptyFolder(publish, publishClient, "/etc/packages/sling/distribution");
        assertEmptyFolder(publish, publishClient, "/var/sling/distribution/jcrpackages");


    }
    
}
