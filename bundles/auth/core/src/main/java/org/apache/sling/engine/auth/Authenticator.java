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
package org.apache.sling.engine.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>Authenticator</code> interface defines the service interface of the
 * authenticator used by the Sling engine. This service provides a method to
 * find an {@link AuthenticationHandler} and call its
 * {@link AuthenticationHandler#requestAuthentication(HttpServletRequest, HttpServletResponse)}
 * method.
 * <p>
 * This interface is not intended to be implemented by applications but may be
 * used to initiate the authentication process form a request processing servlet
 * or script.
 *
 * @since 2.0.4 This is version of the sling engine module where these classes where
 *              hosted before.
 * @deprecated use {@link org.apache.sling.auth.core.AuthenticationSupport} instead
 */
@Deprecated
public interface Authenticator {

    /**
     * Finds an {@link AuthenticationHandler} for the given request and call its
     * {@link AuthenticationHandler#requestAuthentication(HttpServletRequest, HttpServletResponse)}
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
    public void login(HttpServletRequest request, HttpServletResponse response);

}
