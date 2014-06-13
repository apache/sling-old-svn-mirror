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

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.sling.SlingClient;
import org.apache.sling.testing.tools.sling.SlingInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Utils class for Replication ITs
 */
public class ReplicationUtils {

    private static final String JSON_SELECTOR = ".json";
    private static final String REPLICATION_ROOT_PATH = "/libs/sling/replication";

    private static void assertPostResourceWithParameters(SlingInstance slingInstance,
                                                           int status, String path, String... parameters) throws IOException {
        Request request = slingInstance.getRequestBuilder().buildPostRequest(path);

        if (parameters != null) {
            assertEquals(0, parameters.length % 2);
            List<NameValuePair> valuePairList = new ArrayList<NameValuePair>();

            for (int i = 0; i < parameters.length; i += 2) {
                valuePairList.add(new BasicNameValuePair(parameters[i], parameters[i + 1]));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairList);
            request.withEntity(entity);
        }
        slingInstance.getRequestExecutor().execute(
                request.withCredentials(slingInstance.getServerUsername(), slingInstance.getServerPassword())
        ).assertStatus(status);
    }

    private static void assertPostResourceWithHeaders(SlingInstance slingInstance,
                                                        int status, String path, String... headers) throws IOException {
        Request request = slingInstance.getRequestBuilder().buildPostRequest(path);
        if (headers != null) {
            assertEquals(0, headers.length % 2);
            for (int i = 0; i < headers.length; i += 2) {
                request = request.withHeader(headers[i], headers[i + 1]);
            }
        }
        slingInstance.getRequestExecutor().execute(
                request.withCredentials(slingInstance.getServerUsername(), slingInstance.getServerPassword())
        ).assertStatus(status);
    }

    public static void assertResponseContains(SlingInstance slingInstance,
                                                     String resource, String... parameters) throws IOException {
        if (!resource.endsWith(JSON_SELECTOR)) {
            resource += JSON_SELECTOR;
        }
        String content = slingInstance.getRequestExecutor().execute(
                slingInstance.getRequestBuilder().buildGetRequest(resource)
                        .withCredentials(slingInstance.getServerUsername(), slingInstance.getServerPassword())
        ).getContent().replaceAll("\n", "").trim();


        for (int i = 0; i < parameters.length; i++) {
            assertTrue(parameters[i] + " is not contained in " + content,
                    content.contains(parameters[i])
            );
        }
    }


    public static void replicate(SlingInstance slingInstance, String agent, ReplicationActionType action, String... paths) throws IOException {
        String agentResource = agentUrl("publish");


        List<String> args = new ArrayList<String>();
        args.add(ReplicationHeader.ACTION.toString());
        args.add(action.toString());

        if (paths != null) {
            for (String path: paths) {
                args.add(ReplicationHeader.PATH.toString());
                args.add(path);
            }
        }

        assertPostResourceWithHeaders(slingInstance, 202, agentResource, args.toArray(new String[args.size()]));
    }

    public static void deleteNode(SlingInstance slingInstance, String path) throws IOException {
        assertPostResourceWithParameters(slingInstance, 200, path, ":operation", "delete");

    }

    public static void assertExists(SlingClient slingClient, String path) throws Exception {
        int retries = 100;
        while(!slingClient.exists(path) && retries-- > 0) {
            Thread.sleep(1000);
        }

        assertTrue(retries > 0);
    }

    public static void assertNotExits(SlingClient slingClient, String path) throws Exception {
        int retries = 100;
        while(slingClient.exists(path) && retries-- > 0) {
            Thread.sleep(1000);
        }

        assertTrue(retries > 0);
    }

    public static String createRandomNode(SlingClient slingClient, String parentPath) throws Exception {
        String nodePath = parentPath + "/" + UUID.randomUUID();
        slingClient.createNode(nodePath, "propName", "propValue");
        return nodePath;
    }


    public static String agentRootUrl() {
        return REPLICATION_ROOT_PATH + "/agent";
    }

    public static String agentUrl(String agentName) {
        return REPLICATION_ROOT_PATH + "/agent/" + agentName;
    }

    public static String queueUrl(String agentName) {
        return REPLICATION_ROOT_PATH + "/agent/" + agentName +"/queue";
    }

    public static String agentConfigUrl(String agentName) {
        return REPLICATION_ROOT_PATH + "/config/agent/" + agentName;
    }


    public static String importerRootUrl() {
        return REPLICATION_ROOT_PATH + "/importer";
    }

    public static String importerUrl(String importerName) {
        return REPLICATION_ROOT_PATH + "/importer/" + importerName;
    }
}
