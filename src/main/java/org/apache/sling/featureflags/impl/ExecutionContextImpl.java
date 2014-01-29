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

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.featureflags.ExecutionContext;

/**
 * Implementation of the provider context.
 */
public class ExecutionContextImpl implements ExecutionContext {

    private final ResourceResolver resourceResolver;

    private final HttpServletRequest request;

    public ExecutionContextImpl(final ResourceResolver resourceResolver) {
        this.request = null;
        this.resourceResolver = resourceResolver;
    }

    public ExecutionContextImpl(final HttpServletRequest request) {
        this.request = request;
        ResourceResolver resolver = (request instanceof SlingHttpServletRequest)
                ? ((SlingHttpServletRequest) request).getResourceResolver()
                : null;
        if ( resolver == null ) {
            // get ResourceResolver (set by AuthenticationSupport)
            final Object resolverObject = request.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
            resolver = (resolverObject instanceof ResourceResolver)
                    ? (ResourceResolver) resolverObject
                    : null;

        }
        this.resourceResolver = resolver;
    }

    @Override
    public HttpServletRequest getRequest() {
        return this.request;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }
}
