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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.osgi.framework.Constants;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(metatype = false, policy = ConfigurationPolicy.IGNORE)
@Service(ServletContextHelper.class)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Sling JcrRemoting Servlet"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, value = AuthHttpContext.HTTP_CONTEXT_NAME),
    @Property(name = HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, value = "/"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 5)
})
public class AuthHttpContext extends ServletContextHelper {

    /**
     * The name of this ServletContext for use by they SlingDavExServlet
     */
    static final String HTTP_CONTEXT_NAME = "DavExAuthHttpContext";

    /**
     * Handles security
     *
     * @see #handleSecurity(HttpServletRequest, HttpServletResponse)
     */
    @Reference
    private AuthenticationSupport authenticator;

    // ---------- ServletContextHelper

    /**
     * Always returns <code>null</code> as resources are only accessible through
     * the {@link SlingDavExServlet}.
     */
    @Override
    public Set<String> getResourcePaths(String path) {
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

        // Paths to consider  | Result
        // -------------------+---------
        // null               | null
        // "" (empty)         | null
        // /                  | null
        // /wsp               | wsp
        // /wsp/              | wsp
        // /wsp/...           | wsp

        if (uriPath != null && uriPath.length() > 1 && uriPath.charAt(0) == '/') {
            int end = uriPath.indexOf('/', 1);
            if (end > 1) {
                return uriPath.substring(1, end);
            } else if (end < 0) {
                return uriPath.substring(1);
            }
        }

        return null;
    }
}
