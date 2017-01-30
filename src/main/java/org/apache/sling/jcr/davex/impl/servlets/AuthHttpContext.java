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
import org.osgi.service.http.HttpContext;

class AuthHttpContext implements HttpContext {

    /**
     * The root path at which the DavEx servlet is registered. This is used to
     * extract the workspace name from the request URL where the workspace name
     * is the first segment in the path after the this path.
     */
    private final String davRoot;

    /**
     * Handles security
     *
     * @see #handleSecurity(HttpServletRequest, HttpServletResponse)
     */
    private AuthenticationSupport authenticator;

    AuthHttpContext(final String davRoot) {
        this.davRoot = davRoot;
    }

    public void setAuthenticationSupport(final AuthenticationSupport auth) {
        this.authenticator = auth;
    }

    // ---------- HttpContext

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
    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final AuthenticationSupport localAuthenticator = this.authenticator;
        if (localAuthenticator != null) {

            final String wsp = getWorkspace(request.getPathInfo());
            if (wsp != null) {
                request.setAttribute("j_workspace", wsp);
            }
            return localAuthenticator.handleSecurity(request, response);
        }
        // send 503/SERVICE UNAVAILABLE, flush to ensure delivery
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.flushBuffer();

        // terminate this request now
        return false;
    }

    private final String getWorkspace(final String uriPath) {

        // Paths to consider
        // /davRoot
        // /davRoot/
        // /davRoot/wsp
        // /davRoot/wsp/
        // /davRoot/wsp/...

        if (uriPath != null && uriPath.startsWith(this.davRoot)) {

            // cut off root
            int start = this.davRoot.length();

            // just the root
            if (start >= uriPath.length()) {
                return null;
            }

            if (uriPath.charAt(start) == '/') {
                start++;
            } else {
                // expected slash, actually (don't care)
                return null;
            }

            // just the root with trailing slash
            if (start >= uriPath.length()) {
                return null;
            }

            int end = uriPath.indexOf('/', start);
            if (end > start) {
                return uriPath.substring(start, end);
            } else if (end < 0) {
                return uriPath.substring(start);
            }
        }

        return null;
    }
}
