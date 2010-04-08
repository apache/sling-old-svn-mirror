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
package org.apache.sling.engine.impl;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfilingResourceResolver implements ResourceResolver {

    private final ResourceResolver delegatee;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    int getResource;

    public ProfilingResourceResolver(final ResourceResolver delegatee) {
        this.delegatee = delegatee;
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return delegatee.adaptTo(type);
    }

    public void close() {
        logger.info("* #getResource({})", getResource);
        delegatee.close();
    }

    public Iterator<Resource> findResources(String query, String language) {
        return delegatee.findResources(query, language);
    }

    public Resource getResource(Resource base, String path) {
        getResource++;
        logger.info("* getResource({}, {})", base.getPath(), path);
        return delegatee.getResource(base, path);
    }

    public Resource getResource(String path) {
        getResource++;
        logger.info("* getResource({})", path);
        return delegatee.getResource(path);
    }

    public String[] getSearchPath() {
        return delegatee.getSearchPath();
    }

    public Iterator<Resource> listChildren(Resource parent) {
        logger.info("* listChildren({})", parent.getPath());
        return delegatee.listChildren(parent);
    }

    public String map(HttpServletRequest request, String resourcePath) {
        return delegatee.map(request, resourcePath);
    }

    public String map(String resourcePath) {
        return delegatee.map(resourcePath);
    }

    public Iterator<Map<String, Object>> queryResources(String query,
            String language) {
        return delegatee.queryResources(query, language);
    }

    public Resource resolve(HttpServletRequest request, String absPath) {
        return delegatee.resolve(request, absPath);
    }

    public Resource resolve(HttpServletRequest request) {
        return delegatee.resolve(request);
    }

    public Resource resolve(String path) {
        logger.info("* resolve({})", path);
        return delegatee.resolve(path);
    }
}
