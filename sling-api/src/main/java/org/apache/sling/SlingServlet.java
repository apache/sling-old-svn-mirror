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
package org.apache.sling;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * The <code>SlingServlet</code> is a convenience extension to the Servlet API
 * <code>GenericServlet</code> which forwards requests to a
 * {@link #service(SlingRequest, SlingResponse) service} method providing the
 * request and response objects as {@link SlingRequest} and
 * {@link SlingResponse} objects respectively.
 * <p>
 * Extensions may implement the new service method and make use of the
 * functionality of the <code>GenericServlet</code>.
 */
public abstract class SlingServlet extends GenericServlet {

    public abstract void service(SlingRequest request, SlingResponse response)
            throws ServletException, IOException;

    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
        try {
            service((SlingRequest) req, (SlingResponse) res);
        } catch (ClassCastException e) {
            throw new SlingException("Non-Sling request or response");
        }
    }

}
