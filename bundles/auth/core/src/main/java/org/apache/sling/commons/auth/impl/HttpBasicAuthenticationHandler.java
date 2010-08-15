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
package org.apache.sling.commons.auth.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.sling.commons.auth.spi.AuthenticationHandler;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.apache.sling.commons.auth.spi.DefaultAuthenticationFeedbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>HttpBasicAuthenticationHandler</code> class supports plain old HTTP
 * Basic authentication. While {@link #extractCredentials(HttpServletRequest)}
 * always accesses the header if called and if present, the
 * {@link #requestCredentials(HttpServletRequest, HttpServletResponse)} and
 * {@link #dropCredentials(HttpServletRequest, HttpServletResponse)} methods
 * must be explicitly enabled to send back a 401/UNAUTHORIZED reply to force the
 * client into HTTP Basic authentication.
 * <p>
 * Being able to just extract credentials but not actively request them provides
 * an easy way for tools (like cURL) or libraries (like Apache HttpCLient) to
 * preemptively authenticate with HTTP Basic authentication.
 */
public class HttpBasicAuthenticationHandler extends
        DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String realm;

    public HttpBasicAuthenticationHandler(final String realm) {
        this.realm = realm;
    }

    // ----------- AuthenticationHandler interface ----------------------------

    /**
     * Extracts credential data from the request if at all contained. This check
     * is only based on the original request object, no URI translation has
     * taken place yet.
     * <p>
     * The method returns any of the following values :
     * <table>
     * <tr>
     * <th>value
     * <th>description
     * </tr>
     * <tr>
     * <td><code>null</code>
     * <td>no user details were contained in the request
     * </tr>
     * <tr>
     * <td>{@link AuthenticationInfo#DOING_AUTH}
     * <td>the handler is in an ongoing authentication exchange with the client.
     * The request handling is terminated.
     * <tr>
     * <tr>
     * <td>valid credentials
     * <td>The user sent credentials.
     * </tr>
     * </table>
     * <p>
     * The method must not request credential information from the client, if
     * they are not found in the request.
     * <p>
     * Note : The implementation should pay special attention to the fact, that
     * the request may be for an included servlet, in which case the values for
     * some URI specific values are contained in javax.servlet.include.* request
     * attributes.
     *
     * @param request The request object containing the information for the
     *            authentication.
     * @param response The response object which may be used to send the
     *            information on the request failure to the user.
     * @return A valid Credentials instance identifying the request user,
     *         DOING_AUTH if the handler is in an authentication trasaction with
     *         the client or null if the request does not contain authentication
     *         information. In case of DOING_AUTH, the method must have sent a
     *         response indicating that fact to the client.
     */
    public AuthenticationInfo extractCredentials(HttpServletRequest request,
            HttpServletResponse response) {

        // extract credentials and return
        AuthenticationInfo info = this.extractCredentials(request);
        if (info != null) {
            return info;
        }

        // no credentials, check whether the client wants to login
        if (forceAuthentication(request, response)) {
            return AuthenticationInfo.DOING_AUTH;
        }

        // no special header, so we will not authenticate here
        return null;
    }

    /**
     * Sends back the form to log into the system.
     *
     * @param request The request object
     * @param response The response object to which to send the request
     * @return <code>true</code> is always returned by this handler
     */
    public boolean requestCredentials(HttpServletRequest request,
            HttpServletResponse response) {

        if (isLoginRequested(request, true)) {
            return sendUnauthorized(response);
        }

        return false;
    }

    /**
     * Sends a 401/UNATUHORIZED response if the request has an Authorization
     * header and if this handler is configured to actually send this response
     * in response to a request to drop the credentials.
     * <p>
     * Note, that sending a 401/UNAUTHORIZED response is generally the only save
     * means to remove HTTP Basic credentials from a browser's cache. Yet, the
     * nasty side-effect is that the browser's login form is displayed as a
     * reaction to the 401/UNAUTHORIZED response.
     */
    public void dropCredentials(HttpServletRequest request,
            HttpServletResponse response) {
        if (request.getHeader(HEADER_AUTHORIZATION) != null) {
            sendUnauthorized(response);
        }
    }

    /**
     * Returns true if the {@link #REQUEST_LOGIN_PARAMETER} parameter is set to
     * the value <code>Basic</code> thus requesting plain basic authentication.
     */
    private boolean isLoginRequested(HttpServletRequest request,
            boolean optionalLoginParameter) {
        final String reqLogin = request.getParameter(REQUEST_LOGIN_PARAMETER);
        if (reqLogin == null) {
            return optionalLoginParameter;
        }
        return "1".equals(reqLogin)
            || HttpServletRequest.BASIC_AUTH.equals(reqLogin);
    }

    /**
     * If the {@link #REQUEST_LOGIN_PARAMETER} parameter is set this method
     * sends status <code>401</code> (Unauthorized) with a
     * <code>WWW-Authenticate</code> requesting standard HTTP header
     * authentication with the <code>Basic</code> scheme and the configured
     * realm name. If the response is already committed, an error message is
     * logged but the 401 status is not sent.
     * <p>
     * <code>false</code> is returned if the request parameter is not set, if
     * the response is already committed or if an error occurred sending the
     * status response. The latter two situations are logged as errors.
     *
     * @param request The request object
     * @param response The response object to which to send the request
     * @return <code>true</code> if the 401/UNAUTHORIZED method has successfully
     *         been sent.
     */
    private boolean forceAuthentication(HttpServletRequest request,
            HttpServletResponse response) {

        // presume 401/UNAUTHORIZED has not been sent
        boolean authenticationForced = false;

        if (isLoginRequested(request, false)) {

            authenticationForced = sendUnauthorized(response);

        } else {

            log.debug(
                "forceAuthentication: Not forcing authentication because request parameter {} is not set",
                REQUEST_LOGIN_PARAMETER);

        }

        // true if 401/UNAUTHORIZED has been sent, false otherwise
        return authenticationForced;
    }

    /**
     * Sends status <code>401</code> (Unauthorized) with a
     * <code>WWW-Authenticate</code> requesting standard HTTP header
     * authentication with the <code>Basic</code> scheme and the configured
     * realm name.
     *
     * @param response The response object to which to send the request
     * @return <code>true</code> if the 401/UNAUTHORIZED method has successfully
     *         been sent.
     */
    private boolean sendUnauthorized(HttpServletResponse response) {

        if (response.isCommitted()) {

            log.error("sendUnauthorized: Cannot send 401/UNAUTHORIZED; response is already committed");

        } else {

            response.setHeader(HEADER_WWW_AUTHENTICATE,
                AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"");

            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                response.flushBuffer();
                return true;
            } catch (IOException ioe) {
                log.error("sendUnauthorized: Failed requesting authentication",
                    ioe);
            }
        }

        return false;
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Extract the Base64 authentication string from the request
     */
    protected AuthenticationInfo extractCredentials(HttpServletRequest request) {

        // Return immediately if the header is missing
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader == null || authHeader.length() == 0) {
            return null;
        }

        // Get the authType (Basic, Digest) and authInfo (user/password) from
        // the header
        authHeader = authHeader.trim();
        int blank = authHeader.indexOf(' ');
        if (blank <= 0) {
            return null;
        }
        String authType = authHeader.substring(0, blank);
        String authInfo = authHeader.substring(blank).trim();

        // Check whether authorization type matches
        if (!authType.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
            return null;
        }

        // Base64 decode and split on colon

        // we cannot use default base64, since we need iso encoding
        // (nb: ISO-8859-1 is required as per API spec to be available)
        String decoded;
        try {
            byte[] encoded = authInfo.getBytes("ISO-8859-1");
            byte[] bytes = Base64.decodeBase64(encoded);
            decoded = new String(bytes, "ISO-8859-1");
        } catch (UnsupportedEncodingException uee) {
            // unexpected
            log.error(
                "extractAuthentication: Cannot en/decode authentication info",
                uee);
            return null;
        }

        final int colIdx = decoded.indexOf(':');
        final String userId;
        final char[] password;
        if (colIdx < 0) {
            userId = decoded;
            password = new char[0];
        } else {
            userId = decoded.substring(0, colIdx);
            password = decoded.substring(colIdx + 1).toCharArray();
        }

        return new AuthenticationInfo(HttpServletRequest.BASIC_AUTH, userId,
            password);
    }
}