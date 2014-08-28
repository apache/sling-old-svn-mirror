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

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.auth.NoAuthenticationHandlerException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.core.AuthUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LoginServlet</code> lets the Authenticator do the login.
 */
@Component()
@Service(value = Servlet.class)
@Properties( {
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Authenticator Login Servlet"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = "sling.servlet.methods", value = { "GET", "POST" }) })
public class LoginServlet extends SlingAllMethodsServlet {

    /** serialization UID */
    private static final long serialVersionUID = -8797082194403667968L;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private volatile Authenticator authenticator;

    /**
     * The servlet is registered on this path, and the authenticator allows any
     * requests to that path, without authentication
     */
    @Property(name = "sling.servlet.paths")
    public static final String SERVLET_PATH = "/system/sling/login";

    @Override
    protected void service(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {


        // if the request is logged in and the resource is not set (such
        // as when requesting /system/sling/login from the browser with the
        // browser sending credentials) or the resource is set to the login
        // servlet as a result of authenticating after providing credentials
        // through the login servlet), redirect to root now assuming we are
        // authenticated.
        if (request.getAuthType() != null) {
            final String resourcePath = AuthUtil.getLoginResource(request, null);
            if (isSelf(resourcePath)) {
                String redirectTarget = request.getContextPath() + "/";
                log.warn(
                    "doGet: Redirecting to {} to prevent login loop for resource {}",
                    redirectTarget, resourcePath);
                response.sendRedirect(redirectTarget);
                return;
            }
        }

        Authenticator authenticator = this.authenticator;
        if (authenticator != null) {
            try {

                // set the login resource to select the authenticator
                AuthUtil.setLoginResourceAttribute(request, null);
                authenticator.login(request, response);
                return;

            } catch (IllegalStateException ise) {

                log.error("doGet: Response already committed, cannot login");
                return;

            } catch (NoAuthenticationHandlerException nahe) {

                log.error("doGet: No AuthenticationHandler to login registered");

            }

        } else {

            log.error("doGet: Authenticator service missing, cannot login");

        }

        // fall back to forbid access
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot login");
    }

    private boolean isSelf(final String resourcePath) {
        // no resource, assume self
        if (resourcePath == null) {
            return true;
        }

        // login servlet is addressed
        if (resourcePath.startsWith(SERVLET_PATH)) {
            return true;
        }

        // not a prefix
        return false;
    }
}
