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
package org.apache.sling.servlets.post.impl.helper;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.servlets.post.PostResponse;
import org.junit.Test;

public class HtmlResponseProxyTest {
    
    @Test
    public void testConstructor() {
        new HtmlResponseProxy(POST_RESPONSE);
    }
    
    private static final PostResponse POST_RESPONSE = new PostResponse() {
        
        public void setTitle(String title) {
        }
        
        public void setStatus(int code, String message) {
        }
        
        public void setReferer(String referer) {
        }
        
        public void setPath(String path) {
        }
        
        public void setParentLocation(String parentLocation) {
        }
        
        public void setLocation(String location) {
        }
        
        public void setError(Throwable error) {
        }
        
        public void setCreateRequest(boolean isCreateRequest) {
        }
        
        public void send(HttpServletResponse response, boolean setStatus) throws IOException {
        }
        
        public void onMoved(String srcPath, String dstPath) {
        }
        
        public void onModified(String path) {
        }
        
        public void onDeleted(String path) {
        }
        
        public void onCreated(String path) {
        }
        
        public void onCopied(String srcPath, String dstPath) {
        }
        
        public void onChange(String type, String... arguments) {
        }
        
        public boolean isSuccessful() {
            return false;
        }
        
        public boolean isCreateRequest() {
            return false;
        }
        
        public String getStatusMessage() {
            return null;
        }
        
        public int getStatusCode() {
            return 0;
        }
        
        public String getReferer() {
            return null;
        }
        
        public String getPath() {
            return null;
        }
        
        public String getParentLocation() {
            return null;
        }
        
        public String getLocation() {
            return null;
        }
        
        public Throwable getError() {
            return null;
        }
    };
}
