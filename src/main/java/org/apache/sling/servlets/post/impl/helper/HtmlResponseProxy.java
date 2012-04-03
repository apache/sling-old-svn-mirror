/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.servlets.post.impl.helper;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.PostResponse;

/**
 * The <code>HtmlResponseProxy</code> extends the Sling API
 * <code>HtmlResponse</code> overwriting all public methods and redirecting to a
 * proxied {@link PostResponse}. As a consequence the underlying (extended)
 * Sling API <code>HtmlResponse</code> will not be fed with data and thus will
 * remain "empty".
 * <p>
 * This class is mainly used by the deprecated
 * {@link org.apache.sling.servlets.post.AbstractSlingPostOperation} for
 * bridging into the new
 * {@link org.apache.sling.servlets.post.AbstractPostOperation}.
 */
public class HtmlResponseProxy extends HtmlResponse {

    private final PostResponse postResponse;
    private boolean createRequest;

    public HtmlResponseProxy(final PostResponse postResponse) {
        if(postResponse == null) {
            throw new IllegalArgumentException("Null PostResponse, cannot build HtmlResponseProxy");
        }
        this.postResponse = postResponse;
        postResponse.setCreateRequest(createRequest);
    }

    public PostResponse getPostResponse() {
        return postResponse;
    }

    public <Type> Type getProperty(String name, Class<Type> type) {
        // return postResponse.getProperty(name, type);
        return null;
    }

    public Object getProperty(String name) {
        // return postResponse.getProperty(name);
        return null;
    }

    public void setProperty(String name, Object value) {
        // postResponse.setProperty(name, value);
    }

    public Throwable getError() {
        return postResponse.getError();
    }

    public String getLocation() {
        return postResponse.getLocation();
    }

    public String getParentLocation() {
        return postResponse.getParentLocation();
    }

    public String getPath() {
        return postResponse.getPath();
    }

    public String getReferer() {
        return postResponse.getReferer();
    }

    public int getStatusCode() {
        return postResponse.getStatusCode();
    }

    public String getStatusMessage() {
        return postResponse.getStatusMessage();
    }

    public boolean isCreateRequest() {
        return postResponse.isCreateRequest();
    }

    public boolean isSuccessful() {
        return postResponse.isSuccessful();
    }

    public void onChange(String type, String... arguments) {
        postResponse.onChange(type, arguments);
    }

    public void onCopied(String srcPath, String dstPath) {
        postResponse.onCopied(srcPath, dstPath);
    }

    public void onCreated(String path) {
        postResponse.onCreated(path);
    }

    public void onDeleted(String path) {
        postResponse.onDeleted(path);
    }

    public void onModified(String path) {
        postResponse.onModified(path);
    }

    public void onMoved(String srcPath, String dstPath) {
        postResponse.onMoved(srcPath, dstPath);
    }

    public void send(HttpServletResponse response, boolean setStatus)
            throws IOException {
        postResponse.send(response, setStatus);
    }

    public void setCreateRequest(boolean isCreateRequest) {
        createRequest = isCreateRequest;
        if(postResponse != null) {
            // ugly...needed because of SLING-2453, this is called
            // by the base class's constructor before postResponse is set
            postResponse.setCreateRequest(isCreateRequest);
        }
    }

    public void setError(Throwable error) {
        postResponse.setError(error);
    }

    public void setLocation(String location) {
        postResponse.setLocation(location);
    }

    public void setParentLocation(String parentLocation) {
        postResponse.setParentLocation(parentLocation);
    }

    public void setPath(String path) {
        postResponse.setPath(path);
    }

    public void setReferer(String referer) {
        postResponse.setReferer(referer);
    }

    public void setStatus(int code, String message) {
        postResponse.setStatus(code, message);
    }

    public void setTitle(String title) {
        postResponse.setTitle(title);
    }
}
