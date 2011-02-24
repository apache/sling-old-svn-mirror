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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/** Convenience builder for Request objects */
public class RequestBuilder {
    private final String baseUrl;
    
    public RequestBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /** Build a GET request to specified path with optional query
     *  parameters. See {@link #buildUrl(String, String...)} for
     *  queryParameters semantics.
     */
    public Request buildGetRequest(String path, String...queryParameters) {
        return new Request(new HttpGet(buildUrl(path, queryParameters)));
    }
    
    /** Build a POST request to specified path with optional query
     *  parameters. See {@link #buildUrl(String, String...)} for
     *  queryParameters semantics.
     */
    public Request buildPostRequest(String path) {
        return new Request(new HttpPost(buildUrl(path)));
    }
    
    /** Wrap supplied HTTP request */
    public Request buildOtherRequest(HttpRequestBase r) {
        return new Request(r);
    }
    
    /** Build an URL from our base path, supplied path and optional
     *  query parameters.
     *  @param queryParameters an even number of Strings, each pair
     *  of values represents the key and value of a query parameter.
     *  Keys and values are encoded by this method.
     */
    public String buildUrl(String path, String...queryParameters) {
        final StringBuilder sb = new StringBuilder();
        
        if(queryParameters == null || queryParameters.length == 0) {
            sb.append(baseUrl);
            sb.append(path);
            
        } else if(queryParameters.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Invalid number of queryParameters arguments ("
                    + queryParameters.length
                    + "), must be even"
                    );
        } else {
            final List<NameValuePair> p = new ArrayList<NameValuePair>();
            for(int i=0 ; i < queryParameters.length; i+=2) {
                p.add(new BasicNameValuePair(queryParameters[i], queryParameters[i+1]));
            }
            sb.append(baseUrl);
            sb.append(path);
            sb.append("?");
            sb.append(URLEncodedUtils.format(p, "UTF-8"));
        }
        
        return sb.toString();
    }
}
