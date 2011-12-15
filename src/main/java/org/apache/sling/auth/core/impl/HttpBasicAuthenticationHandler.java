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
package org.apache.sling.auth.core.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
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
class HttpBasicAuthenticationHandler extends
        DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The realm to send back with the 401 response */
    private final String realm;

    /**
     * Whether this authentication handler is fully enabled and sends back 401
     * responses from the
     * {@link #requestCredentials(HttpServletRequest, HttpServletResponse)} and
     * {@link #dropCredentials(HttpServletRequest, HttpServletResponse)}
     * methods.
     */
    private final boolean fullSupport;

    HttpBasicAuthenticationHandler(final String realm,
            final boolean fullSupport) {
        this.realm = realm;
        this.fullSupport = fullSupport;
    }

    // ----------- AuthenticationHandler interface ----------------------------

    /**
     * Returns the credential present within in an HTTP Basic authentication
     * header or <code>null</code> if no credentials are provided and the
     * {@link AuthenticationHandler#REQUEST_LOGIN_PARAMETER} is neither set as a
     * request parameter nor as a request attribute.
     * <p>
     * If the {@link AuthenticationHandler#REQUEST_LOGIN_PARAMETER} is set as a
     * request parameter or request attribute, a 401 response is sent to the
     * client and the method returns {@link AuthenticationInfo#DOING_AUTH} to
     * indicate that the handler has started its own credentials requesting.
     *
     * @param request The request object containing the information for the
     *            authentication.
     * @param response The response object which may be used to send the
     *            information on the request failure to the user.
     * @return A valid Credentials instance identifying the request user,
     *         DOING_AUTH if the handler is in an authentication transaction with
     *         the client or null if the request does not contain authentication
     *         information. In case of DOING_AUTH, the method has sent back a
     *         401 requesting the client to provide credentials.
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
     * Called by the SlingAuthenticator.login method in case no other
     * authentication handler was willing to request credentials from the
     * client. In this case this HTTP Basic authentication handler will send
     * back a {@link #sendUnauthorized(HttpServletResponse) 401 response} to
     * request HTTP Basic authentication from the client if full support has
     * been configured in the
     * {@link #HttpBasicAuthenticationHandler(String, boolean) constructor}
     *
     * @param request The request object
     * @param response The response object to which to send the request
     * @return <code>true</code> if full support is enabled and the 401 response
     *         could be sent. If full support is not enabled <code>false</code>
     *         is always returned.
     */
    public boolean requestCredentials(HttpServletRequest request,
            HttpServletResponse response) {
        return fullSupport ? sendUnauthorized(response) : false;
    }

    /**
     * Sends a 401/UNAUTHORIZED response if the request has an Authorization
     * header and if this handler is configured to actually send this response
     * in response to a request to drop the credentials; that is if full support
     * has been enabled in the
     * {@link #HttpBasicAuthenticationHandler(String, boolean) constructor}.
     * <p>
     * Note, that sending a 401/UNAUTHORIZED response is generally the only save
     * means to remove HTTP Basic credentials from a browser's cache. Yet, the
     * nasty side-effect is that the browser's login form is displayed as a
     * reaction to the 401/UNAUTHORIZED response.
     */
    public void dropCredentials(HttpServletRequest request,
            HttpServletResponse response) {
        if (fullSupport && request.getHeader(HEADER_AUTHORIZATION) != null) {
            sendUnauthorized(response);
        }
    }

    /**
     * Called if the credentials extracted by the
     * {@link #extractCredentials(HttpServletRequest, HttpServletResponse)}
     * method are not valid and sends back a 401/UNAUTHORIZED response
     * requesting the credentials again.
     * <p>
     * The only way to get a browser (or a client in general) into forgetting
     * the current credentials and sending different credentials is sending back
     * such a response. Otherwise the browser sends the same credentials over
     * and over again.
     * <p>
     * The assumption of this method unconditionally sending back the
     * 401/UNAUTHORIZED response is that this method here is only called if the
     * request actually provided invalid HTTP Basic credentials.
     * <p>
     * If the request is a
     * {@link AuthUtil#isValidateRequest(HttpServletRequest) validation request}
     * this method actually does nothing to allow for the expected 403/FORBIDDEN
     * response to be sent.
     */
    @Override
    public void authenticationFailed(HttpServletRequest request, HttpServletResponse response,
            AuthenticationInfo authInfo) {
        if (!AuthUtil.isValidateRequest(request)) {
            sendUnauthorized(response);
        }
    }

    /**
     * Returns true if the {@link #REQUEST_LOGIN_PARAMETER} parameter or request
     * attribute is set to any non-<code>null</code> value.
     * <p>
     * This method always returns <code>true</code> if the parameter or request
     * attribute is set regardless of its value because the client indicated it
     * wanted to login but no authentication handler was willing to actually
     * handle this request. So as a last fallback this handler request HTTP
     * Basic Credentials.
     *
     * @param request The request object providing the parameter or attribute.
     * @return <code>true</code> if the
     *         {@link AuthenticationHandler#REQUEST_LOGIN_PARAMETER} parameter
     *         or attribute is set to any value.
     */
    private boolean isLoginRequested(HttpServletRequest request) {
        return AuthUtil.getAttributeOrParameter(request, REQUEST_LOGIN_PARAMETER, null) != null;
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

        if (isLoginRequested(request)) {

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
     *         been sent and the response has been committed.
     */
    boolean sendUnauthorized(HttpServletResponse response) {

        if (response.isCommitted()) {

            log.error("sendUnauthorized: Cannot send 401/UNAUTHORIZED; response is already committed");

        } else {

            response.resetBuffer();

            /*
             * TODO: Check whether we have to redirect
             * If this is a GET request not targeted at the registration path
             * for which this handler is selected we have to redirect to the
             * registration path using either the provided resource attribute
             * or parameter or the current URL as the "resource" parameter
             * for the redirect and also setting the "sling:authRequestLogin"
             * parameter to "BASIC" to get the 401 response for the registration
             * path and redirect back to actual path afterwards.
             */

            // just set the status because this may be called as part of an
            // error handler in which case sendError would result in an error
            // handler loop and thus be ignored.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HEADER_WWW_AUTHENTICATE,
                AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"");

            try {
                response.flushBuffer();
                return true;
            } catch (IOException ioe) {
                log.error("sendUnauthorized: Failed requesting authentication",
                    ioe);
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "HTTP Basic Authentication Handler ("
            + (fullSupport ? "enabled" : "preemptive") + ")";
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