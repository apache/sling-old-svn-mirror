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
package org.apache.sling.testing.tools.sling;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;

/** Simple Sling client, created for integration
 *  tests but should be general purpose */
public class SlingClient {
    public static final String LOCATION_HEADER = "Location";
    public static final String HTTP_PREFIX = "http://";
    private final RequestExecutor executor;
    private final RequestBuilder builder;
    private final String slingServerUrl;
    private final String username;
    private final String password;
    
    static class HttpAnyMethod extends HttpRequestBase {
        private final URI uri;
        private final String method;
        
        HttpAnyMethod(String method, String uriString) {
            this.uri = URI.create(uriString);
            this.method = method;
        }

        @Override
        public String getMethod() {
            return method;
        }
        
        @Override
        public URI getURI() {
            return uri;
        }
    };
    
    public SlingClient(String slingServerUrl, String username, String password) {
        this.slingServerUrl = slingServerUrl;
        this.username = username;
        this.password = password;
        builder = new RequestBuilder(slingServerUrl);
        executor = new RequestExecutor(new DefaultHttpClient());
    }
    
    /** Create a node at specified path, with optional properties
     *  specified as a list of String arguments, odd values are keys
     *  and even arguments are values.
     */
    public String createNode(String path, String...properties) throws IOException {
        Map<String, Object> props = extractMap(properties);

        return createNode(path, props);
    }
    
    /** Create a node at specified path, with optional properties
     * @param path Used in POST request to Sling server
     * @param properties If not null, properties are added to the created node
     * @return The actual path of the node that was created
     */
    public String createNode(String path, Map<String, Object> properties) throws UnsupportedEncodingException, IOException {
        String actualPath = null;
        
        final MultipartEntity entity = new MultipartEntity();
        
        // Add Sling POST options 
        entity.addPart(":redirect",new StringBody("*"));
        entity.addPart(":displayExtension",new StringBody(""));
        
        // Add user properties
        if(properties != null) {
            for(Map.Entry<String, Object> e : properties.entrySet()) {
                entity.addPart(e.getKey(), new StringBody(e.getValue().toString()));
            }
        }
        
        final HttpResponse response = 
            executor.execute(
                builder.buildPostRequest(path)
                .withEntity(entity)
                .withCredentials(username, password)
            )
            .assertStatus(302)
            .getResponse();
        
        final Header location = response.getFirstHeader(LOCATION_HEADER);
        assertNotNull("Expecting " + LOCATION_HEADER + " in response", location);
        actualPath = locationToPath(location.getValue());
        return actualPath;
    }
    
    /** Convert a Location value to the corresponding node path */
    String locationToPath(String locationHeaderValue) {
        if(locationHeaderValue.startsWith(slingServerUrl)) {
            return locationHeaderValue.substring(slingServerUrl.length());
        } else if(locationHeaderValue.startsWith(HTTP_PREFIX)){
            throw new IllegalArgumentException(
                    "Unexpected Location header value [" + locationHeaderValue 
                    + "], should start with [" + slingServerUrl + "] if starting with " 
                    + HTTP_PREFIX);
        } else {
            return locationHeaderValue;
        }
    }

    private Map<String, Object> extractMap(String[] properties) {
        Map<String, Object> props = null;
        if(properties != null && properties.length > 0) {
            props = new HashMap<String, Object>();
            if(properties.length % 2 != 0) {
                throw new IllegalArgumentException("Odd number of properties is invalid:" + properties.length);
            }
            for(int i=0 ; i<properties.length; i+=2) {
                props.put(properties[i], properties[i+1]);
            }
        }

        return props;
    }

    /** Updates a node at specified path, with optional properties
     *  specified as a list of String arguments, odd values are keys
     *  and even arguments are values.
     */
    public void setProperties(String path, String... properties) throws IOException {
        Map<String, Object> props = extractMap(properties);
        setProperties(path, props);
    }

    /** Updates a node at specified path, with optional properties
    */
     public void setProperties(String path, Map<String, Object> properties) throws IOException {
        final MultipartEntity entity = new MultipartEntity();
        // Add user properties
        if(properties != null) {
            for(Map.Entry<String, Object> e : properties.entrySet()) {
                entity.addPart(e.getKey(), new StringBody(e.getValue().toString()));
            }
        }

        final HttpResponse response =
                executor.execute(
                        builder.buildPostRequest(path)
                                .withEntity(entity)
                                .withCredentials(username, password)
                )
                        .assertStatus(200)
                        .getResponse();
    }
    
    /** Delete supplied path */
    public void delete(String path) throws IOException {
        executor.execute(
        builder.buildOtherRequest(
            new HttpDelete(builder.buildUrl(path)))
            .withCredentials(username, password)
        )
        .assertStatus(204);
    }
    
    /** Upload using a PUT request.
     *  @param path the path of the uploaded file
     *  @param data the content
     *  @param length Use -1 if unknown
     *  @param createFolders if true, intermediate folders are created via mkdirs
     */
    public void upload(String path, InputStream data, int length, boolean createFolders) throws IOException {
        final HttpEntity e = new InputStreamEntity(data, length);
        if(createFolders) {
            mkdirs(getParentPath(path));
        }
        executor.execute(
        builder.buildOtherRequest(
            new HttpPut(builder.buildUrl(path))).withEntity(e)
            .withCredentials(username, password)
        )
        .assertStatus(201);
    }
    
    /** Create path and all its parent folders, using MKCOL */
    public void mkdirs(String path) throws IOException {
        // Call mkdir on all parent paths, starting at the topmost one
        final Stack<String> parents = new Stack<String>();
        path = getParentPath(path);
        while(path.length() > 0 && !exists(path)) {
            parents.push(path);
            path = getParentPath(path);
        }
        
        while(!parents.isEmpty()) {
        	mkdir(parents.pop());
        }
    }
    
    /** Create path using MKCOL */
    public void mkdir(String path) throws IOException {
        if(!exists(path)) {
            executor.execute(
                builder.buildOtherRequest(
                    new HttpAnyMethod("MKCOL", builder.buildUrl(path)))
                    .withCredentials(username, password)
            )
            .assertStatus(201); 
        }
    }
    
    public boolean exists(String path) throws IOException {
        final int status = executor.execute(builder.buildGetRequest(path + ".json")
                .withCredentials(username, password))
        .getResponse().getStatusLine().getStatusCode();
        return status == 200;
    }
    
    /** Return parent path: whatever comes before the last / in path, empty
     *  string if no / in path.
     */
    protected String getParentPath(String path) {
        final int pos = path.lastIndexOf('/');
        if(pos > 0) {
            return path.substring(0, pos);
        } else {
            return "";
        }
    }
}
