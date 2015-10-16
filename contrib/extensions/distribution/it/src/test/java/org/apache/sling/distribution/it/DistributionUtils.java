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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import junit.framework.Assert;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.sling.distribution.DistributionRequestType;
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

    public static JSONObject getResource(SlingInstance slingInstance, String path) throws IOException, JSONException {
        if (!path.endsWith(JSON_SELECTOR)) {
            path += JSON_SELECTOR;
        }
        Request request = slingInstance.getRequestBuilder().buildGetRequest(path)
                .withCredentials(slingInstance.getServerUsername(), slingInstance.getServerPassword());


        // Get list of tests in JSON format
        String content = slingInstance.getRequestExecutor().execute(request)
                .assertStatus(200)
                .assertContentType("application/json").getContent();

        // Parse JSON response for more precise testing
        final JSONObject json = new JSONObject(new JSONTokener(content));

        return json;
    }

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

    public static void setArrayProperties(SlingInstance slingInstance, String resource, String property, String... values) throws IOException {
        List<String> parameters = new ArrayList<String>();
        for (String value : values) {
            parameters.add(property);
            parameters.add(value);
        }

        assertPostResourceWithParameters(slingInstance, 200, resource, parameters.toArray(new String[0]));

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

        executeDistributionRequest(slingInstance, 202, agentResource, action, false, paths);
    }

    public static void distributeDeep(SlingInstance slingInstance, String agentName, DistributionRequestType action, String... paths) throws IOException {
        String agentResource = agentUrl(agentName);

        executeDistributionRequest(slingInstance, 202, agentResource, action, true, paths);
    }

    public static String executeDistributionRequest(SlingInstance slingInstance, int status, String resource, DistributionRequestType action, boolean deep, String... paths) throws IOException {

        List<String> args = new ArrayList<String>();
        args.add("action");
        args.add(action.toString());

        if (deep) {
            args.add("deep");
            args.add("true");
        }

        if (paths != null) {
            for (String path : paths) {
                args.add("path");
                args.add(path);
            }
        }

        return assertPostResourceWithParameters(slingInstance, status, resource, args.toArray(new String[args.size()]));
    }

    public static String doExport(SlingInstance slingInstance, String exporterName, DistributionRequestType action, String... paths) throws IOException {
        String exporterUrl = exporterUrl(exporterName);

        return executeDistributionRequest(slingInstance, 200, exporterUrl, action, false, paths);
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
            createNode(slingClient, parentPath);
        }

        slingClient.createNode(nodePath, "jcr:primaryType", "nt:unstructured", "propName", "propValue");
        return nodePath;
    }

    public static void createNode(SlingClient slingClient, String path) throws IOException {

        if (slingClient.exists(path)) {
            return;
        }

        String parentPath = Text.getRelativeParent(path, 1);

        createNode(slingClient, parentPath);

        slingClient.createNode(path, "jcr:primaryType", "nt:unstructured");
    }

    public static String agentRootUrl() {
        return DISTRIBUTION_ROOT_PATH + "/services/agents";
    }

    public static String agentUrl(String agentName) {
        return agentRootUrl() + "/" + agentName;
    }

    public static String queueUrl(String agentName) {
        return agentUrl(agentName) + "/queues";
    }

    public static String logUrl(String agentName) {
        return agentUrl(agentName) + "/log";
    }

    public static String authorAgentConfigUrl(String agentName) {
        return DISTRIBUTION_ROOT_PATH + "/settings/agents/" + agentName;
    }

    public static String publishAgentConfigUrl(String agentName) {
        return DISTRIBUTION_ROOT_PATH + "/settings/agents/" + agentName;
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


    public static void assertEmptyFolder(SlingInstance instance, SlingClient client, String path) throws IOException, JSONException {

        if (client.exists(path)) {
            List<String> children = getChildrenForFolder(instance, path);

            assertEquals(0, children.size());
        }

    }


    public static List<String> getChildrenForFolder(SlingInstance instance, String path) throws IOException, JSONException {
        List<String> result = new ArrayList<String>();
        JSONObject authorJson = getResource(instance, path + ".1.json");
        Iterator<String> it = authorJson.keys();
        while (it.hasNext()) {
            String key = it.next();

            if (!key.contains(":")) {
                result.add(key);
            }
        }
        return result;
    }

    public static Map<String, Map<String, Object>> getQueues(SlingInstance instance, String agentName) throws IOException, JSONException {
        Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();

        JSONObject json = getResource(instance, queueUrl(agentName) + ".infinity");

        JSONArray items = json.getJSONArray("items");

        for(int i=0; i < items.length(); i++) {
            String queueName = items.getString(i);

            Map<String, Object> queueProperties = new HashMap<String, Object>();

            JSONObject queue = json.getJSONObject(queueName);
            queueProperties.put("empty", queue.getBoolean("empty"));
            queueProperties.put("itemsCount", queue.get("itemsCount"));

            result.put(queueName, queueProperties);
        }

        return result;
    }

}
