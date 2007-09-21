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
package org.apache.sling.core.impl.auth;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.Dictionary;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.RequestUtil;
import org.apache.sling.core.auth.AuthenticationHandler;
import org.apache.sling.core.impl.RequestData;
import org.apache.sling.jcr.SlingRepository;
import org.apache.sling.jcr.TooManySessionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>AuthenticationFilter</code> class is the default implementation
 * of the {@link AuthenticationFilter} interface. This class supports :
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
 * {@link Ticket} for the handling of the request. This method uses any of the
 * handlers to extract the user information from the reques. Next a ticket is
 * created for this user information. If no user information is contained in the
 * request (according to the handler), the anonymous ticket is used.
 * <p>
 * If the service is configured with session support, a session is created whose
 * sessionId is transported between client and server using HTTP cookies. The
 * session configuration specifies what name those cookies should have and how
 * long theses sessions will be kept alive between two successive requests. That
 * is, the time-to-life value is really and "idle timeout value".
 * <p>
 * Sessions can be canceled either with the {@link #destroySession} method or
 * when the session times out. To not clutter the session map with old, unused
 * sessions, a separate thread scans the session list for expired sessions
 * removing any one the thread finds. Currently the cleanup routine runs at and
 * interval twice as big as the time-to-life value.
 *
 * @scr.component immediate="true" label="%auth.name"
 *          description="%auth.description"
 * @scr.property name="service.description"
 *          value="Default AuthenticationService implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-900" type="Integer" private="true"
 * @scr.service
 */
public class AuthenticationFilter implements ComponentFilter {

    /**
     * The name of the request attribute containing the AuthenticationHandler
     * which authenticated the current request. If the request is authenticated
     * through a session, this is the handler, which iinitially authenticated
     * the user.
     */
    public static final String REQUEST_ATTRIBUTE_HANDLER = "org.apache.sling.core.impl.auth.authentication_handler";

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    /**
     * @scr.property value="cqsudo"
     */
    public static final String PAR_IMPERSONATION_COOKIE_NAME = "auth.sudo.cookie";

    /**
     * @scr.property value="sudo"
     */
    public static final String PAR_IMPERSONATION_PAR_NAME = "auth.sudo.parameter";

    /**
     * @scr.property value="false" type="Boolean"
     */
    public static final String PAR_ANONYMOUS_ALLOWED = "auth.annonymous";

    /** The default impersonation parameter name */
    private static final String DEFAULT_IMPERSONATION_PARAMETER = "sudo";

    /** The default impersonation cookie name */
    private static final String DEFAULT_IMPERSONATION_COOKIE = "cqsudo";

    /** The default value for allowing anonymous access */
    private static final boolean DEFAULT_ANONYMOUS_ALLOWED = false;

    /**
     * @scr.reference
     */
    private SlingRepository repository;

    /** The name of the impersonation parameter */
    private String sudoParameterName;

    /** The name of the impersonation cookie */
    private String sudoCookieName;

    /** Cache control flag */
    private boolean cacheControl;

    /** Whether access without credentials is allowed */
    boolean anonymousAllowed;

    /**
     * The map of {@link AuthenticationHandler} implementations indexed by
     * configured name of the handler.
     *
     * @scr.reference cardinality="0..n" policy="dynamic"
     */
    private AuthenticationHandler[] handlers = new AuthenticationHandler[0];

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

    // ----------- AbstractCoreFilter ------------------------------------------

    public void doFilter(ComponentRequest request, ComponentResponse response,
            ComponentFilterChain filterChain) throws IOException,
            ComponentException {
        Session session = this.authenticate(request, response);
        if (session != null) {
            try {
                // set the session (throws if no request data is available)
                RequestData.getRequestData(request).setSession(session);

                // continue processing
                filterChain.doFilter(request, response);

            } catch (AccessControlException ace) {

                // try to request authentication fail, if not possible
                if (!this.requestAuthentication(request, response)) {
                    this.sendFailure(response);
                }

            } finally {
                // make sure the session is closed after processing !!
                session.logout();
            }
        }
    }

    public void init(ComponentContext context) {}
    public void destroy() {}

    // ----------- SCR Integration ---------------------------------------------

    protected void activate(org.osgi.service.component.ComponentContext context) {
        Dictionary configuration = context.getProperties();

        String newCookie = (String) configuration.get(PAR_IMPERSONATION_COOKIE_NAME);
        if (newCookie == null || newCookie.length() == 0) {
            newCookie = DEFAULT_IMPERSONATION_COOKIE;
        }
        if (!newCookie.equals(this.sudoCookieName)) {
            log.info("Setting new cookie name for impersonation {} (was {})",
                newCookie, this.sudoCookieName);
            this.sudoCookieName = newCookie;
        }

        String newPar = (String) configuration.get(PAR_IMPERSONATION_PAR_NAME);
        if (newPar == null || newPar.length() == 0) {
            newPar = DEFAULT_IMPERSONATION_PARAMETER;
        }
        if (!newPar.equals(this.sudoParameterName)) {
            log.info(
                "Setting new parameter name for impersonation {} (was {})",
                newPar, this.sudoParameterName);
            this.sudoParameterName = newPar;
        }

        Object flag = configuration.get(PAR_ANONYMOUS_ALLOWED);
        if (flag instanceof Boolean) {
            this.anonymousAllowed = ((Boolean) flag).booleanValue();
        } else {
            this.anonymousAllowed = DEFAULT_ANONYMOUS_ALLOWED;
        }
    }

    protected void bindRepository(SlingRepository repository) {
        this.repository = repository;
    }

    protected void unbindRepository(SlingRepository repository) {
        this.repository = null;
    }

    protected void bindAuthenticationHandler(
            AuthenticationHandler authenticationHandler) {
        // ensure not in the list yet
        for (int i = 0; i < this.handlers.length; i++) {
            if (this.handlers[i] == authenticationHandler) {
                // already in the list, ignore this time
                return;
            }
        }

        AuthenticationHandler[] newHandlers = new AuthenticationHandler[this.handlers.length + 1];
        System.arraycopy(this.handlers, 0, newHandlers, 0, this.handlers.length);
        newHandlers[this.handlers.length] = authenticationHandler;
        this.handlers = newHandlers;
    }

    protected void unbindAuthenticationHandler(
            AuthenticationHandler authenticationHandler) {
        for (int i = 0; i < this.handlers.length; i++) {
            if (this.handlers[i] == authenticationHandler) {
                // remove this handler
                AuthenticationHandler[] newHandlers = new AuthenticationHandler[this.handlers.length - 1];

                if (i > 0) System.arraycopy(this.handlers, 0, newHandlers, 0, i);
                if (i < newHandlers.length)
                    System.arraycopy(this.handlers, i + 1, newHandlers, i,
                        newHandlers.length - i);

                this.handlers = newHandlers;
            }
        }
    }

    // ---------- AuthenticationFilter interface ------------------------------

    /**
     * Checks the authentication contained in the request. This check is only
     * based on the original request object, no URI translation has taken place
     * yet.
     * <p>
     * The method will either return the anonymous ticket, if no authentication
     * handler could extract credentials from the request, or null, if
     * credentials extracted from the request are not valid to create a ticket
     * or a ticket identifying the user's credentials extracted from the ticket.
     * This method must not call back to client for valid credentials, if they
     * are missing.
     * <p>
     * If sessions are enabled the returned ticket may be impersonated, that is
     * for another user than the one who has authenticated.
     *
     * @param req The request object containing the information for the
     *            authentication.
     * @param res The response object which may be used to send the information
     *            on the request failure to the user.
     * @return A valid ContentBus Ticket identifying the request user or the
     *         anonymous ticket, if the request does not contain credential data
     *         or null if the credential data cannot be used to create a ticket.
     *         If <code>null</code> the request should be terminated as it can
     *         be assumed, that during this method enough response information
     *         has been sent to the client.
     */
    private Session authenticate(ComponentRequest req, ComponentResponse res) {

        // 0. Get package for request and be anonymous if none configured
        AuthenticationHandler handler = this.getAuthHandler(req);
        if (handler == null) {
            log.debug("authenticate: no authentication needed, anonymous access");
            return this.getAnonymousSession(req, res);
        }

        // 1. Check request login session - only if we have sessions
        // not any more :-)

        // 2. Ask the packages handler for the credentials
        Credentials creds = handler.authenticate(req, res);

        // 3. Check Credentials
        if (creds == AuthenticationHandler.DOING_AUTH) {

            log.debug("authenticate: ongoing authentication in the handler");
            // is this the correct return value ??
            return null;

        } else if (creds == null) {

            log.debug("authenticate: no credentials in the request, anonymous");
            return this.getAnonymousSession(req, res);

        } else {
            // try to connect
            try {
                log.debug("authenticate: credentials, trying to get a ticket");
                Session session = this.repository.login(creds, null);

                // handle impersonation
                session = this.handleImpersonation(req, res, session);

                return session;

            } catch (TooManySessionsException se) {
                log.info("Too many sessions for user: {}", se.getMessage());
            } catch (LoginException e) {
                log.info("Unable to authenticate: {}", e.getMessage());
            } catch (RepositoryException re) {
                log.error("Unable to authenticate", re);
            }

            // request authentication information and send 403 (Forbidden)
            // if the handle cannot request authentication information.
            if (!handler.requestAuthentication(req, res)) {
                this.sendFailure(res);
            }

            // end request
            return null;
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
     * @param req The request object
     * @param res The response object to which to send the request
     * @return true if the information could be requested or false, if the
     *         request should fail with the appropriate error status
     */
    public boolean requestAuthentication(ComponentRequest req, ComponentResponse res) {

        AuthenticationHandler handler = this.getAuthHandler(req);
        if (handler != null) {
            log.debug("requestAuthentication: requesting authentication using "
                + "handler: {0}", handler);

            return handler.requestAuthentication(req, res);
        }

        log.info("requestAuthentication: no handler found for request");
        return false;
    }

    // ---------- internal ----------------------------------------------------

    private AuthenticationHandler getAuthHandler(ComponentRequest req) {
        AuthenticationHandler[] local = this.handlers;
        for (int i = 0; i < local.length; i++) {
            if (local[i].handles(req)) {
                return local[i];
            }
        }

        // no handler found for the request ....
        return null;
    }

    // private AuthPackage getAuthPackage(HttpServletRequest req) {
    //
    // // Get the request URI from the request or from the include
    // String requestURI = req.getRequestURI();
    // log.debug("getAuthPackage: Check for {0}", requestURI);
    //
    // // Look in the packages list
    // for (int i = 0; i < numPackages; i++) {
    // if (packages[i].contains(requestURI)) {
    // return packages[i];
    // }
    // }
    // // invariant: returned or no package found
    //
    // // if no package is responsible
    // return null;
    // }

    // TODO
    private Session getAnonymousSession(ComponentRequest req, ComponentResponse res) {
        // login anonymously, log the exact cause in case of failure
        if (this.anonymousAllowed) {
            try {
                return this.repository.login();
            } catch (TooManySessionsException se) {
                log.error("getAnonymousSession: Too many anonymous users active", se);
            } catch (LoginException le) {
                log.error("getAnonymousSession: Login failure, requesting authentication", le);
            } catch (RepositoryException re) {
                log.error("getAnonymousSession: Cannot get anonymous session", re);
            }
        } else {
            log.debug("getAnonymousSession: Anonymous access not allowed by configuration");
        }

        // request authentication now, and fail if not possible
        if (!this.requestAuthentication(req, res)) {
            this.sendFailure(res);
        }

        // fallback to no session
        return null;
    }

    // TODO
    private void sendFailure(ComponentResponse res) {
        try {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
        } catch (IOException ioe) {
            log.error("Cannot send error " + HttpServletResponse.SC_FORBIDDEN
                + " code", ioe);
        }
    }

    /**
     * Tries to instantiate a handler from the given handler configuration.
     *
     * @param defaultPackage The name of the package for the handler class if
     *            the class name is not a fully qualified class name.
     * @param className The name of the class. If this is not fully qualified,
     *            the class is assumed to be in the defaultPackage.
     * @param configPath The path name (handle) of the handler configuration or
     *            <code>null</code> if the handler has no configuration.
     * @throws ServiceException if the handler cannot be instantiated and
     *             initialized.
     */
    // private AuthenticationHandler getHandlerInstance(String defaultPackage,
    // String className, String configPath) {
    //
    // // check fully qualified classname
    // if (className.indexOf('.') < 0 && defaultPackage != null) {
    // className = defaultPackage + "." + className;
    // }
    //
    // Exception e = null;
    // try {
    // // Read the configuration
    // Config config = (configPath != null) ? MutableConfig.createFromXml(
    // null, ticket, configPath) : null;
    //
    // // get the instance
    // Class clazz = classLoader.loadClass(className);
    // AuthenticationHandler handler = (AuthenticationHandler)
    // clazz.newInstance();
    //
    // // initialize the handler
    // handler.init(ticket, config);
    //
    // // return the handler
    // return handler;
    //
    // } catch (ContentBusException cbe) {
    // // MutableConfig.createFromXml()
    // e = cbe;
    // } catch (ClassNotFoundException cnfe) {
    // // Class.forName()
    // e = cnfe;
    // } catch (InstantiationException ie) {
    // // newInstance()
    // e = ie;
    // } catch (IllegalAccessException iae) {
    // // newInstance()
    // e = iae;
    // } catch (ClassCastException cce) {
    // // clazz is not an AuthenticationHandler
    // e = cce;
    // }
    //
    // // invariant : e != null if we get here
    // throw new ServiceException(e.getMessage(), e);
    // }
    /**
     * Sends the session cookie for the name session with the given age in
     * seconds. This sends a Version 1 cookie.
     *
     * @param response The
     *            {@link DeliveryHttpServletResponse} on
     *            which to send back the cookie.
     * @param name The name of the cookie to send.
     * @param value The value of cookie.
     * @param maxAge The maximum age of the cookie in seconds. Positive values
     *            are persisted on the browser for the indicated number of
     *            seconds, setting the age to 0 (zero) causes the cookie to be
     *            deleted in the browser and using a negative value defines a
     *            temporary cookie to be deleted when the browser exits.
     * @param path The cookie path to use. If empty or <code>null</code> the
     */
    private void sendCookie(ComponentResponse response, String name, String value,
            int maxAge, String path) {

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
     * handle of a user page acceptable for the {@link Ticket#impersonate}
     * method.
     *
     * @param req The {@link DeliveryHttpServletRequest} optionally containing
     *            the sudo parameter.
     * @param res The {@link DeliveryHttpServletResponse} to
     *            send the impersonation cookie.
     * @param ticket The real {@link Ticket} to optionally replace with an
     *            impersonated ticket.
     * @return The impersonated ticket or the input ticket.
     * @throws LoginException thrown by the {@link Ticket#impersonate} method.
     * @throws ContentBusException thrown by the {@link Ticket#impersonate}
     *             method.
     * @see Ticket#impersonate for details on the user configuration
     *      requirements for impersonation.
     */
    private Session handleImpersonation(ComponentRequest req, ComponentResponse res,
            Session session) throws LoginException, RepositoryException {

        // the current state of impersonation
        Cookie sudoCookie = RequestUtil.getCookie(req, this.sudoCookieName);
        String currentSudo = (sudoCookie == null)
                ? null
                : sudoCookie.getValue();

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

        // sudo the ticket if needed
        if (sudo != null && sudo.length() > 0) {
            Credentials creds = new SimpleCredentials(sudo, new char[0]);
            session = session.impersonate(creds);
        }
        // invariant: same ticket or successful impersonation

        // set the (new) impersonation
        if (sudo != currentSudo) {
            if (sudo == null) {
                // Parameter set to "-" to clear impersonation, which was
                // active due to cookie setting

                // clear impersonation
                this.sendCookie(res, this.sudoCookieName, "", 0, req.getContextPath());

            } else if (currentSudo == null || !currentSudo.equals(sudo)) {
                // Parameter set to a name. As the cookie is not set yet
                // or is set to another name, send the cookie with current sudo

                // (re-)set impersonation
                this.sendCookie(res, this.sudoCookieName, sudo, -1, req.getContextPath());
            }
        }

        // return the ticket
        return session;
    }

    // ---------- internal class -----------------------------------------------

    /**
     * The <code>AuthPackage</code> class implements the
     * {@link ContentPackage} package providing additional information for
     * handler detection.
     */
    // private class AuthPackage implements ContentPackage {
    //
    // /**
    // * The name of the configuration attribute defining the handler to use
    // * for requests matching this package.
    // */
    // private static final String HANDLER_ATTR = "handler";
    //
    // /**
    // * The name of the configuration attribute defining the parameter the
    // * handler may use when requesting authentication.
    // */
    // private static final String PARAM_ATTR = "param";
    //
    // /**
    // * The default value of the parameter attribute
    // *
    // * @see #PARAM_ATTR
    // */
    // private static final String DEFAULT_PARAM = "";
    //
    // /** the name of the handler */
    // private final String handler;
    //
    // /** the addInfo parameter for the requestAuthentication call */
    // private final String param;
    //
    // private final ContentPackage delegatee;
    //
    // /**
    // * Create the package from the configuration element using the given
    // * handler name as the default handler name
    // *
    // * @param config The configuration element on which to base the package
    // * definition and authentication configuration.
    // * @param defaultHandler The defualt authentication handler name to use
    // * if none is specified in the configuration.
    // */
    // AuthPackage(Configuration config, String defaultHandler) throws
    // RepositoryException {
    // FilterContentPackageBuilder builder = new FilterContentPackageBuilder();
    // builder.addFilters((Session) null, config);
    // delegatee = builder.createContentPackage();
    //
    // this.handler = config.getString(HANDLER_ATTR, defaultHandler);
    // this.param = config.getString(PARAM_ATTR, DEFAULT_PARAM);
    // }
    //
    // /**
    // * Returns the name of the handler to use for requests matching this
    // * package.
    // *
    // * @return The name of the handler to use.
    // */
    // String getHandler() {
    // return handler;
    // }
    //
    // /**
    // * The parameter to provide to the handler when requesting
    // * authentication.
    // *
    // * @return The authentication request parameter for the handler.
    // */
    // String getParam() {
    // return param;
    // }
    //
    // /**
    // * @see ContentPackage#contains(String)
    // */
    // public boolean contains(String handle) {
    // return delegatee.contains(handle);
    // }
    //
    // /**
    // * @see ContentPackage#contains(Ticket, String)
    // */
    // public boolean contains(Session session, String handle) {
    // return delegatee.contains(session, handle);
    // }
    //
    // /**
    // * @see ContentPackage#contains(Page)
    // */
    // public boolean contains(Item item) {
    // return delegatee.contains(item);
    // }
    //
    // /**
    // * @see ContentPackage#getTraversingStartPoints()
    // */
    // public String[] getTraversingStartPoints() {
    // return delegatee.getTraversingStartPoints();
    // }
    // }
}
