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

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.core.AuthUtil;

/**
 * The <code>AbstractAuthenticationHandler</code> implements the
 * <code>AuthenticationHandler</code> interface and extends the
 * {@link DefaultAuthenticationFeedbackHandler} providing some helper methods
 * which may be used by authentication handlers.
 *
 * @deprecated since Bundle 1.0.8; {@link AuthenticationHandler} implementations
 *             should extend {@link DefaultAuthenticationFeedbackHandler}
 *             directly and use the utility methods in the {@link AuthUtil}
 *             class.
 */
@Deprecated
public abstract class AbstractAuthenticationHandler extends DefaultAuthenticationFeedbackHandler implements
        AuthenticationHandler {

    /**
     * Returns the value of the named request attribute or parameter as a string
     * as follows:
     * <ol>
     * <li>If there is a request attribute of that name, which is a non-empty
     * string, it is returned.</li>If there is a non-empty request parameter of
     * that name, this parameter is returned.
     * <li>Otherwise the <code>defaultValue</code> is returned.
     *
     * @param request The request from which to return the attribute or request
     *            parameter
     * @param name The name of the attribute/parameter
     * @param defaultValue The default value to use if neither a non-empty
     *            string attribute or a non-empty parameter exists in the
     *            request.
     * @return The attribute, parameter or <code>defaultValue</code> as defined
     *         above.
     * @deprecated since Bundle 1.0.8, use
     *             {@link AuthUtil#getAttributeOrParameter(HttpServletRequest, String, String)}
     */
    @Deprecated
    public static String getAttributeOrParameter(final HttpServletRequest request, final String name,
            final String defaultValue) {
        return AuthUtil.getAttributeOrParameter(request, name, defaultValue);
    }

    /**
     * Returns any resource target to redirect to after successful
     * authentication. This method either returns a non-empty string or the
     * <code>defaultLoginResource</code> parameter. First the
     * <code>resource</code> request attribute is checked. If it is a non-empty
     * string, it is returned. Second the <code>resource</code> request
     * parameter is checked and returned if it is a non-empty string.
     *
     * @param request The request providing the attribute or parameter
     * @param defaultLoginResource The default login resource value
     * @return The non-empty redirection target or
     *         <code>defaultLoginResource</code>.
     * @deprecated since Bundle 1.0.8, use
     *             {@link AuthUtil#getLoginResource(HttpServletRequest, String)}
     */
    @Deprecated
    public static String getLoginResource(final HttpServletRequest request, String defaultLoginResource) {
        return AuthUtil.getLoginResource(request, defaultLoginResource);
    }

    /**
     * Ensures and returns the {@link Authenticator#LOGIN_RESOURCE} request
     * attribute is set to a non-null, non-empty string. If the attribute is not
     * currently set, this method sets it as follows:
     * <ol>
     * <li>If the {@link Authenticator#LOGIN_RESOURCE} request parameter is set
     * to a non-empty string, that parameter is set</li>
     * <li>Otherwise if the <code>defaultValue</code> is a non-empty string the
     * default value is used</li>
     * <li>Otherwise the attribute is set to "/"</li>
     * </ol>
     *
     * @param request The request to check for the resource attribute
     * @param defaultValue The default value to use if the attribute is not set
     *            and the request parameter is not set. This parameter is
     *            ignored if it is <code>null</code> or an empty string.
     * @return returns the value of resource request attribute
     * @since 1.0.2 (Bundle version 1.0.4)
     * @deprecated since Bundle 1.0.8, use
     *             {@link AuthUtil#setLoginResourceAttribute(HttpServletRequest, String)}
     */
    @Deprecated
    public static String setLoginResourceAttribute(final HttpServletRequest request, final String defaultValue) {
        return AuthUtil.setLoginResourceAttribute(request, defaultValue);
    }

    /**
     * Redirects to the given target path appending any parameters provided in
     * the parameter map.
     * <p>
     * This method implements the following functionality:
     * <ul>
     * <li>The target path is prefixed with the request's context path to ensure
     * proper redirection into the same web application. Therefore the
     * <code>target</code> path parameter must not be prefixed with the context
     * path.</li>
     * <li>If the <code>params</code> map does not contain a (non-
     * <code>null</code>) value for the {@link Authenticator#LOGIN_RESOURCE
     * resource} entry, such an entry is generated from the request URI and the
     * (optional) query string of the given <code>request</code>.</li>
     * <li>The parameters from the <code>params</code> map or at least a single
     * {@link Authenticator#LOGIN_RESOURCE resource} parameter are added to the
     * target path for the redirect. Each parameter value is encoded using the
     * <code>java.net.URLEncoder</code> with UTF-8 encoding to make it safe for
     * requests</li>
     * </ul>
     *
     * @param request The request object used to get the current request URI and
     *            request query string if the <code>params</code> map does not
     *            have the {@link Authenticator#LOGIN_RESOURCE resource}
     *            parameter set.
     * @param response The response used to send the redirect to the client.
     * @param target The target path to redirect the client to. This parameter
     *            must not be prefixed with the request's context path because
     *            this will be added by this method. If this parameter is not a
     *            valid target request as per the
     *            {@link #isRedirectValid(HttpServletRequest, String)} method
     *            the target is modified to be the root of the request's
     *            context.
     * @param params The map of parameters to be added to the target path. This
     *            may be <code>null</code>.
     * @throws IOException If an error occurs sending the redirect request
     * @throws IllegalStateException If the response was committed or if a
     *             partial URL is given and cannot be converted into a valid URL
     * @throws InternalError If the UTF-8 character encoding is not supported by
     *             the platform. This should not be caught, because it is a real
     *             problem if the encoding required by the specification is
     *             missing.
     * @since 1.0.2 (Bundle version 1.0.4)
     * @since 1.0.4 (bundle version 1.0.8) the target is validated with the
     *        {@link AuthUtil#isRedirectValid(HttpServletRequest, String)}
     *        method.
     * @deprecated since Bundle 1.0.8, use
     *             {@link AuthUtil#sendRedirect(HttpServletRequest, HttpServletResponse, String, Map)}
     */
    @Deprecated
    public static void sendRedirect(final HttpServletRequest request, final HttpServletResponse response,
            final String target, Map<String, String> params) throws IOException {
        AuthUtil.sendRedirect(request, response, request.getContextPath() + target, params);
    }

    /**
     * This method calls
     * {@link AuthUtil#isRedirectValid(HttpServletRequest, String)}.
     *
     * @deprecated This method has been introduced after Bundle release 1.0.6
     *             but has been replaced with
     *             {@link AuthUtil#isRedirectValid(HttpServletRequest, String)}.
     *             Use the latter method.
     */
    @Deprecated
    public static boolean isRedirectValid(final HttpServletRequest request, final String target) {
        return AuthUtil.isRedirectValid(request, target);
    }

    /**
     * Returns <code>true</code> if the the client just asks for validation of
     * submitted username/password credentials.
     * <p>
     * This implementation returns <code>true</code> if the request parameter
     * {@link #PAR_J_VALIDATE} is set to <code>true</code> (case-insensitve). If
     * the request parameter is not set or to any value other than
     * <code>true</code> this method returns <code>false</code>.
     *
     * @param request The request to provide the parameter to check
     * @return <code>true</code> if the {@link #PAR_J_VALIDATE} parameter is set
     *         to <code>true</code>.
     * @since 1.0.2 (Bundle version 1.0.4)
     * @deprecated since Bundle 1.0.8, use
     *             {@link AuthUtil#isValidateRequest(HttpServletRequest)}
     */
    @Deprecated
    public static boolean isValidateRequest(final HttpServletRequest request) {
        return AuthUtil.isValidateRequest(request);
    }

    /**
     * Sends a 200/OK response to a credential validation request.
     *
     * @param response The response object
     * @since 1.0.2 (Bundle version 1.0.4)
     * @deprecated since Bundle 1.0.8, use
     *             {@link AuthUtil#sendValid(HttpServletResponse)}
     */
    @Deprecated
    public static void sendValid(final HttpServletResponse response) {
        AuthUtil.sendValid(response);
    }

    /**
     * Sends a 403/FORBIDDEN response optionally stating the reason for this
     * response code in the {@link #X_REASON} header. The value for the
     * {@link #X_REASON} header is taken from
     * {@link AuthenticationHandler#FAILURE_REASON} request attribute if set.
     *
     * @param request The request object
     * @param response The response object
     * @since 1.0.2 (Bundle version 1.0.4)
     * @deprecated since Bundle 1.0.8, use
     *             {@link AuthUtil#sendInvalid(HttpServletRequest, HttpServletResponse)}
     */
    @Deprecated
    public static void sendInvalid(final HttpServletRequest request, final HttpServletResponse response) {
        AuthUtil.sendInvalid(request, response);
    }
}
