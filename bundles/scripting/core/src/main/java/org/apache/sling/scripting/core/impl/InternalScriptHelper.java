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
package org.apache.sling.scripting.core.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.scripting.core.ScriptHelper;
import org.osgi.framework.BundleContext;

/**
 * Internal script helper
 */
public class InternalScriptHelper extends ScriptHelper {

    private final ServiceCache serviceCache;

    public InternalScriptHelper(final BundleContext ctx,
            final SlingScript script,
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response,
            final ServiceCache cache) {
        super(ctx, script, request, response);
        this.serviceCache = cache;
    }

    public InternalScriptHelper(final BundleContext ctx,
            final SlingScript script,
            final ServiceCache cache) {
        super(ctx, script);
        this.serviceCache = cache;
    }

    /**
     * @see org.apache.sling.api.scripting.SlingScriptHelper#getService(java.lang.Class)
     */
    public <ServiceType> ServiceType getService(Class<ServiceType> type) {
        return this.serviceCache.getService(type);
    }
}
