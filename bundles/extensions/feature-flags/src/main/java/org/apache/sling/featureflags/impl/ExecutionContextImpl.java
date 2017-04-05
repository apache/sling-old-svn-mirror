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
package org.apache.sling.featureflags.impl;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.featureflags.ExecutionContext;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.Features;

/**
 * Implementation of the provider context.
 */
public class ExecutionContextImpl implements ExecutionContext {

    private static final String REQUEST_ATTRIBUTE_RESOLVER = "org.apache.sling.auth.core.ResourceResolver";

    private final ResourceResolver resourceResolver;

    private final HttpServletRequest request;

    private final Map<String, Boolean> featureCache;

    private final Features features;

    public ExecutionContextImpl(final Features features, final HttpServletRequest request) {
        ResourceResolver resourceResolver = null;
        if (request != null) {
            if ( request instanceof SlingHttpServletRequest ) {
                resourceResolver = ((SlingHttpServletRequest)request).getResourceResolver();
            } else {
                Object resolverObject = request.getAttribute(REQUEST_ATTRIBUTE_RESOLVER);
                if (resolverObject instanceof ResourceResolver) {
                    resourceResolver = (ResourceResolver) resolverObject;
                }
            }
        }

        this.request = request;
        this.resourceResolver = resourceResolver;
        this.featureCache = new HashMap<String, Boolean>();
        this.features = features;
    }

    @Override
    public HttpServletRequest getRequest() {
        return this.request;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }

    @Override
    public Features getFeatures() {
        return this.features;
    }

    boolean isEnabled(final Feature feature) {
        final String name = feature.getName();
        Boolean entry = this.featureCache.get(name);
        if (entry == null) {
            // put false in the cache to stop on circular calls
            this.featureCache.put(name, Boolean.FALSE);
            entry = feature.isEnabled(this);
            this.featureCache.put(name, entry);
        }
        return entry;
    }
}
