/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.felix.utils.json.JSONParser;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SmokeIT {

    private static final int LAUNCHPAD_PORT = Integer.getInteger("launchpad.http.port", 8080);
    private static final int EXPECTED_BUNDLES_COUNT = Integer.getInteger("IT.expected.bundles.count", Integer.MAX_VALUE);

    @ClassRule
    public static LaunchpadReadyRule LAUNCHPAD = new LaunchpadReadyRule(LAUNCHPAD_PORT);

    private CloseableHttpClient newClient() {

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "admin");
        credsProvider.setCredentials(new AuthScope("localhost", LAUNCHPAD_PORT), creds);

        return HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build();
    }

    @Test
    public void verifyAllBundlesStarted() throws Exception {

        try ( CloseableHttpClient client = newClient() ) {

            HttpGet get = new HttpGet("http://localhost:" + LAUNCHPAD_PORT + "/system/console/bundles.json");

            try ( CloseableHttpResponse response = client.execute(get) ) {

                if ( response.getStatusLine().getStatusCode() != 200 ) {
                    fail("Unexpected status line " + response.getStatusLine());
                }

                Header contentType = response.getFirstHeader("Content-Type");
                assertThat("Content-Type header", contentType.getValue(), CoreMatchers.startsWith("application/json"));

                Map<String, Object> obj = new JSONParser(response.getEntity().getContent()).getParsed();

                @SuppressWarnings("unchecked")
                List<Object> status = (List<Object>) obj.get("s");

                @SuppressWarnings("unchecked")
                List<Object> bundles = (List<Object>) obj.get("data");
                if(bundles.size() < EXPECTED_BUNDLES_COUNT) {
                    fail("Expected at least " + EXPECTED_BUNDLES_COUNT + " bundles, got " + bundles.size());
                }

                BundleStatus bs = new BundleStatus(status);

                if ( bs.resolvedBundles != 0 || bs.installedBundles != 0 ) {

                    StringBuilder out = new StringBuilder();
                    out.append("Expected all bundles to be active, but instead got ")
                        .append(bs.resolvedBundles).append(" resolved bundles, ")
                        .append(bs.installedBundles).append(" installed bundlles: ");

                    for ( int i = 0 ; i < bundles.size(); i++ ) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> bundle = (Map<String, Object>) bundles.get(i);

                        String bundleState = (String) bundle.get("state");
                        String bundleSymbolicName = (String) bundle.get("symbolicName");
                        String bundleVersion = (String) bundle.get("version");

                        switch ( bundleState ) {
                            case "Active":
                            case "Fragment":
                                continue;

                            default:
                                out.append("\n- ").append(bundleSymbolicName).append(" ").append(bundleVersion).append(" is in state " ).append(bundleState);
                        }
                    }

                    fail(out.toString());
                }
            }
        }
    }

    @Test
    public void ensureRepositoryIsStarted() throws Exception {
        try ( CloseableHttpClient client = newClient() ) {

            HttpGet get = new HttpGet("http://localhost:" + LAUNCHPAD_PORT + "/server/default/jcr:root");

            try ( CloseableHttpResponse response = client.execute(get) ) {

                if ( response.getStatusLine().getStatusCode() != 200 ) {
                    fail("Unexpected status line " + response.getStatusLine());
                }

                Header contentType = response.getFirstHeader("Content-Type");
                assertThat("Content-Type header", contentType.getValue(), equalTo("text/xml"));

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document document = db.parse(response.getEntity().getContent());

                Element docElement = document.getDocumentElement();
                NamedNodeMap attrs = docElement.getAttributes();

                Node nameAttr = attrs.getNamedItemNS("http://www.jcp.org/jcr/sv/1.0", "name");
                assertThat("no 'name' attribute found", nameAttr, notNullValue());
                assertThat("Invalid name attribute value", nameAttr.getNodeValue(), equalTo("jcr:root"));
            }
        }
    }

    static class BundleStatus {

        long totalBundles;
        long activeBundles;
        long activeFragments;
        long resolvedBundles;
        long installedBundles;

        public BundleStatus(List<Object> array) {

            totalBundles = (Long)array.get(0);
            activeBundles = (Long)array.get(1);
            activeFragments = (Long)array.get(2);
            resolvedBundles = (Long)array.get(3);
            installedBundles = (Long)array.get(4);

        }
    }
}
