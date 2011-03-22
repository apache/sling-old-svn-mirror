/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.junit.remote.httpclient;

import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.testing.tools.http.Request;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestCustomizer;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP client that executes tests remotely */ 
public class RemoteTestHttpClient {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final String junitServletUrl;
    private StringBuilder subpath;
    private boolean consumeContent;
    private RequestCustomizer requestCustomizer;
    private static final String SLASH = "/";
    private static final String DOT = ".";
    
    public RemoteTestHttpClient(String junitServletUrl, boolean consumeContent) {
        if(junitServletUrl == null) {
            throw new IllegalArgumentException("JUnit servlet URL is null, cannot run tests");
        }
        this.junitServletUrl = junitServletUrl;
        this.consumeContent = consumeContent;
    }
    
    public void setRequestCustomizer(RequestCustomizer c) {
        requestCustomizer = c;
    }
    
    public RequestExecutor runTests(String testClassesSelector, String testMethodSelector, String extension) 
    throws ClientProtocolException, IOException {
        final RequestBuilder builder = new RequestBuilder(junitServletUrl);

        // Optionally let the client to consume the response entity
        final RequestExecutor executor = new RequestExecutor(new DefaultHttpClient()) {
            @Override
            protected void consumeEntity() throws ParseException, IOException {
                if(consumeContent) {
                    super.consumeEntity();
                }
            }
        };
        
        // Build path for POST request to execute the tests
        
        // Test classes selector
        subpath = new StringBuilder();
        if(!junitServletUrl.endsWith(SLASH)) {
            subpath.append(SLASH);
        }
        subpath.append(testClassesSelector);
        
        // Test method selector
        if(testMethodSelector != null && testMethodSelector.length() > 0) {
            subpath.append("/");
            subpath.append(testMethodSelector);
        }
        
        // Extension
        if(!extension.startsWith(DOT)) {
            subpath.append(DOT);
        }
        subpath.append(extension);
        
        log.info("Executing test remotely, path={} JUnit servlet URL={}", 
                subpath, junitServletUrl);
        final Request r = builder
        .buildPostRequest(subpath.toString())
        .withCustomizer(requestCustomizer);
        executor.execute(r).assertStatus(200);

        return executor;
    }
    
    /** If called after runTests, returns the path used to
     *  run tests on the remote JUnit servlet
     */
    public String getTestExecutionPath() {
        return subpath == null ? null : subpath.toString();
    }
}
