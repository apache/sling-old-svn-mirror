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
package org.apache.sling.tooling.lc.jira;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;

public class IssueFinder {
    
    public static void main(String[] args) throws IOException {
        
        new IssueFinder().findIssues(Arrays.asList("SLING-1", "SLING-2"))
            .stream()
            .forEach(System.out::println);
    }
    
    public List<Issue> findIssues(List<String> issueKeys) throws IOException{
        
        HttpClient client = new DefaultHttpClient();
        
        HttpGet get;
        try {
            URIBuilder builder = new URIBuilder("https://issues.apache.org/jira/rest/api/2/search")
                    .addParameter("jql", "key in (" + String.join(",", issueKeys) + ")")
                    .addParameter("fields", "key,summary");
            
            get = new HttpGet(builder.build());
        } catch (URISyntaxException e) {
            // never happens
            throw new RuntimeException(e);
        }
        
        HttpResponse response = client.execute(get);
        try {
            if ( response.getStatusLine().getStatusCode() != 200 ) { 
                throw new IOException("Search call returned status " + response.getStatusLine().getStatusCode());
            }
            
            try ( Reader reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8") ) {
                Response apiResponse = new Gson().fromJson(reader, Response.class);
                List<Issue> issues = apiResponse.getIssues();
                Collections.sort(issues);
                return issues;
                
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
        
    }
}
