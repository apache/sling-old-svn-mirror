/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.davex.impl.servlets;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.osgi.service.http.HttpContext;

class AuthHttpContext implements HttpContext {

    /**
     * Handles security
     *
     * @see #handleSecurity(HttpServletRequest, HttpServletResponse)
     */
    private AuthenticationSupport authenticator;

    public void setAuthenticationSupport(final AuthenticationSupport auth) {
        this.authenticator = auth;
    }

    // ---------- HttpContext interface ----------------------------------------

    /**
     * Returns the MIME type as resolved by the <code>MimeTypeService</code> or
     * <code>null</code> if the service is not available.
     */
    public String getMimeType(String name) {
        return null;
    }

    /**
     * Always returns <code>null</code> because resources are all provided
     * through the {@link MainServlet}.
     */
    public URL getResource(String name) {
        return null;
    }

    /**
     * Tries to authenticate the request using the
     * <code>SlingAuthenticator</code>. If the authenticator or the Repository
     * is missing this method returns <code>false</code> and sends a 503/SERVICE
     * UNAVAILABLE status back to the client.
     */
    public boolean handleSecurity(final HttpServletRequest request,
            final HttpServletResponse response)
    throws IOException {
        final AuthenticationSupport localAuthenticator = this.authenticator;
        if ( localAuthenticator != null ) {
            request.setAttribute(AuthenticationHandler.REQUEST_LOGIN_PARAMETER, "BASIC");
            return localAuthenticator.handleSecurity(request, response);
        }
        // send 503/SERVICE UNAVAILABLE, flush to ensure delivery
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.flushBuffer();

        // terminate this request now
        return false;
    }
}
