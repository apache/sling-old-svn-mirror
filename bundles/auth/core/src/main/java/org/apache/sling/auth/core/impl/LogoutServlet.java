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
package org.apache.sling.auth.core.impl;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.core.AuthUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LogoutServlet</code> lets the Authenticator
 * do the logout.
 */
@Component(metatype=true, label="Apache Sling Authentication Logout Servlet",
           description="Servlet for logging out users through the authenticator service.")
@Service(value = Servlet.class)
@Properties( {
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Authenticator Logout Servlet"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = "sling.servlet.methods", value = { "GET", "POST" } ,
              label = "Method", description = "Supported Methdos", unbounded=PropertyUnbounded.ARRAY)
})
public class LogoutServlet extends SlingAllMethodsServlet {

    /** serialization UID */
    private static final long serialVersionUID = -1L;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private Authenticator authenticator;

    /**
     * The servlet is registered on this path.
     */
    @Property(name = "sling.servlet.paths")
    public static final String SERVLET_PATH = "/system/sling/logout";

    @Override
    protected void service(SlingHttpServletRequest request,
            SlingHttpServletResponse response) {

        final Authenticator authenticator = this.authenticator;
        if (authenticator != null) {
            try {
                AuthUtil.setLoginResourceAttribute(request, null);
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
