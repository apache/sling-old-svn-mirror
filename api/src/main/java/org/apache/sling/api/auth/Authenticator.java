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
package org.apache.sling.api.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>Authenticator</code> interface defines the service interface which
 * may be used by applications to enforce requests to be authenticated (see
 * {@link #login(HttpServletRequest, HttpServletResponse)}) or to end enforced
 * authentication (see {@link #logout(HttpServletRequest, HttpServletResponse)}
 * ). As such this service may be looked at as the functionality to enable
 * applications to log users in and out.
 * <p>
 * A very simple login script (using ESP here) could be implemented like this:
 *
 * <pre>
 * var auth = sling.getService(org.apache.sling.commons.auth.Authenticator);
 * if (auth != null) {
 *     try {
 *         auth.login(request, response);
 *         return; // we are done here
 *     } catch (e) {
 *         // probably NoAuthenticationHandler exception
 *     }
 * }
 * // Authenticator service is missing or no AuthenticationHandler
 * ... do whatever you want to for error handling ...
 * </pre>
 * <p>
 * Likewise implementing a logout script (ESP, too) is equally simple:
 *
 * <pre>
 * if (request.authType) {
 *     // not logged in at all, no need to logout
 * } else {
 *     var auth = sling.getService(org.apache.sling.commons.auth.Authenticator);
 *     if (auth != null) {
 *         auth.logout(request, response);
 *     } else {
 *         // handle the case of no Authenticator service to logout with
 *     }
 * }
 * </pre>
 * <p>
 * This interface is not intended to be implemented by applications but may be
 * used to initiate the authentication process form a request processing servlet
 * or script.
 *
 * @since 1.0 (Sling API Bundle 2.1.0)
 */
@ProviderType
public interface Authenticator {

    /**
     * The name under which this service is registered.
     */
    static final String SERVICE_NAME = Authenticator.class.getName();

    /**
     * Name of the request attribute which may be set by the application to
     * indicate to the {@link #login(HttpServletRequest, HttpServletResponse)}
     * method to which resource access should actually be authenticated. If this
     * request attribute is not set or is the empty string, the
     * {@link #login(HttpServletRequest, HttpServletResponse)} method uses the
     * request path info (<code>HttpServletRequest.getPathInfo()</code>) method
     * to find the resource to which to authenticate access.
     * <p>
     * This request attribute can be used by frontend servlets/scripts which
     * call into {@link #login(HttpServletRequest, HttpServletResponse)} on
     * behalf of users.
     */
    static final String LOGIN_RESOURCE = "resource";

    /**
     * Tries to login a request user for the current request.
     * <p>
     * To identify the resource to which access should be authenticated the
     * <code>{@link #LOGIN_RESOURCE resource}</code> request attribute is
     * considered. If the request attribute is not set the request path info (
     * <code>HttpServletRequest.getPathInfo()</code>) is used.
     * <p>
     * This method must be called on an uncommitted response since the
     * implementation may want to reset the response to start the authentication
     * process with a clean response. If the response is already committed an
     * <code>IllegalStateException</code> is thrown.
     * <p>
     * After this method has finished, request processing should be terminated
     * and the response be considered committed and finished unless the
     * {@link NoAuthenticationHandlerException} exception is thrown in which
     * case no response has been sent to the client.
     *
     * @param request The object representing the client request.
     * @param response The object representing the response to the client.
     * @throws NoAuthenticationHandlerException If the service cannot find a way
     *             to authenticate a request user.
     * @throws IllegalStateException If the response has already been committed.
     */
    void login(HttpServletRequest request, HttpServletResponse response);

    /**
     * Logs out if the current request is authenticated.
     * <p>
     * This method must be called on an uncommitted response since the
     * implementation may want to reset the response to restart the
     * authentication process with a clean response. If the response is already
     * committed an <code>IllegalStateException</code> is thrown.
     * <p>
     * After this method has finished, request processing should be terminated
     * and the response be considered committed and finished.
     *
     * @param request The object representing the client request.
     * @param response The object representing the response to the client.
     * @throws IllegalStateException If the response has already been committed.
     */
    void logout(HttpServletRequest request, HttpServletResponse response);
}
