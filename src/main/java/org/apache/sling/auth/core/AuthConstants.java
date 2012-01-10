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

/**
 * The <code>AuthConstants</code> provides a collection of constants used to
 * configure and customize the Sling authentication infrastructure.
 * <p>
 * This class can neither be extended from nor can it be instantiated.
 *
 * @since 1.1 (bundle version 1.0.8)
 */
public final class AuthConstants {

    /**
     * The name of the request parameter indicating that the submitted username
     * and password should just be checked and a status code be set for success
     * (200/OK) or failure (403/FORBIDDEN).
     *
     * @see #isValidateRequest(HttpServletRequest)
     * @see #sendValid(HttpServletResponse)
     * @see #sendInvalid(HttpServletRequest, HttpServletResponse)
     */
    public static final String PAR_J_VALIDATE = "j_validate";

    /**
     * The name of the request header set by the
     * {@link #sendInvalid(HttpServletRequest, HttpServletResponse)} method if the provided
     * credentials cannot be used for login.
     * <p>
     * This header may be inspected by clients for a reason why the request
     * failed.
     *
     * @see #sendInvalid(HttpServletRequest, HttpServletResponse)
     */
    public static final String X_REASON = "X-Reason";

    /**
     * Service Registration property which may be set by an
     * {@link org.apache.sling.auth.core.spi.AuthenticationHandler} service to
     * indicate whether its
     * {@link org.apache.sling.auth.core.spi.AuthenticationHandler#requestCredentials(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * method supports non-browser requests (according to
     * {@link AuthUtil#isBrowserRequest(javax.servlet.http.HttpServletRequest)}
     * or not.
     * <p>
     * For backwards compatibility with existing
     * {@link org.apache.sling.auth.core.spi.AuthenticationHandler} services the
     * default assumption in the absence of this property is that all requests
     * are supported.
     * <p>
     * If this property is set to <code>true</code> or <code>yes</code>
     * (case-insensitive check) the handler is not called for requests assumed
     * to be sent from non-browser clients. Any other value of this property
     * indicates support for non-browser requests by the handler.
     * <p>
     * Note that this property only influences whether the
     * <code>requestCredentials</code> method is called or not. The
     * <code>extractCredentials</code> and <code>dropCredentials</code> are
     * called regardless of this property.
     */
    public static final String AUTH_HANDLER_BROWSER_ONLY = "sling.auth.browser-only";

    /**
     * The topic for the OSGi event which is sent when a user has logged in successfully.
     * The event contains at least the {@link org.apache.sling.api.SlingConstants#PROPERTY_USERID},
     * and {@link #PROPERTY_AUTH_HANDLER_CLASS} properties.
     */
    public static final String TOPIC_LOGIN = "org/apache/sling/auth/core/Authenticator/LOGIN";

    /**
     * The topic for the OSGi event which is sent when a user has logged out.
     * The event contains at least the {@link org.apache.sling.api.SlingConstants#PROPERTY_USERID}
     * property.
     */
    public static final String TOPIC_LOGOUT = "org/apache/sling/auth/core/Authenticator/LOGOUT";

    /**
     * The name of the event property holding the authentication handler class name.
     */
    public static final String PROPERTY_AUTH_HANDLER_CLASS = "authHandlerClassName";

    private AuthConstants() {
    }

}
