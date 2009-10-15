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
package org.apache.sling.engine.impl.auth;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.engine.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LogoutServlet</code> lets the Authenticator
 * do the logout.
 *
 * @scr.component metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="Authenticator Logout Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.servlet.methods" values.0="GET" values.1="POST"
 *
 * @since 2.1
 */
public class LogoutServlet extends SlingAllMethodsServlet {

    /** serialization UID */
    private static final long serialVersionUID = -1L;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private Authenticator authenticator;

    /** The servlet is registered on this path.
     *  @scr.property name="sling.servlet.paths" */
    public static final String LOGIN_SERVLET_PATH = "/system/sling/logout";

    @Override
    protected void service(SlingHttpServletRequest request,
            SlingHttpServletResponse response) {

        Authenticator authenticator = this.authenticator;
        if (authenticator != null) {
            try {
                authenticator.logout(request, response);
                return;
            } catch (IllegalStateException ise) {
                log.error("service: Response already committed, cannot logout");
                return;
            }
        }

        log.error("service: Authenticator service missing, cannot logout");

        // well, we don't really have something to say here, do we ?
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
