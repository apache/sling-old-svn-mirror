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
import org.apache.sling.engine.impl.request.RequestData;

/**
 * The <code>SlingComponentFilterChain</code> implements the filter chain for
 * component scoped filters. It is used by the
 * {@link org.apache.sling.engine.impl.SlingMainServlet#processRequest(SlingHttpServletRequest, SlingHttpServletResponse)}
 * method to dispatch component processing.
 */
public class SlingComponentFilterChain extends AbstractSlingFilterChain {

    public SlingComponentFilterChain(FilterHandle[] filters) {
        super(filters);
    }

    protected void render(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException {
        RequestData.service(request, response);
    }
}