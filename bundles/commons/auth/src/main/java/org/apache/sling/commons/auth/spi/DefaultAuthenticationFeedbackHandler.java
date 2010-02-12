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
package org.apache.sling.commons.auth.spi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.auth.AuthenticationSupport;
import org.slf4j.LoggerFactory;

public class DefaultAuthenticationFeedbackHandler implements
        AuthenticationFeedbackHandler {

    /**
     * Handles an optional request for a redirect after successful
     * authentication and <code>true</code> if the request has been redirected.
     * <p>
     * If sending the redirect response fails due to some IO problems, the
     * request is still terminated but an error message is logged indicating the
     * problem.
     *
     * @return <code>true</code> if redirect was requested. Otherwise
     *         <code>false</code> is returned. Note, that <code>true</code> is
     *         returned regardless of whether sending the redirect response
     *         succeeded or not.
     */
    public static boolean handleRedirect(final HttpServletRequest request,
            final HttpServletResponse response) {

        final String redirect = request.getParameter(AuthenticationSupport.REDIRECT_PARAMETER);
        if (redirect != null) {

            // find the redirect target
            final String target;
            if ("true".equalsIgnoreCase(redirect) || redirect.length() == 0) {
                // redirect to the same path
                target = request.getRequestURI();

            } else if (redirect.startsWith("/")) {
                // absolute target (in the servlet context)
                target = request.getContextPath() + redirect;

            } else {
                // redirect relative to the current request
                target = redirect;

            }

            // and redirect ensuring the response is sent to the client
            try {
                response.sendRedirect(target);
            } catch (Exception e) {
                // expected: IOException and IllegalStateException
                LoggerFactory.getLogger(
                    DefaultAuthenticationFeedbackHandler.class).error(
                    "handleRedirect: Failed to send redirect to " + target
                        + ", aborting request without redirect", e);
            }

            // consider the request done
            return true;
        }

        // no redirect requested
        return false;
    }

    /**
     * This default implementation does nothing.
     * <p>
     * Extensions of this class may overwrite to cleanup any internal state.
     */
    public void authenticationFailed(HttpServletRequest request,
            HttpServletResponse response, AuthenticationInfo authInfo) {
    }

    /**
     * This default implementation calls the
     * {@link #handleRedirect(HttpServletRequest, HttpServletResponse)} method
     * to optionally redirect the request after successful authentication.
     * <p>
     * Extensions of this class may overwrite this method to perform additional
     * cleanup etc.
     *
     * @return the result of calling the
     *            {@link #handleRedirect(HttpServletRequest, HttpServletResponse)}
     *            method.
     */
    public boolean authenticationSucceeded(HttpServletRequest request,
            HttpServletResponse response, AuthenticationInfo authInfo) {
        return handleRedirect(request, response);
    }

}
