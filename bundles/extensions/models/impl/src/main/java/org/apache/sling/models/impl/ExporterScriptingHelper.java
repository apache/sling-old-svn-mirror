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
package org.apache.sling.models.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;

class ExporterScriptingHelper implements SlingScriptHelper {

    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;

    public ExporterScriptingHelper(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public void dispose() {

    }

    @Override
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    @Override
    public SlingHttpServletResponse getResponse() {
        return response;
    }

    @Override
    public SlingScript getScript() {
        return null;
    }

    @Override
    public void include(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(String s, RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(Resource resource, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void include(Resource resource, RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(String s, RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(Resource resource, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forward(Resource resource, RequestDispatcherOptions requestDispatcherOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <ServiceType> ServiceType getService(Class<ServiceType> aClass) {
        return null;
    }

    @Override
    public <ServiceType> ServiceType[] getServices(Class<ServiceType> aClass, String s) {
        return null;
    }
}
