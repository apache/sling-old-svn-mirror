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
 * The <code>HtmlPostResponseProxy</code> class implements the
 * {@link PostResponse} interface using a Sling API <code>HtmlResponse</code>.
 * <p>
 * This class is mainly used by the deprecated
 * {@link org.apache.sling.servlets.post.AbstractSlingPostOperation} for
 * bridging into the new
 * {@link org.apache.sling.servlets.post.AbstractPostOperation}.
 * @deprecated
 */
@Deprecated
public class HtmlPostResponseProxy implements PostResponse {

    private final HtmlResponse apiHtmlResponse;

    public HtmlPostResponseProxy(final HtmlResponse apiHtmlResponse) {
        this.apiHtmlResponse = apiHtmlResponse;
    }

    public HtmlResponse getHtmlResponse() {
        return apiHtmlResponse;
    }

    @Override
    public Throwable getError() {
        return apiHtmlResponse.getError();
    }

    @Override
    public String getLocation() {
        return apiHtmlResponse.getLocation();
    }

    @Override
    public String getParentLocation() {
        return apiHtmlResponse.getParentLocation();
    }

    @Override
    public String getPath() {
        return apiHtmlResponse.getPath();
    }

    public <Type> Type getProperty(String name, Class<Type> type) {
        return apiHtmlResponse.getProperty(name, type);
    }

    public Object getProperty(String name) {
        return apiHtmlResponse.getProperty(name);
    }

    @Override
    public String getReferer() {
        return apiHtmlResponse.getReferer();
    }

    @Override
    public int getStatusCode() {
        return apiHtmlResponse.getStatusCode();
    }

    @Override
    public String getStatusMessage() {
        return apiHtmlResponse.getStatusMessage();
    }

    @Override
    public boolean isCreateRequest() {
        return apiHtmlResponse.isCreateRequest();
    }

    @Override
    public boolean isSuccessful() {
        return apiHtmlResponse.isSuccessful();
    }

    @Override
    public void onChange(String type, String... arguments) {
        apiHtmlResponse.onChange(type, arguments);
    }

    @Override
    public void onCopied(String srcPath, String dstPath) {
        apiHtmlResponse.onCopied(srcPath, dstPath);
    }

    @Override
    public void onCreated(String path) {
        apiHtmlResponse.onCreated(path);
    }

    @Override
    public void onDeleted(String path) {
        apiHtmlResponse.onDeleted(path);
    }

    @Override
    public void onModified(String path) {
        apiHtmlResponse.onModified(path);
    }

    @Override
    public void onMoved(String srcPath, String dstPath) {
        apiHtmlResponse.onMoved(srcPath, dstPath);
    }

    @Override
    public void send(HttpServletResponse response, boolean setStatus)
            throws IOException {
        apiHtmlResponse.send(response, setStatus);
    }

    @Override
    public void setCreateRequest(boolean isCreateRequest) {
        apiHtmlResponse.setCreateRequest(isCreateRequest);
    }

    @Override
    public void setError(Throwable error) {
        apiHtmlResponse.setError(error);
    }

    @Override
    public void setLocation(String location) {
        apiHtmlResponse.setLocation(location);
    }

    @Override
    public void setParentLocation(String parentLocation) {
        apiHtmlResponse.setParentLocation(parentLocation);
    }

    @Override
    public void setPath(String path) {
        apiHtmlResponse.setPath(path);
    }

    public void setProperty(String name, Object value) {
        apiHtmlResponse.setProperty(name, value);
    }

    @Override
    public void setReferer(String referer) {
        apiHtmlResponse.setReferer(referer);
    }

    @Override
    public void setStatus(int code, String message) {
        apiHtmlResponse.setStatus(code, message);
    }

    @Override
    public void setTitle(String title) {
        apiHtmlResponse.setTitle(title);
    }
}
