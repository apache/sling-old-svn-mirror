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
package org.apache.sling.extensions.featureflags.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.extensions.featureflags.ExecutionContext;
import org.apache.sling.extensions.featureflags.Feature;

@Component
@Service(value=Filter.class)
@Property(name="pattern", value="/.*")
public class ExecutionContextFilter implements Filter {

    private static ThreadLocal<ExecutionContextInfo> EXECUTION_CONTEXT;

    @Reference
    private Feature feature;

    public static ExecutionContextInfo getCurrentExecutionContextInfo() {
        final ThreadLocal<ExecutionContextInfo> local = EXECUTION_CONTEXT;
        if ( local != null ) {
            return local.get();
        }
        return null;
    }

    @Override
    public void destroy() {
        EXECUTION_CONTEXT = null;
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res,
            final FilterChain chain)
    throws IOException, ServletException {
        final ThreadLocal<ExecutionContextInfo> local = EXECUTION_CONTEXT;
        if ( local != null && req instanceof SlingHttpServletRequest ) {
            local.set(new ExecutionContextInfo((SlingHttpServletRequest)req, feature));
        }
        try {
            chain.doFilter(req, res);
        } finally {
            if ( local != null && req instanceof SlingHttpServletRequest ) {
                local.set(null);
            }
        }
    }

    @Override
    public void init(final FilterConfig config) throws ServletException {
        EXECUTION_CONTEXT = new ThreadLocal<ExecutionContextInfo>();
    }

    public final class ExecutionContextInfo {

        public final ExecutionContext context;
        public final List<String> enabledFeatures = new ArrayList<String>();

        public ExecutionContextInfo(final SlingHttpServletRequest req,
                final Feature feature) {
            this.context = ExecutionContext.fromRequest(req);
            for(final String name : feature.getFeatureNames()) {
                if ( feature.isEnabled(name, context) ) {
                    enabledFeatures.add(name);
                }
            }
            Collections.sort(enabledFeatures);
        }
    }
}
