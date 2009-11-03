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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>AuthenticationHandler</code> interface defines the service API used
 * by the authentication implementation to support plugin various ways of
 * extracting credentials from the request.
 * <p>
 * Authentication handlers implementing this interface should still be
 * registered with the {@link AuthenticationHandler#SERVICE_NAME authentication
 * handler service name}. Internally, the
 * {@link Authenticator#logout(HttpServletRequest, HttpServletResponse)} method
 * will identify the authentication handler appropriately.
 *
 * @since 2.1
 */
public interface AuthenticationHandler2 extends AuthenticationHandler {

    /**
     * Drops any credential and authentication details from the request and asks
     * the client to do the same.
     *
     * @param request The request object.
     * @param response The response object to which to send the request.
     * @throws IOException If an error occurrs asking the client to drop any
     *             authentication traces.
     * @since 2.1
     */
    void dropAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

}
