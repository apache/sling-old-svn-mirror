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

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.AuthUtil;
import org.slf4j.LoggerFactory;

public class DefaultAuthenticationFeedbackHandler implements
        AuthenticationFeedbackHandler {

    /**
     * Handles an optional request for a redirect after successful
     * authentication and <code>true</code> if the request has been redirected.
     * <p>
     * This method checks {@link AuthenticationSupport#REDIRECT_PARAMETER}
     * request parameter for the redirect target. This parameter is handled
     * as follows:
     * <ul>
     * <li>If the parameter does not exist, the method does not redirect and
     * <code>false</code> is returned.</li>
     * <li>If the parameter is the string <code>true</code> or is empty, a
     *  redirect response to the request URI (<code>HttpServletRequest.getRequestURI()</code>)
     *  is sent and <code>true</code> is returned.</li>
     * <li>If the parameter is a relative path, the path is made absolute
     *  by resolving it relative to the request URI
     *  (<code>HttpServletRequest.getRequestURI()</code>). The resulting
     *  target is validated with the
     *  {@link AbstractAuthenticationHandler#isRedirectValid(HttpServletRequest, String)}
     *  method. If valid a redirect to that target is sent back and <code>true</code>
     *  is returned. Otherwise a redirect to the servlet context root is
     *  sent back and <code>true</code> is returned.</li>
     * <li>If the parameter is an absolute path it is validated with the
     *  {@link AbstractAuthenticationHandler#isRedirectValid(HttpServletRequest, String)}
     *  method. If valid a redirect to that path is sent back and <code>true</code>
     *  is returned. Otherwise a redirect to the servlet context root is
     *  sent back and <code>true</code> is returned.</li>
     * <p>
     * If sending the redirect response fails due to some IO problems, the
     * request is still terminated but an error message is logged indicating the
     * problem.
     *
     * @return <code>true</code> if redirect was requested. Otherwise
     *         <code>false</code> is returned. Note, that <code>true</code> is
     *         returned regardless of whether sending the redirect response
     *         succeeded or not.
     *
     * @since 1.0.4 (bundle version 1.0.8) the target is validated with the
     *        {@link AbstractAuthenticationHandler#isRedirectValid(HttpServletRequest, String)}
     *        method.
     */
    public static boolean handleRedirect(final HttpServletRequest request,
            final HttpServletResponse response) {

        final String redirect = getValidatedRedirectTarget(request);
        if (redirect != null) {
            // and redirect ensuring the response is sent to the client
            try {
                response.sendRedirect(redirect);
            } catch (Exception e) {
                // expected: IOException and IllegalStateException
                LoggerFactory.getLogger(
                    DefaultAuthenticationFeedbackHandler.class).error(
                    "handleRedirect: Failed to send redirect to " + redirect
                        + ", aborting request without redirect", e);
            }

            // consider the request done
            return true;
        }

        // no redirect requested
        return false;
    }

    private static String getValidatedRedirectTarget(
            final HttpServletRequest request) {
        String redirect = request.getParameter(AuthenticationSupport.REDIRECT_PARAMETER);
        if (redirect == null) {
            return null;
        }

        // redirect to the same path
        if ("true".equalsIgnoreCase(redirect) || redirect.length() == 0) {
            return request.getRequestURI();
        }

        // redirect relative to the current request (make absolute)
        if (!redirect.startsWith("/") && !redirect.contains("://")) {
            String path = request.getRequestURI();
            path = path.substring(request.getContextPath().length());
            int lastSlash = path.lastIndexOf('/');
            path = (lastSlash > 0) ? path.substring(0, lastSlash + 1) : path;
            redirect = path.concat(redirect);
            redirect = ResourceUtil.normalize(redirect);
        }

        // prepend context path if necessary
        if (redirect.startsWith("/") && !redirect.startsWith(request.getContextPath())) {
            redirect = request.getContextPath().concat(redirect);
        }

        // absolute target (in the servlet context)
        if (!AuthUtil.isRedirectValid(request, redirect)) {
            LoggerFactory.getLogger(DefaultAuthenticationFeedbackHandler.class).error(
                "handleRedirect: Redirect target '{}' is invalid, redirecting to '/'",
                redirect);
            redirect = "/";
        }

        return redirect;
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
