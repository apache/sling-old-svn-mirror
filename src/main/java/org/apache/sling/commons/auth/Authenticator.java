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
package org.apache.sling.commons.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>Authenticator</code> interface defines the service interface of the
 * authenticator used by the Sling engine. This service provides a method to
 * find an {@link org.apache.sling.commons.auth.spi.AuthenticationHandler
 * AuthenticationHandler} and call its
 * {@link org.apache.sling.commons.auth.spi.AuthenticationHandler#requestAuthentication(HttpServletRequest, HttpServletResponse)
 * requestAuthentication} or
 * {@link org.apache.sling.commons.auth.spi.AuthenticationHandler#dropAuthentication(HttpServletRequest, HttpServletResponse)
 * dropAuthentication} methods.
 * <p>
 * This service is used by applications which want to provide functionality to
 * their users to log into the application and log out from it.
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
 *         // probably no AuthenticationHandler available
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
 *         // handle the case of no Authenticator to logout with
 *     }
 * }
 * </pre>
 * <p>
 * This interface is not intended to be implemented by applications but may be
 * used to initiate the authentication process form a request processing servlet
 * or script.
 */
public interface Authenticator {

    /**
     * The name under which this service is registered.
     */
    static final String SERVICE_NAME = "org.apache.sling.commons.auth.Authenticator";

    /**
     * Name of the request attribute used by the
     * {@link #login(HttpServletRequest, HttpServletResponse)} method to select
     * an {@link org.apache.sling.commons.auth.spi.AuthenticationHandler} to
     * call. If this request attribute is not set or is the empty string, the
     * request path info ( <code>HttpServletRequest.getPathInfo()</code>) method
     * is used to get the path.
     * <p>
     * This request attribute can be used by frontend servlets/scripts which
     * call into {@link #login(HttpServletRequest, HttpServletResponse)} on
     * behalf of users.
     */
    static final String LOGIN_RESOURCE = "resource";

    /**
     * Finds an {@link org.apache.sling.commons.auth.spi.AuthenticationHandler}
     * for the given request and call its
     * {@link org.apache.sling.commons.auth.spi.AuthenticationHandler#requestAuthentication(HttpServletRequest, HttpServletResponse)}
     * method to initiate an authentication process with the client to login to
     * Sling.
     * <p>
     * This method must be called on an uncommitted response since the
     * implementation may want to reset the response to start the authentication
     * process with a clean response. If the response is already committed an
     * <code>IllegalStateException</code> is thrown.
     * <p>
     * After this method has finished, request processing should be terminated
     * and the response be considered committed and finished.
     *
     * @param request The object representing the client request.
     * @param response The object representing the response to the client.
     * @throws NoAuthenticationHandlerException If no authentication handler
     *             claims responsibility to authenticate the request.
     * @throws IllegalStateException If the response has already been committed.
     */
    void login(HttpServletRequest request, HttpServletResponse response);

    /**
     * Finds an {@link org.apache.sling.commons.auth.spi.AuthenticationHandler}
     * for the given request and call its
     * {@link org.apache.sling.commons.auth.spi.AuthenticationHandler#dropAuthentication(HttpServletRequest, HttpServletResponse)}
     * method to drop authentication credentials for the client to logout from
     * Sling.
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
