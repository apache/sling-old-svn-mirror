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
package org.apache.sling.engine.impl.auth;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.engine.CoreConstants;
import org.apache.sling.engine.auth.AuthenticationHandler;
import org.apache.sling.engine.auth.AuthenticationInfo;
import org.apache.sling.jcr.api.TooManySessionsException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingAuthenticator</code> class is the default implementation
 * for handling authentication. This class supports :
 * <ul>
 * <li>Support for login sessions where session ids are exchanged with cookies
 * <li>Support for multiple authentication handlers, which must implement the
 * {@link AuthenticationHandler} interface.
 * <li>Use of different handlers depending on the request URL. </ul
 * <p>
 * Currently this class does not support multiple handlers for any one request
 * URL.
 * <p>
 * Clients of this class use {@link #authenticate} method to create a
 * {@link AuthenticationInfo} for the handling of the request. This method uses any of the
 * handlers to extract the user information from the request. Next an object is
 * created for this user information. If no user information is contained in the
 * request (according to the handler), the anonymous info is used.
 * <p>
 *
 * @scr.component label="%auth.name" description="%auth.description" ds="false"
 * @scr.property name="service.description" value="Sling Authenticator"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 */
public class SlingAuthenticator implements ManagedService {

    /**
     * The name of the request attribute containing the AuthenticationHandler
     * which authenticated the current request. If the request is authenticated
     * through a session, this is the handler, which iinitially authenticated
     * the user.
     */
    public static final String REQUEST_ATTRIBUTE_HANDLER = "org.apache.sling.core.impl.auth.authentication_handler";

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(SlingAuthenticator.class);

    /**
     * @scr.property valueRef="DEFAULT_IMPERSONATION_COOKIE"
     */
    public static final String PAR_IMPERSONATION_COOKIE_NAME = "auth.sudo.cookie";

    /**
     * @scr.property valueRef="DEFAULT_IMPERSONATION_PARAMETER"
     */
    public static final String PAR_IMPERSONATION_PAR_NAME = "auth.sudo.parameter";

    /**
     * @scr.property valueRef="DEFAULT_ANONYMOUS_ALLOWED" type="Boolean"
     */
    public static final String PAR_ANONYMOUS_ALLOWED = "auth.annonymous";

    /** The default impersonation parameter name */
    private static final String DEFAULT_IMPERSONATION_PARAMETER = "sudo";

    /** The default impersonation cookie name */
    private static final String DEFAULT_IMPERSONATION_COOKIE = "sling.sudo";

    /** The default value for allowing anonymous access */
    private static final boolean DEFAULT_ANONYMOUS_ALLOWED = true;

    private final ServiceTracker repositoryTracker;

    private final ServiceTracker authHandlerTracker;

    private int authHandlerTrackerCount;

    private AuthenticationHandler[] authHandlerCache;

    /** The name of the impersonation parameter */
    private String sudoParameterName;

    /** The name of the impersonation cookie */
    private String sudoCookieName;

    /** Cache control flag */
    private boolean cacheControl;

    /** Whether access without credentials is allowed */
    boolean anonymousAllowed;

    /**
     * The list of packages from the configuration file. This list is checked
     * for each request. The handler of the first package match is used for the
     * authentication.
     */
    // private AuthPackage[] packages;
    /**
     * The number of {@link AuthPackage} elements in the {@link #packages} list.
     */
    // private int numPackages;
    private ServiceRegistration registration;

    public SlingAuthenticator(BundleContext bundleContext) {
        repositoryTracker = new ServiceTracker(bundleContext,
            Repository.class.getName(), null);
        repositoryTracker.open();

        authHandlerTracker = new ServiceTracker(bundleContext,
            AuthenticationHandler.class.getName(), null);
        authHandlerTracker.open();
        authHandlerTrackerCount = -1;
        authHandlerCache = null;

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION, "Sling Request Authenticator");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        registration = bundleContext.registerService(
            ManagedService.class.getName(), this, props);
    }

    public void dispose() {
        registration.unregister();
        authHandlerTracker.close();
        repositoryTracker.close();
    }

    /**
     * Checks the authentication contained in the request. This check is only
     * based on the original request object, no URI translation has taken place
     * yet.
     * <p>
     *
     * @param req The request object containing the information for the
     *            authentication.
     * @param res The response object which may be used to send the information
     *            on the request failure to the user.
     */
    public boolean authenticate(HttpServletRequest req, HttpServletResponse res)
            throws MissingRepositoryException {

        // 0. Nothing to do, if the session is also in the request
        // this might be the case if the request is handled as a result
        // of a servlet container include inside another Sling request
        Object sessionAttr = req.getAttribute(CoreConstants.SESSION);
        if (sessionAttr instanceof Session) {
            log.debug("authenticate: Request already authenticated, nothing to do");
            return true;
        } else if (sessionAttr != null) {
            // warn and remove existing non-session
            log.warn(
                "authenticate: Overwriting existing Session attribute ({})",
                sessionAttr);
            req.removeAttribute(CoreConstants.SESSION);
        }

        // 1. Ask all authentication handlers to try to extract credentials
        AuthenticationInfo authInfo = getAuthenticationInfo(req, res);

        // 3. Check Credentials
        if (authInfo == AuthenticationInfo.DOING_AUTH) {

            log.debug("authenticate: ongoing authentication in the handler");
            return false;

        } else if (authInfo == null) {

            log.debug("authenticate: no credentials in the request, anonymous");
            return getAnonymousSession(req, res);

        } else {
            // try to connect
            try {
                log.debug("authenticate: credentials, trying to get a session");
                Session session = getRepository().login(
                    authInfo.getCredentials(), null);

                // handle impersonation
                session = handleImpersonation(req, res, session);
                setAttributes(session, authInfo.getAuthType(), req);

                return true;

            } catch (RepositoryException re) {

                handleLoginFailure(req, res, re);

            }

            // end request
            return false;
        }
    }

    /**
     * Requests authentication information from the client. Returns
     * <code>true</code> if the information has been requested and request
     * processing can be terminated. Otherwise the request information could not
     * be requested and the request should be terminated with a 40x (Forbidden)
     * response.
     * <p>
     * Any response sent by the handler is also handled by the error handler
     * infrastructure.
     *
     * @param request The request object
     * @param response The response object to which to send the request
     */
    public void requestAuthentication(HttpServletRequest request,
            HttpServletResponse response) {

        AuthenticationHandler[] handlers = getAuthenticationHandlers();
        boolean done = false;
        for (int i = 0; !done && i < handlers.length; i++) {
            log.debug(
                "requestAuthentication: requesting authentication using handler: {0}",
                handlers[i]);

            try {
                done = handlers[i].requestAuthentication(request, response);
            } catch (IOException ioe) {
                log.error(
                    "requestAuthentication: Failed sending authentication request through handler "
                        + handlers[i] + ", access forbidden", ioe);
                done = true;
            }
        }

        if ( !done ) {
            // no handler could send an authentication request, fail with FORBIDDEN
            log.info("requestAuthentication: No handler for request, sending FORBIDDEN");
            sendFailure(response);
        }
    }

    // ----------- ManagedService interface -----------------------------------

    @SuppressWarnings("unchecked")
    public void updated(Dictionary properties) {

        if (properties == null) {
            properties = new Hashtable<String, Object>();
        }

        String newCookie = (String) properties.get(PAR_IMPERSONATION_COOKIE_NAME);
        if (newCookie == null || newCookie.length() == 0) {
            newCookie = DEFAULT_IMPERSONATION_COOKIE;
        }
        if (!newCookie.equals(this.sudoCookieName)) {
            log.info("Setting new cookie name for impersonation {} (was {})",
                newCookie, this.sudoCookieName);
            this.sudoCookieName = newCookie;
        }

        String newPar = (String) properties.get(PAR_IMPERSONATION_PAR_NAME);
        if (newPar == null || newPar.length() == 0) {
            newPar = DEFAULT_IMPERSONATION_PARAMETER;
        }
        if (!newPar.equals(this.sudoParameterName)) {
            log.info(
                "Setting new parameter name for impersonation {} (was {})",
                newPar, this.sudoParameterName);
            this.sudoParameterName = newPar;
        }

        Object flag = properties.get(PAR_ANONYMOUS_ALLOWED);
        if (flag instanceof Boolean) {
            this.anonymousAllowed = ((Boolean) flag).booleanValue();
        } else {
            this.anonymousAllowed = DEFAULT_ANONYMOUS_ALLOWED;
        }
    }

    // ---------- internal ----------------------------------------------------

    private Repository getRepository() throws MissingRepositoryException {
        final Repository repo = (Repository) repositoryTracker.getService();
        if (repo == null) {
            throw new MissingRepositoryException("No Repository available to "
                + getClass().getSimpleName() + ", cannot authenticate");
        }
        return repo;
    }

    private AuthenticationHandler[] getAuthenticationHandlers() {
        if (authHandlerCache == null
            || authHandlerTrackerCount < authHandlerTracker.getTrackingCount()) {
            Object[] services = authHandlerTracker.getServices();
            AuthenticationHandler[] ac = new AuthenticationHandler[services == null ? 0 : services.length];
            for (int i = 0; i < ac.length; i++) {
                ac[i] = (AuthenticationHandler) services[i];
            }
            authHandlerCache = ac;
            authHandlerTrackerCount = authHandlerTracker.getTrackingCount();
        }
        return authHandlerCache;
    }

    private AuthenticationInfo getAuthenticationInfo(
            HttpServletRequest request, HttpServletResponse response) {
        AuthenticationHandler[] local = getAuthenticationHandlers();
        for (int i = 0; i < local.length; i++) {
            AuthenticationInfo authInfo = local[i].authenticate(request,
                response);
            if (authInfo != null) {
                return authInfo;
            }
        }

        // no handler found for the request ....
        log.debug("getCredentials: no handler could extract credentials");
        return null;
    }

    // TODO
    private boolean getAnonymousSession(HttpServletRequest req,
            HttpServletResponse res) throws MissingRepositoryException {
        // login anonymously, log the exact cause in case of failure
        if (this.anonymousAllowed) {
            try {
                Session session = getRepository().login();
                setAttributes(session, null, req);
                return true;
            } catch (RepositoryException re) {
                // cannot login > fail login, do not try to authenticate
                handleLoginFailure(req, res, re);
                return false;
            }
        }
        
        // request authentication now, and fail if not possible
        log.debug("getAnonymousSession: Anonymous access not allowed by configuration");
        requestAuthentication(req, res);

        // fallback to no session
        return false;
    }

    private void handleLoginFailure(HttpServletRequest request,
            HttpServletResponse response, RepositoryException reason) {
        
        if (reason instanceof TooManySessionsException) {

            // to many users, send a 503 Service Unavailable
            log.info("authenticate: Too many sessions for user: {}",
                reason.getMessage());

            try {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SlingAuthenticator: Too Many Users");
            } catch (IOException ioe) {
                log.error("authenticate: Cannot send status 503 to client", ioe);
            }

        } else if (reason instanceof LoginException) {

            // request authentication information and send 403 (Forbidden)
            // if no handler can request authentication information.
            log.info("authenticate: Unable to authenticate: {}",
                reason.getMessage());
            requestAuthentication(request, response);

        } else {

            // general problem, send a 500 Internal Server Error
            log.error("authenticate: Unable to authenticate", reason);

            try {
                response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "SlingAuthenticator: data access error, reason=" + reason.getClass().getSimpleName());
            } catch (IOException ioe) {
                log.error("authenticate: Cannot send status 500 to client", ioe);
            }
        }
        
    }
    
    // TODO
    private void sendFailure(HttpServletResponse res) {
        try {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
        } catch (IOException ioe) {
            log.error("Cannot send error " + HttpServletResponse.SC_FORBIDDEN
                + " code", ioe);
        }
    }

    /**
     * Sets the request attributes required by the OSGi HttpContext interface
     * specification for the <code>handleSecurity</code> method. In addition
     * the {@link SlingHttpContext#SESSION} request attribute is set with the
     * JCR Session.
     */
    private void setAttributes(Session session, String authType,
            HttpServletRequest request) {

        request.setAttribute(HttpContext.REMOTE_USER, session.getUserID());
        request.setAttribute(HttpContext.AUTHENTICATION_TYPE, authType);
        request.setAttribute(CoreConstants.SESSION, session);

        log.debug("Session stored as request attribute: user={}, workspace={}",
                session.getUserID(), session.getWorkspace().getName());
    }

    /**
     * Sends the session cookie for the name session with the given age in
     * seconds. This sends a Version 1 cookie.
     *
     * @param response The {@link DeliveryHttpServletResponse} on which to send
     *            back the cookie.
     * @param name The name of the cookie to send.
     * @param value The value of cookie.
     * @param maxAge The maximum age of the cookie in seconds. Positive values
     *            are persisted on the browser for the indicated number of
     *            seconds, setting the age to 0 (zero) causes the cookie to be
     *            deleted in the browser and using a negative value defines a
     *            temporary cookie to be deleted when the browser exits.
     * @param path The cookie path to use. If empty or <code>null</code> the
     */
    private void sendCookie(HttpServletResponse response, String name,
            String value, int maxAge, String path) {

        if (path == null || path.length() == 0) {
            log.debug("sendCookie: Using root path ''/''");
            path = "/";
        }

        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        response.addCookie(cookie);

        // Tell a potential proxy server that this cookie is uncacheable
        if (this.cacheControl) {
            response.addHeader("Cache-Control", "no-cache=\"Set-Cookie\"");
        }
    }

    /**
     * Handles impersonation based on the request parameter for impersonation
     * (see {@link #sudoParameterName}) and the current setting in the sudo
     * cookie.
     * <p>
     * If the sudo parameter is empty or missing, the current cookie setting for
     * impersonation is used. Else if the parameter is <code>-</code>, the
     * current cookie impersonation is removed and no impersonation will take
     * place for this request. Else the parameter is assumed to contain the
     * handle of a user page acceptable for the {@link Session#impersonate}
     * method.
     *
     * @param req The {@link DeliveryHttpServletRequest} optionally containing
     *            the sudo parameter.
     * @param res The {@link DeliveryHttpServletResponse} to send the
     *            impersonation cookie.
     * @param session The real {@link Session} to optionally replace with an
     *            impersonated session.
     * @return The impersonated session or the input session.
     * @throws LoginException thrown by the {@link Session#impersonate} method.
     * @throws ContentBusException thrown by the {@link Session#impersonate}
     *             method.
     * @see Session#impersonate for details on the user configuration
     *      requirements for impersonation.
     */
    private Session handleImpersonation(HttpServletRequest req,
            HttpServletResponse res, Session session) throws LoginException,
            RepositoryException {

        // the current state of impersonation
        String currentSudo = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int i = 0; currentSudo == null && i < cookies.length; i++) {
                if (sudoCookieName.equals(cookies[i].getName())) {
                    currentSudo = cookies[i].getValue();
                }
            }
        }

        /**
         * sudo parameter : empty or missing to continue to use the setting
         * already stored in the session; or "-" to remove impersonationa
         * altogether (also from the session); or the handle of a user page to
         * impersonate as that user (if possible)
         */
        String sudo = req.getParameter(this.sudoParameterName);
        if (sudo == null || sudo.length() == 0) {
            sudo = currentSudo;
        } else if ("-".equals(sudo)) {
            sudo = null;
        }

        // sudo the session if needed
        if (sudo != null && sudo.length() > 0) {
            Credentials creds = new SimpleCredentials(sudo, new char[0]);
            session = session.impersonate(creds);
        }
        // invariant: same session or successful impersonation

        // set the (new) impersonation
        if (sudo != currentSudo) {
            if (sudo == null) {
                // Parameter set to "-" to clear impersonation, which was
                // active due to cookie setting

                // clear impersonation
                this.sendCookie(res, this.sudoCookieName, "", 0,
                    req.getContextPath());

            } else if (currentSudo == null || !currentSudo.equals(sudo)) {
                // Parameter set to a name. As the cookie is not set yet
                // or is set to another name, send the cookie with current sudo

                // (re-)set impersonation
                this.sendCookie(res, this.sudoCookieName, sudo, -1,
                    req.getContextPath());
            }
        }

        // return the session
        return session;
    }

}
