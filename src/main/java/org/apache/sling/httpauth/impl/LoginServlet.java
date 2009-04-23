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
package org.apache.sling.httpauth.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.engine.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LoginServlet</code> TODO
 * 
 * @scr.component metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="HTTP Header Login Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.servlet.paths" value="/system/sling/login"
 * @scr.property name="sling.servlet.methods" values.0="GET" values.1="POST"
 */
public class LoginServlet extends SlingAllMethodsServlet {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private Authenticator authenticator;

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        Authenticator authenticator = this.authenticator;
        if (authenticator != null) {
            authenticator.login(request, response);
        } else {
            log.error("doGet: Authenticator service missing, cannot request authentication");
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Cannot request Authentication");
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {
        response.sendRedirect(request.getRequestURI());
    }

}
