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
package org.apache.sling.commons.testing.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

/** Client functions to interact with Sling in integration tests */
public class SlingIntegrationTestClient {
    private final HttpClient httpClient;

    public SlingIntegrationTestClient(HttpClient client) {
        this.httpClient = client;
    }

    /** Upload a file to the Sling repository
     *  @return the HTTP status code
     */
    public int upload(String toUrl, InputStream is) throws IOException {
        final PutMethod put = new PutMethod(toUrl);
        put.setRequestEntity(new InputStreamRequestEntity(is));
        return httpClient.executeMethod(put);
    }

    /** Delete a file from the Sling repository
     *  @return the HTTP status code
     */
    public int delete(String url) throws IOException {
        final DeleteMethod delete = new DeleteMethod(url);
        return httpClient.executeMethod(delete);
    }

    /** Create the given directory via WebDAV, if needed, under given URL */
    public void mkdir(String url) throws IOException {
        int status = 0;
        status = httpClient.executeMethod(new GetMethod(url));
        if(status != 200) {
            status = httpClient.executeMethod(new HttpAnyMethod("MKCOL",url));
            if(status!=201) {
                throw new IOException("mkdir(" + url + ") failed, status code=" + status);
            }
        }
    }

    /** Create the given directory via WebDAV, including parent directories */
    public void mkdirs(String baseUrl,String path) throws IOException {
        final String [] paths = path.split("/");
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0,baseUrl.length() - 1);
        }

        String currentPath = baseUrl;
        for(String pathElement : paths) {
            if(pathElement.length() == 0) {
                continue;
            }
            currentPath += "/" + pathElement;
            mkdir(currentPath);
        }

        final String url = baseUrl + path;
        final int status = httpClient.executeMethod(new GetMethod(url));
        if(status!=200) {
            throw new HttpStatusCodeException(200, status, "GET", url);
        }
    }

    /** Call the other createNode method with headers==null */
    public String createNode(String url, Map<String,String> nodeProperties) throws IOException {
        return createNode(url, nodeProperties, null, false);
    }

    /** Create a node under given path, using a POST to Sling
     *  @param url under which node is created
     *  @param multiPart if true, does a multipart POST
     *  @return the URL that Sling provides to display the node
     */
    public String createNode(String url, Map<String,String> clientNodeProperties, Map<String,String> requestHeaders,boolean multiPart)
    throws IOException {
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);

        // create a private copy of the properties to not tamper with
        // the properties of the client
        Map<String, String> nodeProperties = new HashMap<String, String>();

        // add sling specific properties
        nodeProperties.put(":redirect", url);
        nodeProperties.put(":displayExtension", "");
        nodeProperties.put(":status", "browser");

        // take over any client provided properties
        if (clientNodeProperties != null) {
            nodeProperties.putAll(clientNodeProperties);
        } else {
            // add fake property - otherwise the node is not created
            nodeProperties.put("jcr:created", "");
        }

        // force form encoding to UTF-8, which is what we use to convert the
        // string parts into stream data
        nodeProperties.put("_charset_", "UTF-8");

        if( nodeProperties.size() > 0) {
            if(multiPart) {
                final List<Part> partList = new ArrayList<Part>();
                for(Map.Entry<String,String> e : nodeProperties.entrySet()) {
                    if (e.getValue() != null) {
                        partList.add(new StringPart(e.getKey().toString(), e.getValue().toString(), "UTF-8"));
                    }
                }
                final Part [] parts = partList.toArray(new Part[partList.size()]);
                post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
            } else {
                for(Map.Entry<String,String> e : nodeProperties.entrySet()) {
                    post.addParameter(e.getKey(),e.getValue());
                }
            }
        }

        if(requestHeaders != null) {
            for(Map.Entry<String,String> e : requestHeaders.entrySet()) {
                post.addRequestHeader(e.getKey(), e.getValue());
            }
        }

        final int status = httpClient.executeMethod(post);
        if(status!=302) {
            throw new HttpStatusCodeException(302, status, "POST", url);
        }
        String location = post.getResponseHeader("Location").getValue();
        post.releaseConnection();
        // simple check if host is missing
        if (!location.startsWith("http://")) {
            String host = HttpTestBase.HTTP_BASE_URL;
            int idx = host.indexOf('/', 8);
            if (idx > 0) {
                host = host.substring(0, idx);
            }
            location = host + location;
        }
        return location;
    }

    /** Upload to an file node structure, see SLING-168 */
    public void uploadToFileNode(String url, File localFile, String fieldName, String typeHint)
        throws IOException {

        final Part[] parts = new Part[typeHint == null ? 1 : 2];
        parts[0] = new FilePart(fieldName, localFile);
        if (typeHint != null) {
            parts[1] = new StringPart(fieldName + "@TypeHint", typeHint);
        }
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));

        final int status = httpClient.executeMethod(post);
        if(status!=200) { // fmeschbe: The default sling status is 200, not 302
            throw new HttpStatusCodeException(200, status, "POST", url);
        }
    }
}
