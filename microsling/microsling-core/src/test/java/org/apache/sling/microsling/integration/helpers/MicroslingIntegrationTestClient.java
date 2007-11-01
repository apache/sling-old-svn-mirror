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
package org.apache.sling.microsling.integration.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;

/** Client functions to interact with microsling in integration tests */ 
public class MicroslingIntegrationTestClient {
    private final HttpClient httpClient;
    
    public MicroslingIntegrationTestClient(HttpClient client) {
        this.httpClient = client;
    }
    
    /** Upload a file to the microsling repository 
     *  @return the HTTP status code
     */
    public int upload(String toUrl, InputStream is) throws IOException {
        final PutMethod put = new PutMethod(toUrl);
        put.setRequestEntity(new InputStreamRequestEntity(is));
        return httpClient.executeMethod(put);
    }
    
    /** Delete a file from the microsling repository 
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
            throw new IOException("Expected status 200, got " + status + " for URL=" + url);
        }
    }
    
    /** Create a node under given path, using a POST to microsling
     *  @param url must end with ".sling" to use the microsling's default
     *  servlet POST behaviour. 
     *  @return the URL that microsling provides to display the node 
     */
    public String createNode(String url, Map<String,String> nodeProperties) throws IOException {
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);
        
        if(nodeProperties!=null) {
            for(Map.Entry<String,String> e : nodeProperties.entrySet()) {
                post.addParameter(e.getKey(),e.getValue());
            }
        }
        
        final int status = httpClient.executeMethod(post);
        if(status!=302) {
            throw new IOException("Expected status code 302 for POST, got " + status + ", URL=" + url);
        }
        final String location = post.getResponseHeader("Location").getValue();
        return location;
    }
}
