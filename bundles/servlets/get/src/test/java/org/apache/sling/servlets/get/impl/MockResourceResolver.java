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
package org.apache.sling.servlets.get.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code>MockResourceResolver</code> implements the {@link #map(String)}
 * method simply returning the path unmodified and the
 * {@link #map(HttpServletRequest, String)} method returning the path prefixed
 * with the request context path.
 * <p>
 * The other methods are not implemented and return <code>null</code>.
 */
public class MockResourceResolver implements ResourceResolver {

    public Iterator<Resource> findResources(String arg0, String arg1) {
        return null;
    }

    public Resource getResource(String arg0) {
        return null;
    }

    public Resource getResource(Resource arg0, String arg1) {
        return null;
    }

    public String[] getSearchPath() {
        return null;
    }

    public Iterator<Resource> listChildren(Resource arg0) {
        return null;
    }

    public String map(String path) {
        return path;
    }

    public String map(HttpServletRequest request, String path) {
        if (request.getContextPath().length() == 0) {
            return path;
        }

        return request.getContextPath() + path;
    }

    public Iterator<Map<String, Object>> queryResources(String arg0, String arg1) {
        return null;
    }

    public Resource resolve(String arg0) {
        return null;
    }

    public Resource resolve(HttpServletRequest arg0) {
        return null;
    }

    public Resource resolve(HttpServletRequest arg0, String arg1) {
        return null;
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> arg0) {
        return null;
    }

    public void close() {
        // nothing to do
    }

    public String getUserID() {
        return null;
    }

    public boolean isLive() {
        return true;
    }

    public ResourceResolver clone(Map<String, Object> authenticationInfo) {
        throw new UnsupportedOperationException("clone");
    }

    public Object getAttribute(String name) {
        return null;
    }

    public Iterator<String> getAttributeNames() {
        return Collections.<String> emptyList().iterator();
    }
}
