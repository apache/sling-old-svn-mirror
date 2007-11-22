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
package org.apache.sling.core.impl;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.core.impl.auth.SlingAuthenticator;
import org.osgi.service.http.HttpContext;

/**
 * The <code>SlingHttpContext</code> implements the OSGi
 * <code>HttpContext</code> interface to provide specialized support for
 * Sling.
 */
public class SlingHttpContext implements HttpContext {

    /**
     * The name of the request attribute set by the {@link SlingAuthenticator}
     * when authenticating the request user. Existence of this attribute in the
     * request, provided it is a JCR Session, signals that authentication has
     * already taken place. This may be used when including through the servlet
     * container.
     */
    public static final String SESSION = "org.apache.sling.core.session";

    /** The helper to map MIME types */
    private MimeTypeService mimeTypeService;

    /** The helper to authenticate requests */
    private final SlingAuthenticator slingAuthenticator;

    /**
     * Creates an instance of this OSGi HttpContext implementation using the
     * give MIME type and authentication helpers.
     */
    SlingHttpContext(MimeTypeService mimeTypeService,
            SlingAuthenticator slingAuthenticator) {
        this.mimeTypeService = mimeTypeService;
        this.slingAuthenticator = slingAuthenticator;
    }

    /** Asks the MimeTypeService for the MIME type mapping */
    public String getMimeType(String name) {
        return this.mimeTypeService.getMimeType(name);
    }

    /** Always returns <code>null</code>, we have no resources here */
    public URL getResource(String name) {
        return null;
    }

    /** Asks the SlingAuthenticator to authenticate the request */
    public boolean handleSecurity(HttpServletRequest request,
            HttpServletResponse response) {
        return slingAuthenticator.authenticate(request, response);
    }
}
