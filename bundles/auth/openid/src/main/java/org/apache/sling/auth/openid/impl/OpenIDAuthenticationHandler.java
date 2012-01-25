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
package org.apache.sling.auth.openid.impl;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.auth.core.spi.AbstractAuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.auth.openid.OpenIDConstants;
import org.apache.sling.auth.openid.OpenIDFailure;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyuproject.openid.OpenIdUser;
import com.dyuproject.openid.RelyingParty;
import com.dyuproject.openid.manager.CookieBasedUserManager;

/**
 * The <code>AuthorizationHeaderAuthenticationHandler</code> class implements
 * the authorization steps based on the Authorization header of the HTTP
 * request. This authenticator should eventually support both BASIC and DIGEST
 * authentication methods.
 */
@Component(immediate=false, metatype=true, label="%auth.openid.name",
    description="%auth.openid.description", name="org.apache.sling.auth.openid.OpenIDAuthenticationHandler")
@Service
@org.apache.felix.scr.annotations.Properties({
    @Property(name=Constants.SERVICE_VENDOR, value="The Apache Software Foundation"),
    @Property(name=Constants.SERVICE_DESCRIPTION, value="Apache Sling OpenID Authentication Handler"),
    @Property(name=AuthenticationHandler.PATH_PROPERTY, value="/", unbounded=PropertyUnbounded.ARRAY),
    @Property(name=AuthenticationHandler.TYPE_PROPERTY, value=OpenIDConstants.OPENID_AUTH, propertyPrivate=true)
})
public class OpenIDAuthenticationHandler extends AbstractAuthenticationHandler {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value=AuthenticationFormServlet.SERVLET_PATH)
    public static final String PROP_LOGIN_FORM = "openid.login.form";

    public static final String DEFAULT_LOGIN_IDENTIFIER_FORM_FIELD = RelyingParty.DEFAULT_IDENTIFIER_PARAMETER;

    @Property(value=DEFAULT_LOGIN_IDENTIFIER_FORM_FIELD)
    public static final String PROP_LOGIN_IDENTIFIER_FORM_FIELD = "openid.login.identifier";

    public static final String DEFAULT_EXTERNAL_URL_PREFIX = "";

    @Property(value=DEFAULT_EXTERNAL_URL_PREFIX)
    public static final String PROP_EXTERNAL_URL_PREFIX = "openid.external.url.prefix";

    public static final boolean DEFAULT_USE_COOKIE = true;

    @Property(boolValue=DEFAULT_USE_COOKIE)
    public static final String PROP_USE_COOKIE = "openid.use.cookie";

    public static final String DEFAULT_COOKIE_DOMAIN = "";

    @Property(value=DEFAULT_COOKIE_DOMAIN)
    public static final String PROP_COOKIE_DOMAIN = "openid.cookie.domain";

    public static final String DEFAULT_COOKIE_NAME = "sling.openid";

    @Property(value=DEFAULT_COOKIE_NAME)
    public static final String PROP_COOKIE_NAME = "openid.cookie.name";

    public static final String DEFAULT_COOKIE_SECRET_KEY = "secret";

    @Property(DEFAULT_COOKIE_SECRET_KEY)
    public static final String PROP_COOKIE_SECRET_KEY = "openid.cookie.secret.key";

    private static final String DEFAULT_OPENID_USER_ATTR = "openid.user";

    @Property(DEFAULT_OPENID_USER_ATTR)
    private static final String PROP_OPENID_USER_ATTR = "openid.user.attr";

    private static final String DEFAULT_OPEN_ID_IDENTIFIER_PROPERTY = "openid.identity";

    @Property(value=DEFAULT_OPEN_ID_IDENTIFIER_PROPERTY)
    private static final String PROP_OPEN_ID_IDENTIFIER_PROPERTY = "openid.property.identity";

    /**
     * The name of the attribute set on the OpenID user object to cache the
     * mapping from the OpenID identifier to the JCR user id to prevent repeated
     * time-consuming search for a matching user.
     */
    private static final String ATTR_USER_ID = "jcr.userid";

    static final String SLASH = "/";

    @Reference
    private SlingRepository repository;

    private Session session;

    private UserManager userManager;

    private ComponentContext context;

    private String loginForm;

    /**
     * The prefix used to create an external URL for the resource to which the
     * client should be returned to after successfully authenticating with the
     * OpenID provider. This parameter is used as the basis for the
     * <code>return_to</code> parameter of the OpenID authentication request.
     * <p>
     * If this is not set, it defaults to the created from the request as
     * follows:
     *
     * <pre>
     * request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()</code>
     * </pre>
     * <p>
     * where the port part is omitted if it is the default port for the scheme.
     */
    private String externalUrlPrefix;

    /**
     * The OpenID realm to authenticate with. This String is presented to the
     * client authenticating against the OpenID provider to inform about the
     * site wishing to authenticate a user. This parameter is used as the value
     * of the <code>realm</code> (and <code>trust_root</code> parameter of the
     * OpenID authentication request.
     * <p>
     * If this is not set, the {@link #externalUrlPrefix} is used as the realm.
     * <p>
     * This field may be used to convey a wildcard realm, such as
     * <code>http://*.apache.org</code>.
     */
    private String realm;

    private boolean useCookie;

    private String cookieDomain;

    private char[] cookieSecret;

    private String cookieName;

    /**
     * Name of the request parameter used provide the OpenID identifier.
     * Configured by {@link #PROP_LOGIN_IDENTIFIER_FORM_FIELD}, defaults to
     * {@link #DEFAULT_LOGIN_IDENTIFIER_FORM_FIELD}.
     */
    private String identifierParam;

    /**
     * Name of the JCR User property listing the OpenID identities with which
     * the user is related. Configured by
     * {@link #PROP_OPEN_ID_IDENTIFIER_PROPERTY}, defaults to
     * {@link #DEFAULT_OPEN_ID_IDENTIFIER_PROPERTY}. This property may be
     * multi-valued if the user is associated with more than one OpenID
     * identifiers.
     *
     * @see #getUserName(OpenIdUser)
     */
    private String identityProperty;

    private String openIdAttribute;

    private RelyingParty relyingParty;

    private ServiceRegistration loginModule;

    public OpenIDAuthenticationHandler() {
        log.info("OpenIDAuthenticationHandler created");
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
     *         DOING_AUTH if the handler is in an authentication transaction
     *         with the client or null if the request does not contain
     *         authentication information. In case of DOING_AUTH, the method
     *         must have sent a response indicating that fact to the client.
     */
    public AuthenticationInfo extractCredentials(HttpServletRequest request,
            HttpServletResponse response) {

        try {
            final RelyingParty relyingParty = getRelyingParty(request);

            // this may throw a ClassCastException after an update of the
            // bundle if the HTTP Session object still holds on to an
            // OpenIdUser instance created by the old bundle.
            final OpenIdUser user = discover(relyingParty, request);

            // no OpenID user in the request, check whether this is an
            // OpenID response at all
            if (user == null) {

                if (RelyingParty.isAuthResponse(request)) {

                    log.debug("OpenID authentication timeout");
                    response.sendRedirect(request.getRequestURI());
                    return AuthenticationInfo.DOING_AUTH;

                } else if (RelyingParty.isAuthCancel(request)) {

                    log.info("OpenID authentication cancelled by user");
                    return handleAuthFailure(OpenIDFailure.AUTHENTICATION,
                        request);
                }

                // check whether the request has an OpenID identifier
                // request parameter not leading to a valid OpenID
                // transaction; fail authentication in this case
                final String identifier = request.getParameter(identifierParam);
                if (identifier != null) {
                    log.info("OpenID authentication failed (probably failed to discover OpenID Provider)");
                    return handleAuthFailure(OpenIDFailure.DISCOVERY, request);
                }

            } else if (user.isAuthenticated()) {

                // user already authenticated
                return getAuthInfoFromUser(user);

            } else if (user.isAssociated()) {

                if (RelyingParty.isAuthResponse(request)) {

                    if (relyingParty.verifyAuth(user, request, response)) {
                        // authenticated
                        response.sendRedirect(getReturnToResource(request));
                        return AuthenticationInfo.DOING_AUTH;
                    }

                    // failed verification
                    return handleAuthFailure(OpenIDFailure.VERIFICATION,
                        request);

                }

                // Assume a cancel or some other non-successful response
                // from provider failed verification
                relyingParty.invalidate(request, response);

                return handleAuthFailure(OpenIDFailure.AUTHENTICATION, request);

            } else {

                // associate and authenticate user

                // prepare the url for the return_to parameter
                final String url = getBaseUrl(request);

                // set the realm/trustroot from configuration or the root url
                final String trustRoot = (realm == null) ? url : realm;

                // append the resource URL to the returnTo address
                final String returnTo = url + getReturnToPath(request);

                if (relyingParty.associateAndAuthenticate(user, request,
                    response, trustRoot, trustRoot, returnTo)) {
                    // user is associated and then redirected to his openid
                    // provider for authentication
                    return AuthenticationInfo.DOING_AUTH;
                }

                // failed association or auth request generation
                return handleAuthFailure(OpenIDFailure.ASSOCIATION, request);
            }

        } catch (ClassCastException cce) {
            // expected after bundle update when using HTTP Sessions
            log.warn("extractCredentials: Found OpenID user data in HTTP Session which cannot be used; failing credentials extraction");
            log.debug("extractCredentials: dump", cce);
            dropCredentials(request, response);
            return handleAuthFailure(OpenIDFailure.OTHER, request);

        } catch (Exception e) {
            log.error("Error processing OpenID request", e);
        }

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
     * @throws IOException if an error occurs sending back the response.
     */
    public boolean requestCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 0. ignore this handler if an authentication handler is requested
        if (ignoreRequestCredentials(request)) {
            // consider this handler is not used
            return false;
        }

        //check the referer to see if the request is for this handler
        if (!AuthUtil.checkReferer(request, loginForm)) {
        	//not for this handler, so return
        	return false;
        }


        // requestAuthentication is only called after a failedauthentication
        // so it makes sense to remove any existing login
        final RelyingParty relyingParty = getRelyingParty(request);
        relyingParty.invalidate(request, response);

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Authenticator.LOGIN_RESOURCE,
            getLoginResource(request, null));

        // append indication of previous login failure
        if (request.getAttribute(FAILURE_REASON) != null) {
            final Object jReason = request.getAttribute(FAILURE_REASON);
            @SuppressWarnings("rawtypes")
            final String reason = (jReason instanceof Enum)
                    ? ((Enum) jReason).name()
                    : jReason.toString();
            params.put(FAILURE_REASON, reason);
        }

        final Object paramIdentifier = request.getAttribute(OpenIDConstants.OPENID_IDENTITY);
        if (paramIdentifier instanceof String) {
            params.put(OpenIDConstants.OPENID_IDENTITY,
                (String) paramIdentifier);
        }

        try {
            sendRedirect(request, response, loginForm, params);
        } catch (IOException e) {
            log.error("Failed to redirect to the login form " + loginForm, e);
        }

        return true;
    }

    /**
     * Invalidates the request with the Relying Party if a user is actually
     * available for the request.
     */
    public void dropCredentials(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            getRelyingParty(request).invalidate(request, response);
        } catch (Exception e) {
            log.warn("dropAuthentication: Problem checking whether the user is logged in at all, assuming not logged in and therefore not logging out");
        }
    }

    @Override
    public void authenticationFailed(HttpServletRequest request,
            HttpServletResponse response, AuthenticationInfo authInfo) {

        /*
         * Called if extractCredentials provided an authenticated OpenID user
         * which could not be mapped to a valid repository user !!
         *
         * invalidate the curren OpenID user and set a failure reason
         */

        OpenIdUser user = null;
        try {
            user = getRelyingParty(request).discover(request);
        } catch (Exception e) {
            // don't care ...
        }

        dropCredentials(request, response);

        request.setAttribute(OpenIDConstants.OPENID_FAILURE_REASON,
            OpenIDFailure.REPOSITORY);

        if (user != null && user.getIdentity() != null) {
            request.setAttribute(OpenIDConstants.OPENID_IDENTITY,
                user.getIdentity());
        }
    }

    @Override
    public boolean authenticationSucceeded(HttpServletRequest request,
            HttpServletResponse response, AuthenticationInfo authInfo) {

        // FIXME: check redirect after login !
        return DefaultAuthenticationFeedbackHandler.handleRedirect(request,
            response);
    }

    // ---------- internal

    /**
     * Tries to discover the OpenID user from the request. This involves any of
     * the following steps:
     * <ul>
     * <li>The user is already available as a request attribute</li>
     * <li>The user is available from the HTTP Session or the OpenID cookie</li>
     * <li>The user is identifier with an OpenID identifier supplied with the
     * {@link #identifierParam} request parameter</li>
     * <li>No user is available from the request at all</li>
     * </ul>
     * <p>
     * If no user is available or any error occurs while trying to discover the
     * user from the request, <code>null</code> is returned.
     *
     * @param relyingParty The <code>RelyingParty</code> object used to discover
     *            the OpenID user from the request
     * @param request The <code>HttpServletRequest</code> from which the user is
     *            to be discovered
     * @return the <code>OpenIdUser</code> discovered from the request or
     *         <code>null</code> if no user can be discovered or the discovery
     *         failed.
     * @throws ClassCastException may be thrown if an OpenID user object is
     *             still stored in the HTTP Session after the authentication
     *             handler bundle has been updated.
     */
    private OpenIdUser discover(final RelyingParty relyingParty,
            final HttpServletRequest request) {
        try {
            // this may throw a ClassCastException after an update of the
            // bundle if the HTTP Session object still holds on to an
            // OpenIdUser instance created by the old bundle.
            return relyingParty.discover(request);

        } catch (UnknownHostException uhe) {
            // openid_identifier names an invalid host
            log.info(
                "discover: The OpenID identifier cannot be resolved because it designates an unknown host {}",
                uhe.getMessage());

        } catch (IOException ioe) {
            // another IO problem talking to the OpenID provider
            log.info("discover: Failure to communicate with OpenID provider",
                ioe);

        } catch (ClassCastException cce) {
            // rethrow class cast exception from failure to use OpenID user
            // from Http Session created prior to authentication handler update
            throw cce;

        } catch (Exception e) {
            // any other problem discovering the identifier
            log.warn(
                "discover: Unexpected failure discovering the OpenID user", e);
        }

        // exception discovering the identifier
        return null;
    }

    private AuthenticationInfo handleAuthFailure(OpenIDFailure failure,
            HttpServletRequest request) {

        request.setAttribute(OpenIDConstants.OPENID_FAILURE_REASON,
            failure);
        return AuthenticationInfo.FAIL_AUTH;
    }

    // ---------- SCR Integration

    protected void activate(ComponentContext componentContext) {
        context = componentContext;
        Dictionary<?, ?> props = context.getProperties();

        loginForm = OsgiUtil.toString(props.get(
            PROP_LOGIN_FORM), AuthenticationFormServlet.SERVLET_PATH);

        externalUrlPrefix = OsgiUtil.toString(props.get(
            PROP_EXTERNAL_URL_PREFIX), DEFAULT_EXTERNAL_URL_PREFIX);

        // JCR user properties used to match OpenID users
        identityProperty = OsgiUtil.toString(
            props.get(PROP_OPEN_ID_IDENTIFIER_PROPERTY),
            DEFAULT_OPEN_ID_IDENTIFIER_PROPERTY);

        // DYU OpenID properties
        useCookie = OsgiUtil.toBoolean(props.get(
            PROP_USE_COOKIE), DEFAULT_USE_COOKIE);

        cookieDomain = OsgiUtil.toString(props.get(
            PROP_COOKIE_DOMAIN), DEFAULT_COOKIE_DOMAIN);

        cookieName = OsgiUtil.toString(props.get(
            PROP_COOKIE_NAME), DEFAULT_COOKIE_NAME);

        identifierParam = OsgiUtil.toString(props.get(
            PROP_LOGIN_IDENTIFIER_FORM_FIELD),
            DEFAULT_LOGIN_IDENTIFIER_FORM_FIELD);

        cookieSecret = OsgiUtil.toString(
            props.get(PROP_COOKIE_SECRET_KEY),
            DEFAULT_COOKIE_SECRET_KEY).toCharArray();

        openIdAttribute = OsgiUtil.toString(props.get(
            PROP_OPENID_USER_ATTR), DEFAULT_OPENID_USER_ATTR);

        this.loginModule = null;
        try {
            this.loginModule = OpenIDLoginModulePlugin.register(this,
                componentContext.getBundleContext());
        } catch (Throwable t) {
            log.info("Cannot register OpenIDLoginModulePlugin. This is expected if Sling LoginModulePlugin services are not supported");
            log.debug("dump", t);
        }
    }

    protected void deactivate(
            @SuppressWarnings("unused") ComponentContext componentContext) {
        if (loginModule != null) {
            loginModule.unregister();
            loginModule = null;
        }

        if (session != null) {
            try {
                if (session.isLive()) {
                    session.logout();
                }
            } catch (Throwable t) {
                log.error("deactivate: Unexpected problem logging out session",
                    t);
            }
            userManager = null;
            session = null;
        }
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Returns <code>true</code> if this authentication handler should ignore
     * the call to
     * {@link #requestCredentials(HttpServletRequest, HttpServletResponse)}.
     * <p>
     * This method returns <code>true</code> if the
     * {@link #REQUEST_LOGIN_PARAMETER} is set to any value other than "Form"
     * (HttpServletRequest.FORM_AUTH).
     */
    private boolean ignoreRequestCredentials(final HttpServletRequest request) {
        final String requestLogin = request.getParameter(REQUEST_LOGIN_PARAMETER);
        return requestLogin != null
            && !OpenIDConstants.OPENID_AUTH.equals(requestLogin);
    }

    private AuthenticationInfo getAuthInfoFromUser(final OpenIdUser user) {
        final AuthenticationInfo info = new AuthenticationInfo(
            OpenIDConstants.OPENID_AUTH, getUserName(user));

        // if there is no login module plugin service, set the credentials
        // attribute to the user's OpenID identity, otherwise set it to
        // the actual OpenIDUser object
        if (loginModule == null) {
            info.put(openIdAttribute, user.getIdentity());
        } else {
            info.put(openIdAttribute, user);
        }

        return info;
    }

    OpenIdUser getOpenIdUser(final Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            SimpleCredentials creds = (SimpleCredentials) credentials;
            return (OpenIdUser) creds.getAttribute(openIdAttribute);
        }
        return null;
    }

    /**
     * Find a JCR Repository user name for the given OpenIdUser. Derives a name
     * from the user identifier if none can be found.
     */
    private String getUserName(final OpenIdUser user) {

        final Object nickname = user.getAttribute(ATTR_USER_ID);
        if (nickname instanceof String) {
            return (String) nickname;
        }

        final String identity = user.getIdentity();
        String userId = null;
        UserManager userManager = getUserManager();
        if (userManager != null) {
            userId = getUserIdByProperty(userManager, identityProperty,
                identity);
        }

        // still null, use some dummy value to fail login and be able
        // to associate user afterwards
        if (userId == null) {
            userId = "::not_valid_for_login::";
        } else {
            // store the id in the attribute
            user.setAttribute(ATTR_USER_ID, userId);
        }

        return userId;
    }

    private UserManager getUserManager() {
        if (userManager == null) {
            try {
                if (session == null) {
                    session = repository.loginAdministrative(null);
                }
                if (session instanceof JackrabbitSession) {
                    userManager = ((JackrabbitSession) session).getUserManager();
                }
            } catch (RepositoryException re) {
                log.error("getUserManager: Cannot get UserManager", re);
            }
        }
        return userManager;
    }

    private String getUserIdByProperty(final UserManager userManager,
            final String propName, final String propValue) {
        String userId = null;
        try {
            Iterator<?> users = userManager.findAuthorizables(propName,
                propValue, UserManager.SEARCH_TYPE_USER);

            // use the first user found
            if (users.hasNext()) {
                userId = ((User) users.next()).getID();

                // warn if more than one user found
                if (users.hasNext()) {
                    log.warn(
                        "getUserName: Multiple users found with property {}={}; using {}",
                        new Object[] { propName, propValue, userId });
                }
            }
        } catch (RepositoryException re) {
            log.warn("getUserName: Problem finding user with property {}={}",
                new Object[] { propName, propValue }, re);
        }

        return userId;
    }

    private RelyingParty getRelyingParty(final HttpServletRequest request) {
        if (relyingParty == null) {
            Properties openIdProps = new Properties();
            openIdProps.setProperty("openid.identifier.parameter",
                identifierParam);

            if (useCookie) {

                final String ctxPath = request.getContextPath();
                final String cookiePath = (ctxPath == null || ctxPath.length() == 0)
                        ? "/"
                        : ctxPath;

                openIdProps.setProperty("openid.user.manager",
                    CookieBasedUserManager.class.getName());
                openIdProps.setProperty("openid.user.manager.cookie.name",
                    cookieName);
                openIdProps.setProperty("openid.user.manager.cookie.path",
                    cookiePath);

                if (cookieDomain != null) {
                    openIdProps.setProperty(
                        "openid.user.manager.cookie.domain", cookieDomain);
                }

                openIdProps.setProperty(
                    "openid.user.manager.cookie.security.secret_key",
                    new String(cookieSecret));
            }

            final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            try {
                relyingParty = RelyingParty.newInstance(openIdProps);
            } finally {
                Thread.currentThread().setContextClassLoader(oldTCCL);
            }
        }
        return relyingParty;
    }

    String getBaseUrl(HttpServletRequest request) {
        /*
         * package private for unit testing
         */
        if (externalUrlPrefix == null || externalUrlPrefix.length() == 0) {
            final String scheme = request.getScheme();
            final String host = request.getServerName();
            final int port = request.getServerPort();
            final String ctx = request.getContextPath();

            StringBuilder url = new StringBuilder();
            url.append(scheme).append("://");
            url.append(host);
            if ((port > 0) && (!"http".equals(scheme) || port != 80)
                && (!"https".equals(scheme) || port != 443)) {
                url.append(':').append(port);
            }
            url.append(ctx);
            return url.toString();
        }
        return externalUrlPrefix;
    }

    /**
     * Returns the resource to use as the OpenID returnTo path. This resource is
     * either set as a the resource request attribute or parameter or is derived
     * from the current request (URI plus query string). Next the resource is
     * prefixed with the request context path to ensure it is properly
     * transmitted accross the OpenID redirection series.
     *
     * @param request The request providing the returnTo URL information
     * @return The properly setup returnTo URL path.
     */
    private String getReturnToPath(final HttpServletRequest request) {
        // find the return to parameter with optional request parameters
        String resource = getLoginResource(request, null);
        if (resource == null) {
            resource = request.getRequestURI();
            if (request.getQueryString() != null) {
                resource += "?" + request.getQueryString();
            }
        }

        // prefix with the context path if not empty
        String prefix = request.getContextPath();
        return prefix.length() > 0 ? prefix.concat(resource) : resource;
    }

    /**
     * Returns the target resource to which the client is to be redirected. This
     * is the path from the returnTo parameter sent on the initial OpenID
     * redirect which has been encoded with
     * {@link #getEncodedReturnToResource(HttpServletRequest)}. Thus this method
     * must do the reverse operations, namely cutting of the request context
     * path prefix.
     *
     * @param request The request providing the request URL and context path
     * @return the path to which the client is be redirected after successful
     *         OpenID authentication
     */
    private String getReturnToResource(final HttpServletRequest request) {
        final String resource = request.getRequestURI();
        if (request.getQueryString() != null) {
            return resource + "?" + request.getQueryString();
        }
        return resource;
    }
}