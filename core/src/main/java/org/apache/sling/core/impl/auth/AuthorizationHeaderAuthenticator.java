/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.auth.AuthenticationHandler;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>AuthorizationHeaderAuthenticator</code> class implements the
 * authorization steps based on the Authorization header of the HTTP request.
 * This authenticator should eventually support both BASIC and DIGEST
 * authentication methods.
 * 
 * @scr.component immediate="false" label="%auth.http.name"
 *                description="%auth.http.description"
 * @scr.property name="service.description"
 *          value="HTTP Header Authentication Handler"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service
 */
public class AuthorizationHeaderAuthenticator implements AuthenticationHandler {

    /**
     * @scr.property value="Sling (Development)"
     */
    public static final String PAR_REALM_NAME = "auth.http.realm";

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(AuthorizationHeaderAuthenticator.class);

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private static final String DEFAULT_REALM = "Sling (Development)";

    private String realm = DEFAULT_REALM;

    public AuthorizationHeaderAuthenticator() {
        log.info("AuthorizationHeaderAuthenticator created");
    }

    // ----------- AuthenticationHandler interface ----------------------------

    public boolean handles(ComponentRequest request) {
        return true;
    }

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
     * <td>{@link #DOING_AUTH}
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
    public Credentials authenticate(ComponentRequest request,
            ComponentResponse response) {
        return extractAuthentication(request);
    }

    /**
     * Sends status <code>401</code> (Unauthorized) with a
     * <code>WWW-Authenticate</code> requesting standard HTTP header
     * authentication with the <code>Basic</code> scheme and the configured
     * realm name.
     * <p>
     * Returns <code>true</code> if the response could successfully be sent to
     * the client. Otherwise <code>false</code> is returned.
     * 
     * @param request The request object
     * @param response The response object to which to send the request
     * @param addInfo Additional information string from the configuration. This
     *            may for example be used as the realm name for HTTP header
     *            authentication. TODO configuration
     * @return true if the information could be requested or false, if the
     *         request should fail with the appropriate error status
     */
    public boolean requestAuthentication(ComponentRequest request,
            ComponentResponse response) {

        // if the response is already committed, we have a problem !!
        if (response.isCommitted()) {
            log.warn("requestAuthentication: response already committed");
            return false;
        }

        response.setHeader(HEADER_WWW_AUTHENTICATE,
            AUTHENTICATION_SCHEME_BASIC + " realm=\"" + realm + "\"");

        try {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return true;
        } catch (IOException ioe) {
            log.info("requestAuthentication: Cannot send the error: {0}",
                ioe.toString());
        }

        // got a problem with sending the error
        return false;
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext componentContext) {
        String newRealm = (String) componentContext.getProperties().get(
            PAR_REALM_NAME);
        if (newRealm == null || newRealm.length() == 0) {
            newRealm = DEFAULT_REALM;
        }
        if (!newRealm.equals(realm)) {
            log.info("Setting new realm name {} (was {})", newRealm, realm);
            realm = newRealm;
        }
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Extract the Base64 authentication string from the request
     */
    protected Credentials extractAuthentication(ComponentRequest request) {

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

        int colIdx = decoded.indexOf(':');
        if (colIdx < 0) {
            return new SimpleCredentials(decoded, new char[0]);
        }

        return new SimpleCredentials(decoded.substring(0, colIdx),
            decoded.substring(colIdx + 1).toCharArray());
    }
}