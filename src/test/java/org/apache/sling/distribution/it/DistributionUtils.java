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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.distribution.communication.DistributionParameter;
import org.apache.sling.distribution.communication.DistributionRequestType;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.sling.SlingClient;
import org.apache.sling.testing.tools.sling.SlingInstance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Utils class for distribution ITs
 */
public class DistributionUtils {

    private static final String JSON_SELECTOR = ".json";
    private static final String DISTRIBUTION_ROOT_PATH = "/libs/sling/distribution";

    public static String assertPostResourceWithParameters(SlingInstance slingInstance,
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

        return slingInstance.getRequestExecutor().execute(
                request.withCredentials(slingInstance.getServerUsername(), slingInstance.getServerPassword())
        ).assertStatus(status).getContent();
    }

    private static String assertPostResource(SlingInstance slingInstance,
                                             int status, String path, byte[] bytes) throws IOException {
        Request request = slingInstance.getRequestBuilder().buildPostRequest(path);

        if (bytes != null) {

            ByteArrayEntity entity = new ByteArrayEntity(bytes);
            request.withEntity(entity);
        }

        return slingInstance.getRequestExecutor().execute(
                request.withCredentials(slingInstance.getServerUsername(), slingInstance.getServerPassword())
        ).assertStatus(status).getContent();
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


        for (String parameter : parameters) {
            assertTrue(parameter + " is not contained in " + content,
                    content.contains(parameter)
            );
        }
    }


    public static void distribute(SlingInstance slingInstance, String agentName, DistributionRequestType action, String... paths) throws IOException {
        String agentResource = agentUrl(agentName);

        executeDistributionRequest(slingInstance, 202, agentResource, action, paths);
    }

    public static String executeDistributionRequest(SlingInstance slingInstance, int status, String resource, DistributionRequestType action, String... paths) throws IOException {

        List<String> args = new ArrayList<String>();
        args.add(DistributionParameter.ACTION.toString());
        args.add(action.toString());

        if (paths != null) {
            for (String path : paths) {
                args.add(DistributionParameter.PATH.toString());
                args.add(path);
            }
        }

        return assertPostResourceWithParameters(slingInstance, status, resource, args.toArray(new String[args.size()]));
    }

    public static String doExport(SlingInstance slingInstance, String exporterName, DistributionRequestType action, String... paths) throws IOException {
        String agentResource = exporterUrl(exporterName);

        return executeDistributionRequest(slingInstance, 200, agentResource, action, paths);
    }

    public static String doImport(SlingInstance slingInstance, String importerName, byte[] bytes) throws IOException {
        String agentResource = importerUrl(importerName);

        return assertPostResource(slingInstance, 200, agentResource, bytes);
    }

    public static void deleteNode(SlingInstance slingInstance, String path) throws IOException {
        assertPostResourceWithParameters(slingInstance, 200, path, ":operation", "delete");
    }

    public static void assertExists(SlingClient slingClient, String path) throws Exception {
        int retries = 100;
        while (!slingClient.exists(path) && retries-- > 0) {
            Thread.sleep(1000);
        }
        assertTrue("path " + path + " doesn't exist", slingClient.exists(path));
    }

    public static void assertNotExists(SlingClient slingClient, String path) throws Exception {
        int retries = 100;
        while (slingClient.exists(path) && retries-- > 0) {
            Thread.sleep(1000);
        }
        assertFalse("path " + path + " still exists", slingClient.exists(path));
    }

    public static String createRandomNode(SlingClient slingClient, String parentPath) throws Exception {
        String nodePath = parentPath + "/" + UUID.randomUUID();
        if (!slingClient.exists(parentPath)) {
            slingClient.mkdirs(parentPath);
        }
        slingClient.createNode(nodePath, "jcr:primaryType", "nt:unstructured", "propName", "propValue");
        return nodePath;
    }

    public static String agentRootUrl() {
        return DISTRIBUTION_ROOT_PATH + "/services/agents";
    }

    public static String agentUrl(String agentName) {
        return agentRootUrl() + "/" + agentName;
    }

    public static String queueUrl(String agentName) {
        return agentUrl(agentName) + "/queue";
    }

    public static String authorAgentConfigUrl(String agentName) {
        return DISTRIBUTION_ROOT_PATH + "/settings.author/agents/" + agentName;
    }

    public static String publishAgentConfigUrl(String agentName) {
        return DISTRIBUTION_ROOT_PATH + "/settings.publish/agents/" + agentName;
    }


    public static String importerRootUrl() {
        return DISTRIBUTION_ROOT_PATH + "/services/importers";
    }

    public static String importerUrl(String importerName) {
        return importerRootUrl() + "/" + importerName;
    }

    public static String exporterRootUrl() {
        return DISTRIBUTION_ROOT_PATH + "/services/exporters";
    }

    public static String exporterUrl(String exporterName) {
        return exporterRootUrl() + "/" + exporterName;
    }

    public static String importerConfigUrl(String importerName) {
        return DISTRIBUTION_ROOT_PATH + "/settings/importers/" + importerName;
    }

    public static String exporterConfigUrl(String exporterName) {
        return DISTRIBUTION_ROOT_PATH + "/settings/exporters/" + exporterName;
    }


    public static String triggerRootUrl() {
        return DISTRIBUTION_ROOT_PATH + "/services/triggers";
    }

    public static String triggerUrl(String triggerName) {
        return triggerRootUrl() + "/" + triggerName;
    }

    public static String triggerEventUrl(String triggerName) {
        return triggerRootUrl() + "/" + triggerName + ".event";
    }

}
