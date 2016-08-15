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
package org.apache.sling.auth.core.spi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The <code>AuthenticationFeedbackHandler</code> may be implemented by
 * {@link AuthenticationHandler} services to request being informed on the
 * success or failure of authentication.
 */
@ConsumerType
public interface AuthenticationFeedbackHandler {

    /**
     * Called if authentication failed with the credentials provided in the
     * <code>authInfo</code> map.
     * <p>
     * This method allows the handler to cleanup any state prepared while
     * handling the
     * {@link AuthenticationHandler#extractCredentials(HttpServletRequest, HttpServletResponse)
     * extractCredentials} method. Handlers are expected to not send a in this
     * method because the Sling Authenticator will proceed to select an
     * authentication handler whose
     * {@link AuthenticationHandler#requestCredentials(HttpServletRequest, HttpServletResponse)
     * requestCredentials} method will be called.
     * <p>
     * Implementations may also wish to set the
     * {@link AuthenticationHandler#FAILURE_REASON} request attribute to inform
     * interested parties (including its any
     * {@link AuthenticationHandler#requestCredentials(HttpServletRequest, HttpServletResponse)}
     * method about the reasons of failure to to authenticate.
     *
     * @param request The current request
     * @param response The current response
     * @param authInfo The {@link AuthenticationInfo} object used to
     *            authenticate the request.
     */
    void authenticationFailed(HttpServletRequest request,
            HttpServletResponse response, AuthenticationInfo authInfo);

    /**
     * Called if authentication succeeded with the credentials provided in the
     * <code>authInfo</code> map.
     * <p>
     * This method is called after successful login and impersonation handling
     * immediately before continuing with the request. The handler may choose to
     * send its own response or to just set some response header (e.g. adding a
     * Cookie) and return appropriately.
     *
     * @param request The current request
     * @param response The current response
     * @param authInfo The {@link AuthenticationInfo} object used to
     *            authenticate the request.
     * @return <code>true</code> if the handler sent back a response to the
     *         client and request processing should be terminated at this point.
     *         If <code>false</code> is returned, the request proceeds as
     *         authenticated.
     */
    boolean authenticationSucceeded(HttpServletRequest request,
            HttpServletResponse response, AuthenticationInfo authInfo);
}
