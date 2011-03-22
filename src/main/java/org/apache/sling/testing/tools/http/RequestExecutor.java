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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/** Executes a Request and provides convenience methods
 *  to validate the results.
 */
public class RequestExecutor {
    private final DefaultHttpClient httpClient;
    private HttpUriRequest request; 
    private HttpResponse response;
    private HttpEntity entity;
    private String content;
    
    /**
     * HttpRequestInterceptor for preemptive authentication, based on httpclient
     * 4.0 example
     */
    private static class PreemptiveAuthInterceptor implements
            HttpRequestInterceptor {

        public void process(HttpRequest request, HttpContext context)
                throws HttpException, IOException {

            AuthState authState = 
                (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
            CredentialsProvider credsProvider = 
                (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
            HttpHost targetHost = 
                (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

            // If not auth scheme has been initialized yet
            if (authState.getAuthScheme() == null) {
                AuthScope authScope = 
                    new AuthScope(targetHost.getHostName(), targetHost.getPort());

                // Obtain credentials matching the target host
                Credentials creds = credsProvider.getCredentials(authScope);

                // If found, generate BasicScheme preemptively
                if(creds != null) {
                    authState.setAuthScheme(new BasicScheme());
                    authState.setCredentials(creds);
                }
            }
        }
    }
    
    public RequestExecutor(DefaultHttpClient client) {
        httpClient = client;
    }
    
    public String toString() {
        if(request == null) {
            return "Request";
        }
        return request.getMethod() + " request to " + request.getURI();
    }
    
    public RequestExecutor execute(Request r) throws ClientProtocolException, IOException {
        clear();
        r.customizeIfNeeded();
        request = r.getRequest();
        
        // Optionally setup for basic authentication
        if(r.getUsername() != null) {
            httpClient.getCredentialsProvider().setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(r.getUsername(), r.getPassword()));

            // And add request interceptor to have preemptive authentication
            httpClient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
        } else {
            // Make sure existing credentials are not reused - but looks like we
            // cannot set null as credentials
            httpClient.getCredentialsProvider().setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(getClass().getName(), getClass().getSimpleName()));
            httpClient.removeRequestInterceptorByClass(PreemptiveAuthInterceptor.class);
        }
        
        // Setup redirects
        httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, r.getRedirects());
        
        // Execute request
        response = httpClient.execute(request);
        entity = response.getEntity();
        if(entity != null) {
            consumeEntity();
        }
        return this;
    }

    /** Can be overridden to consume in a different way, or not at all */ 
    protected void consumeEntity() throws ParseException, IOException {
        content = EntityUtils.toString(entity);
        entity.consumeContent();
    }
    
    protected void clear() {
        request = null;
        entity = null;
        response = null;
        content = null;
    }

    /** Verify that response matches supplied status */
    public RequestExecutor assertStatus(int expected) {
        assertNotNull(this.toString(), response);
        assertEquals(this + ": expecting status " + expected, expected, response.getStatusLine().getStatusCode());
        return this;
    }
    
    /** Verify that response matches supplied content type */
    public RequestExecutor assertContentType(String expected) {
        assertNotNull(this.toString(), response);
        if(entity == null) {
            fail(this + ": no entity in response, cannot check content type");
        }
        
        // Remove whatever follows semicolon in content-type
        String contentType = entity.getContentType().getValue();
        if(contentType != null) {
            contentType = contentType.split(";")[0].trim();
        }
        
        // And check for match
        assertEquals(this + ": expecting content type " + expected, expected, contentType);
        return this;
    }

    /** For each supplied regexp, fail unless content contains at 
     *  least one line that matches.
     *  Regexps are automatically prefixed/suffixed with .* so as
     *  to have match partial lines.
     */
    public RequestExecutor assertContentRegexp(String... regexp) throws IOException {
        assertNotNull(this.toString(), response);
        nextPattern:
        for(String expr : regexp) {
            final Pattern p = Pattern.compile(".*" + expr + ".*");
            final BufferedReader br = new BufferedReader(new StringReader(content));
            String line = null;
            while( (line = br.readLine()) != null) {
                if(p.matcher(line).matches()) {
                    continue nextPattern;
                }
            }
            fail(this + ": no match for regexp '" + expr + "', content=\n" + content);
        }
        return this;
    }

    /** For each supplied string, fail unless content contains it */
    public RequestExecutor assertContentContains(String... expected) throws ParseException, IOException {
        assertNotNull(this.toString(), response);
        for(String exp : expected) {
            if(!content.contains(exp)) {
                fail(this + ": content does not contain '" + exp + "', content=\n" + content);
            }
        }
        return this;
    }
    
    public void generateDocumentation(RequestDocumentor documentor, String...metadata) throws IOException {
        documentor.generateDocumentation(this, metadata);
    }

    public HttpUriRequest getRequest() {
        return request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public HttpEntity getEntity() {
        return entity;
    }
    
    public String getContent() {
        return content;
    }
}
