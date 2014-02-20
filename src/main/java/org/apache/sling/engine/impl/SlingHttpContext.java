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
package org.apache.sling.engine.impl;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.engine.impl.parameters.ParameterSupport;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingHttpContext</code> implements the OSGi HttpContext used to
 * register the {@link SlingMainServlet} with the OSGi HttpService.
 */
class SlingHttpContext implements HttpContext {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(SlingHttpContext.class);

    /**
     * Resolves MIME types
     *
     * @see #getMimeType(String)
     */
    private MimeTypeService mimeTypeService;

    /**
     * Handles security
     *
     * @see #handleSecurity(HttpServletRequest, HttpServletResponse)
     */
    private AuthenticationSupport authenticationSupport;

    public void setMimeTypeService(MimeTypeService mimeTypeService) {
        this.mimeTypeService = mimeTypeService;
    }

    public void unsetMimeTypeService(MimeTypeService mimeTypeService) {
        if (this.mimeTypeService == mimeTypeService) {
            this.mimeTypeService = null;
        }
    }

    public void setAuthenticationSupport(
            AuthenticationSupport authenticationSupport) {
        this.authenticationSupport = authenticationSupport;
    }

    public void unsetAuthenticationSupport(
            AuthenticationSupport authenticationSupport) {
        if (this.authenticationSupport == authenticationSupport) {
            this.authenticationSupport = null;
        }
    }

    // ---------- HttpContext interface ----------------------------------------

    /**
     * Returns the MIME type as resolved by the <code>MimeTypeService</code> or
     * <code>null</code> if the service is not available.
     */
    public String getMimeType(String name) {
        MimeTypeService mtservice = mimeTypeService;
        if (mtservice != null) {
            return mtservice.getMimeType(name);
        }

        log.debug(
            "getMimeType: MimeTypeService not available, cannot resolve mime type for {}",
            name);
        return null;
    }

    /**
     * Always returns <code>null</code> because resources are all provided
     * through the {@link SlingMainServlet}.
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
    public boolean handleSecurity(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        final AuthenticationSupport authenticator = this.authenticationSupport;
        if (authenticator != null) {

            // SLING-559: ensure correct parameter handling according to
            // ParameterSupport
            request = ParameterSupport.getParameterSupportRequestWrapper(request);

            return authenticator.handleSecurity(request, response);

        }

        log.error("handleSecurity: AuthenticationSupport service missing. Cannot authenticate request.");
        log.error("handleSecurity: Possible reason is missing Repository service. Check AuthenticationSupport dependencies.");

        // send 503/SERVICE UNAVAILABLE, flush to ensure delivery
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
            "AuthenticationSupport service missing. Cannot authenticate request.");
        response.flushBuffer();

        // terminate this request now
        return false;
    }
}
