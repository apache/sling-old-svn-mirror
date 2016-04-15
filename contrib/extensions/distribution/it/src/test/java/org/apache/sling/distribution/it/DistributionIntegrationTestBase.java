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
import org.junit.After;
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

    protected SlingInstance author;
    protected SlingInstance publish;

    protected SlingClient authorClient;
    protected SlingClient publishClient;

    static SlingInstanceManager slingInstancesManager = null;
    static SlingInstanceManager sharedSlingInstancesManager = null;

    protected DistributionIntegrationTestBase() {
        this(false);
    }

    protected DistributionIntegrationTestBase(boolean useShared) {
        init(useShared);
    }


    synchronized void init(boolean useShared) {

        if (useShared) {
            if (sharedSlingInstancesManager == null) {
                sharedSlingInstancesManager = new SlingInstanceManager("author-shared", "publish-shared") ;

            }
            author = sharedSlingInstancesManager.getInstance("author-shared");
            publish = sharedSlingInstancesManager.getInstance("publish-shared");

        } else {
            if (slingInstancesManager == null) {
                slingInstancesManager = new SlingInstanceManager("author", "publish");

            }

            author = slingInstancesManager.getInstance("author");
            publish = slingInstancesManager.getInstance("publish");
        }

        authorClient = new SlingClient(author.getServerBaseUrl(), author.getServerUsername(), author.getServerPassword());
        publishClient = new SlingClient(publish.getServerBaseUrl(), publish.getServerUsername(), publish.getServerPassword());

        try {

            String remoteImporterUrl = publish.getServerBaseUrl() + importerUrl("default");



            registerPublish("publish", "default");
            registerPublish("impersonate-publish", "default");

            registerReverse("publish-reverse", "reverse");
            registerReverse("impersonate-publish-reverse", "impersonate-reverse");

            {
                assertExists(authorClient, authorAgentConfigUrl("publish-multiple"));
                setArrayProperties(author, authorAgentConfigUrl("publish-multiple"),
                        "packageImporter.endpoints", "endpoint1=" + remoteImporterUrl, "endpoint2=" + remoteImporterUrl + "badaddress");


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



        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }


    @After
    public void checkNoPackagesLeft() throws IOException, JSONException {


        assertEmptyFolder(author, authorClient, "/var/sling/distribution/packages/default/shared");
        assertEmptyFolder(author, authorClient, "/var/sling/distribution/packages/default/data");
        assertEmptyFolder(author, authorClient, "/etc/packages/sling/distribution");

        assertEmptyFolder(author, authorClient, "/var/sling/distribution/packages/default/shared");
        assertEmptyFolder(author, authorClient, "/var/sling/distribution/packages/default/data");
        assertEmptyFolder(publish, publishClient, "/etc/packages/sling/distribution");
    }

    public void registerPublish(String publishAgent, String remoteImporter) throws Exception {
        String remoteImporterUrl = publish.getServerBaseUrl() + importerUrl(remoteImporter);


        assertExists(authorClient, authorAgentConfigUrl(publishAgent));

        authorClient.setProperties(authorAgentConfigUrl(publishAgent),
                "packageImporter.endpoints", remoteImporterUrl);


        Thread.sleep(1000);

        assertExists(authorClient, agentUrl(publishAgent));
        assertExists(publishClient, importerUrl(remoteImporter));
    }

    public void registerReverse(String reverseAgent, String remoteExporter) throws Exception {
        String remoteExporterUrl = publish.getServerBaseUrl() + exporterUrl(remoteExporter);

        assertExists(authorClient, authorAgentConfigUrl(reverseAgent));

        authorClient.setProperties(authorAgentConfigUrl(reverseAgent), "packageExporter.endpoints", remoteExporterUrl);

        Thread.sleep(1000);
        assertExists(authorClient, agentUrl(reverseAgent));
        assertExists(publishClient, exporterUrl(remoteExporter));
    }


}
