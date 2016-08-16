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
package org.apache.sling.auth.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The <code>AuthenticationSupport</code> provides the service API used to
 * implement the <code>HttpContext.handleSecurity</code> method as defined in
 * the OSGi Http Service specification.
 * <p>
 * Bundles registering servlets and/or resources with custom
 * <code>HttpContext</code> implementations may implement the
 * <code>handleSecurity</code> method using this service. The
 * {@link #handleSecurity(HttpServletRequest, HttpServletResponse)} method
 * implemented by this service exactly implements the specification of the
 * <code>HttpContext.handleSecurity</code> method.
 * <p>
 * A simple implementation of the <code>HttpContext</code> interface based on
 * this could be (using SCR JavaDoc tags of the Maven SCR Plugin) :
 *
 * <pre>
 * &#47;** &#64;scr.component *&#47;
 * public class MyHttpContext implements HttpContext {
 *     &#47;** &#64;scr.reference *&#47;
 *     private AuthenticationSupport authSupport;
 *
 *     &#47;** &#64;scr.reference *&#47;
 *     private MimeTypeService mimeTypes;
 *
 *     public boolean handleSecurity(HttpServletRequest request,
 *             HttpServletResponse response) {
 *         return authSupport.handleSecurity(request, response);
 *     }
 *
 *     public URL getResource(String name) {
 *         return null;
 *     }
 *
 *     public String getMimeType(String name) {
 *         return mimeTypes.getMimeType(name);
 *     }
 * }
 * </pre>
 * <p>
 * This interface is implemented by this bundle and is not intended to be
 * implemented by client bundles.
 */
@ProviderType
public interface AuthenticationSupport {

    /**
     * The name under which this service is registered.
     */
    static final String SERVICE_NAME = "org.apache.sling.auth.core.AuthenticationSupport";

    /**
     * The name of the request attribute set by the
     * {@link #handleSecurity(HttpServletRequest, HttpServletResponse)} method
     * if authentication succeeds and <code>true</code> is returned.
     * <p>
     * The request attribute is set to a Sling <code>ResourceResolver</code>
     * attached to the JCR repository using the credentials provided by the
     * request.
     */
    static final String REQUEST_ATTRIBUTE_RESOLVER = "org.apache.sling.auth.core.ResourceResolver";

    /**
     * The name of the request parameter indicating where to redirect to after
     * successful authentication (and optional impersonation). This parameter is
     * respected if either anonymous authentication or regular authentication
     * succeed.
     * <p>
     * If authentication fails, either because the credentials are wrong or
     * because anonymous authentication fails or because anonymous
     * authentication is not allowed for the request, the parameter is ignored
     * and the
     * {@link org.apache.sling.auth.core.spi.AuthenticationHandler#requestCredentials(HttpServletRequest, HttpServletResponse)}
     * method is called to request authentication.
     */
    static final String REDIRECT_PARAMETER = "sling.auth.redirect";

    /**
     * Handles security on behalf of a custom OSGi Http Service
     * <code>HttpContext</code> instance extracting credentials from the request
     * using any registered
     * {@link org.apache.sling.auth.core.spi.AuthenticationHandler} services.
     * If the credentials can be extracted and used to log into the JCR
     * repository this method sets the request attributes required by the OSGi
     * Http Service specification plus the {@link #REQUEST_ATTRIBUTE_RESOLVER}
     * attribute.
     *
     * @param request The HTTP request to be authenticated
     * @param response The HTTP response to send any response to in case of
     *            problems.
     * @return <code>true</code> if authentication succeeded and the request
     *         attributes are set. If <code>false</code> is returned the request
     *         is immediately terminated and no request attributes are set.
     */
    boolean handleSecurity(HttpServletRequest request,
            HttpServletResponse response);

}
