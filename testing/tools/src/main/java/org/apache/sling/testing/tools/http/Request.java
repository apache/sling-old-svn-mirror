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
package org.apache.sling.testing.tools.http;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

/** Request class with convenience with... methods to 
 *  add headers, parameters etc.
 */
public class Request {
    private final HttpUriRequest request;
    private String username;
    private String password;
    private boolean redirects = true;
    private RequestCustomizer customizer;
    
    Request(HttpUriRequest r) {
        request = r;
    }
    
    public HttpUriRequest getRequest() {
        return request;
    }
    
    public String toString() {
        return getClass().getSimpleName() + ": " + request.getURI();
    }
    
    public Request withHeader(String name, String value) {
        request.addHeader(name, value);
        return this;
    }
    
    public Request withCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public Request withRedirects(boolean followRedirectsAutomatically) {
        redirects = followRedirectsAutomatically;
        return this;
    }
    
    private HttpEntityEnclosingRequestBase getHttpEntityEnclosingRequestBase() {
        if(request instanceof HttpEntityEnclosingRequestBase) {
            return (HttpEntityEnclosingRequestBase)request;
        } else {
            throw new IllegalStateException(
                    "Request is not an HttpEntityEnclosingRequestBase: "  
                + request.getClass().getName());
        }
    }

    public Request withContent(String content) throws UnsupportedEncodingException {
        return withEntity(new StringEntity(content, "UTF-8"));
    }
    
    public Request withEntity(HttpEntity e) throws UnsupportedEncodingException {
        getHttpEntityEnclosingRequestBase().setEntity(e);
        return this;
    }
    
    public Request withCustomizer(RequestCustomizer c) {
        customizer = c;
        return this;
    }
    
    // Execute our {@link RequestCustomizer} if we have one */
    void customizeIfNeeded() {
        if(customizer != null) {
            customizer.customizeRequest(this);
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public boolean getRedirects() {
        return redirects;
    }
    
    public RequestCustomizer getCustomizer() {
        return customizer;
    }
}
