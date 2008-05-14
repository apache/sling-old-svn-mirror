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
package org.apache.sling.httpauth.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.sling.engine.auth.AuthenticationHandler;
import org.apache.sling.engine.auth.AuthenticationInfo;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>AuthorizationHeaderAuthenticationHandler</code> class implements
 * the authorization steps based on the Authorization header of the HTTP
 * request. This authenticator should eventually support both BASIC and DIGEST
 * authentication methods.
 * 
 * @scr.component immediate="false" label="%auth.http.name"
 *                description="%auth.http.description"
 * @scr.property name="service.description" value="HTTP Header Authentication Handler"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service
 */
public class AuthorizationHeaderAuthenticationHandler implements
        AuthenticationHandler {

    /**
     * @scr.property value="Sling (Development)"
     */
    public static final String PAR_REALM_NAME = "auth.http.realm";

    /**
     * The request parameter causing a 401/UNAUTHORIZED status to be sent back
     * in the {@link #authenticate(HttpServletRequest, HttpServletResponse)}
     * method if no credentials are present in the request (value is
     * "sling:authRequestLogin").
     * 
     * @see #authenticate(HttpServletRequest, HttpServletResponse)
     */
    static final String REQUEST_LOGIN_PARAMETER = "sling:authRequestLogin";

    /**
     * The special user, whose credentials are always ignored to enable logging
     * the user out (value is "__forced_logout_user__"). This user id is sent by
     * the {@link LoginServlet} to indicate the request to log out (only from
     * non-IE browsers).
     */
    static final String NOT_LOGGED_IN_USER = "__forced_logout_user__";

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private static final String DEFAULT_REALM = "Sling (Development)";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String realm = DEFAULT_REALM;

    public AuthorizationHeaderAuthenticationHandler() {
        log.info("AuthorizationHeaderAuthenticationHandler created");
    }

    // ----------- AuthenticationHandler interface ----------------------------

    /**
     * Extracts credential data from the request if at all contained. This check
     * is only based on the original request object, no URI translation has
     * taken place yet.
     * <p>
     * The method returns any of the following values : <table>
     * <tr>
     * <th>value
     * <th>description</tr>
     * <tr>
     * <td><code>null</code>
     * <td>no user details were contained in the request </tr>
     * <tr>
     * <td>{@link AuthenticationInfo#DOING_AUTH}
     * <td>the handler is in an ongoing authentication exchange with the
     * client. The request handling is terminated.
     * <tr>
     * <tr>
     * <td>valid credentials
     * <td>The user sent credentials.</tr>
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
    public AuthenticationInfo authenticate(HttpServletRequest request,
            HttpServletResponse response) {

        // extract credentials and return
        AuthenticationInfo info = this.extractAuthentication(request);
        if (info != null) {
            return info;
        }

        // no credentials, check whether the client wants to login
        if (request.getParameter(REQUEST_LOGIN_PARAMETER) != null) {
            try {
                requestAuthentication(request, response);
                return AuthenticationInfo.DOING_AUTH;
            } catch (IOException ioe) {
                log.error("authenticate: Failed requesting authentication", ioe);
            }
        }

        // no special header, so we will not authenticate here
        return null;
    }

    /**
     * Sends status <code>401</code> (Unauthorized) with a
     * <code>WWW-Authenticate</code> requesting standard HTTP header
     * authentication with the <code>Basic</code> scheme and the configured
     * realm name. If the response is already committed, an error message is
     * logged but the 401 status is not sent.
     * 
     * @param request The request object
     * @param response The response object to which to send the request
     * @return <code>true</code> is always returned by this handler
     * @throws IOException if an error occurrs sending back the response.
     */
    public boolean requestAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // if the response is already committed, we have a problem !!
        if (!response.isCommitted()) {

            response.setHeader(HEADER_WWW_AUTHENTICATE,
                AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"");

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

        } else {

            log.error("requestAuthentication: Response is committed, cannot request authentication");

        }

        return true;
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext componentContext) {
        String newRealm = (String) componentContext.getProperties().get(
            PAR_REALM_NAME);
        if (newRealm == null || newRealm.length() == 0) {
            newRealm = DEFAULT_REALM;
        }
        if (!newRealm.equals(this.realm)) {
            log.info("Setting new realm name {} (was {})", newRealm, this.realm);
            this.realm = newRealm;
        }
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Extract the Base64 authentication string from the request
     */
    protected AuthenticationInfo extractAuthentication(
            HttpServletRequest request) {

        // Return immediately if the header is missing
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader == null || authHeader.length() == 0) {

            // try to fall back to cookies
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    if (HEADER_AUTHORIZATION.equalsIgnoreCase(cookies[i].getName())) {
                        authHeader = cookies[i].getValue();
                        break;
                    }
                }
            }

            // If still no authentication, return null
            if (authHeader == null || authHeader.length() == 0) {
                return null;
            }
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

        SimpleCredentials creds;
        int colIdx = decoded.indexOf(':');
        if (colIdx < 0) {
            creds = new SimpleCredentials(decoded, new char[0]);
        } else {
            creds = new SimpleCredentials(decoded.substring(0, colIdx),
                decoded.substring(colIdx + 1).toCharArray());
        }

        if (NOT_LOGGED_IN_USER.equals(creds.getUserID())) {
            return null;
        }

        return new AuthenticationInfo(HttpServletRequest.BASIC_AUTH, creds);
    }
}