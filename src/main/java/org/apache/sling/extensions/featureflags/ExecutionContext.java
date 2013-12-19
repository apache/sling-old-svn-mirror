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
package org.apache.sling.extensions.featureflags;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

public class ExecutionContext {

    private final ResourceResolver resourceResolver;

    private final SlingHttpServletRequest request;

    public static ExecutionContext fromRequest(final SlingHttpServletRequest request) {
        return new ExecutionContext(request);
    }

    public static ExecutionContext fromResourceResolver(final ResourceResolver resourceResolver) {
        return new ExecutionContext(resourceResolver);
    }

    private ExecutionContext(final ResourceResolver resourceResolver) {
        this.request = null;
        this.resourceResolver = resourceResolver;
    }


    private ExecutionContext(final SlingHttpServletRequest request) {
        this.request = request;
        this.resourceResolver = request.getResourceResolver();
    }

    public SlingHttpServletRequest getRequest() {
        return this.request;
    }

    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }
}
