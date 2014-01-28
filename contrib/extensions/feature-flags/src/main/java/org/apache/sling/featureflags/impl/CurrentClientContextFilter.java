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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.sling.featureflags.ClientContext;

/**
 * This general servlet filter sets the current client context to the current
 * request.
 */
public class CurrentClientContextFilter implements Filter {

    private final FeatureManager featureManager;

    public CurrentClientContextFilter(final FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    @Override
    public void init(final FilterConfig config) {
        // nothing to do
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {

        ClientContext current = this.featureManager.setCurrentClientContext(req);
        try {
            chain.doFilter(req, res);
        } finally {
            this.featureManager.unsetCurrentClientContext(current);
        }
    }

    @Override
    public void destroy() {
        // nothing to do
    }
}
