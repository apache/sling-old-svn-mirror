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
package org.apache.sling.engine.impl.filter;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.impl.SlingMainServlet;
import org.apache.sling.engine.impl.SlingRequestProcessorImpl;
import org.apache.sling.engine.impl.filter.ServletFilterManager.FilterChainType;

/**
 * The <code>RequestSlingFilterChain</code> implements the filter chain for
 * request scoped filters. It is used by the
 * {@link SlingMainServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * method to dispatch request processing.
 */
public class RequestSlingFilterChain extends AbstractSlingFilterChain {

    private final SlingRequestProcessorImpl handler;

    public RequestSlingFilterChain(SlingRequestProcessorImpl handler, FilterHandle[] filters) {
        super(filters);
        this.handler = handler;
    }

    protected void render(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {
        handler.processComponent(request, response,FilterChainType.COMPONENT);
    }
}