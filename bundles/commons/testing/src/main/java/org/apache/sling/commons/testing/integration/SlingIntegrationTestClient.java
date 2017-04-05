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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
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
    
    /** Extension to use to check if a folder exists */
    private String folderExistsTestExtension = ".txt";

    public SlingIntegrationTestClient(HttpClient client) {
        this.httpClient = client;
    }
    
    public String getFolderExistsTestExtension() {
        return folderExistsTestExtension;
    }

    public void setFolderExistsTestExtension(String folderExistsTestExtension) {
        this.folderExistsTestExtension = folderExistsTestExtension;
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
        status = httpClient.executeMethod(new GetMethod(url + folderExistsTestExtension));
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
        final int status = httpClient.executeMethod(new GetMethod(url + folderExistsTestExtension));
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
        return createNode(url, new NameValuePairList(clientNodeProperties), requestHeaders, multiPart);
    }

    /** Create a node under given path, using a POST to Sling
     *  @param url under which node is created
     *  @param multiPart if true, does a multipart POST
     *  @return the URL that Sling provides to display the node
     */
    public String createNode(String url, NameValuePairList clientNodeProperties, Map<String,String> requestHeaders, boolean multiPart)
    throws IOException {
    	return createNode(url, clientNodeProperties, requestHeaders, multiPart, null, null, null);
    }
    
    /** Create a node under given path, using a POST to Sling
     *  @param url under which node is created
     *  @param multiPart if true, does a multipart POST
     *  @param localFile file to upload
     *  @param fieldName name of the file field
     *  @param typeHint typeHint of the file field 
     *  @return the URL that Sling provides to display the node
     */
    public String createNode(String url, NameValuePairList clientNodeProperties, Map<String,String> requestHeaders, boolean multiPart, 
    		File localFile, String fieldName, String typeHint) 
    throws IOException {
    	
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);

        // create a private copy of the properties to not tamper with
        // the properties of the client
        NameValuePairList nodeProperties = new NameValuePairList(clientNodeProperties);

        // add sling specific properties
        nodeProperties.prependIfNew(":redirect", "*");
        nodeProperties.prependIfNew(":displayExtension", "");
        nodeProperties.prependIfNew(":status", "browser");

        // add fake property - otherwise the node is not created
        if (clientNodeProperties == null) {
            nodeProperties.add("jcr:created", "");
        }

        // force form encoding to UTF-8, which is what we use to convert the
        // string parts into stream data
        nodeProperties.addOrReplace("_charset_", "UTF-8");

        if( nodeProperties.size() > 0) {
            if(multiPart) {
                final List<Part> partList = new ArrayList<Part>();
                for(NameValuePair e : nodeProperties) {
                    if (e.getValue() != null) {
                        partList.add(new StringPart(e.getName(), e.getValue(), "UTF-8"));
                    }
                }
                if  (localFile != null) {
                    partList.add(new FilePart(fieldName, localFile));
                    if (typeHint != null) {
                    	partList.add(new StringPart(fieldName + "@TypeHint", typeHint));
                    }
                }
                final Part [] parts = partList.toArray(new Part[partList.size()]);
                post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
            } else {
            	post.getParams().setContentCharset("UTF-8");
                for(NameValuePair e : nodeProperties) {
                    post.addParameter(e.getName(),e.getValue());
                }
            }
        }

        if(requestHeaders != null) {
            for(Map.Entry<String,String> e : requestHeaders.entrySet()) {
                post.addRequestHeader(e.getKey(), e.getValue());
            }
        }

        final int expected = 302;
        final int status = httpClient.executeMethod(post);
        if(status!=expected) {
            throw new HttpStatusCodeException(expected, status, "POST", url, HttpTestBase.getResponseBodyAsStream(post, 0));
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
        final int expected = 200;
        if(status!=expected) {
            throw new HttpStatusCodeException(expected, status, "POST", HttpTestBase.getResponseBodyAsStream(post, 0));
        }
    }

    /** Upload multiple files to file node structures */
    public void uploadToFileNodes(String url, File[] localFiles, String[] fieldNames, String[] typeHints)
        throws IOException {

    	List<Part> partsList = new ArrayList<Part>();
    	for (int i=0; i < localFiles.length; i++) {
            Part filePart = new FilePart(fieldNames[i], localFiles[i]);
            partsList.add(filePart);
            if (typeHints != null) {
            	Part typeHintPart = new StringPart(fieldNames[i] + "@TypeHint", typeHints[i]);
            	partsList.add(typeHintPart);
            }
		}

        final Part[] parts = partsList.toArray(new Part[partsList.size()]);
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));

        final int expected = 200;
        final int status = httpClient.executeMethod(post);
        if(status!=expected) {
            throw new HttpStatusCodeException(expected, status, "POST", HttpTestBase.getResponseBodyAsStream(post, 0));
        }
    }
    
    public int post(String url, Map<String,String> properties) throws HttpException, IOException {
        final PostMethod post = new PostMethod(url);
        post.getParams().setContentCharset("UTF-8");
        for(Entry<String, String> e : properties.entrySet()) {
            post.addParameter(e.getKey(), e.getValue());
        }
        return httpClient.executeMethod(post);
    }

    public int get(String url) throws HttpException, IOException {
        final GetMethod get = new GetMethod(url);
        get.getParams().setContentCharset("UTF-8");
        return httpClient.executeMethod(get);
    }

}
