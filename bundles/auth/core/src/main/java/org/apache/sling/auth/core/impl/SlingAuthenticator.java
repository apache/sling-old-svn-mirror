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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.SimpleCredentials;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.CredentialExpiredException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.auth.NoAuthenticationHandlerException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.impl.engine.EngineAuthenticationHandlerHolder;
import org.apache.sling.auth.core.spi.AbstractAuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.AuthenticationInfoPostProcessor;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingAuthenticator</code> class is the default implementation for
 * handling authentication. This class supports :
 * <ul>
 * <li>Support for login sessions where session ids are exchanged with cookies
 * <li>Support for multiple authentication handlers, which must implement the
 * {@link AuthenticationHandler} interface.
 * <li>
 * <p>
 * Currently this class does not support multiple handlers for any one request
 * URL.
 */
@Component(name = "org.apache.sling.engine.impl.auth.SlingAuthenticator", label = "%auth.name", description = "%auth.description", metatype = true)
@Service(value = { Authenticator.class, AuthenticationSupport.class, ServletRequestListener.class })
@Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
public class SlingAuthenticator implements Authenticator,
        AuthenticationSupport, ServletRequestListener {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(SlingAuthenticator.class);

    @Property(name = Constants.SERVICE_DESCRIPTION)
    static final String DESCRIPTION = "Apache Sling Request Authenticator";

    /** The default impersonation cookie name */
    private static final String DEFAULT_IMPERSONATION_COOKIE = "sling.sudo";

    @Property(value = DEFAULT_IMPERSONATION_COOKIE)
    public static final String PAR_IMPERSONATION_COOKIE_NAME = "auth.sudo.cookie";

    /** The default impersonation parameter name */
    private static final String DEFAULT_IMPERSONATION_PARAMETER = "sudo";

    @Property(value = DEFAULT_IMPERSONATION_PARAMETER)
    public static final String PAR_IMPERSONATION_PAR_NAME = "auth.sudo.parameter";

    /** The default value for allowing anonymous access */
    private static final boolean DEFAULT_ANONYMOUS_ALLOWED = true;

    @Property(boolValue = DEFAULT_ANONYMOUS_ALLOWED)
    public static final String PAR_ANONYMOUS_ALLOWED = "auth.annonymous";

    @Property(cardinality = 2147483647)
    private static final String PAR_AUTH_REQ = AuthConstants.AUTH_REQUIREMENTS;

    @Property()
    private static final String PAR_ANONYMOUS_USER = "sling.auth.anonymous.user";

    @Property() // TODO: This should be a PASSWORD type
    private static final String PAR_ANONYMOUS_PASSWORD = "sling.auth.anonymous.password";

    /**
     * Value of the {@link #PAR_HTTP_AUTH} property to fully enable the built-in
     * HTTP Authentication Handler (value is "enabled").
     */
    private static final String HTTP_AUTH_ENABLED = "enabled";

    /**
     * Value of the {@link #PAR_HTTP_AUTH} property to completely disable the
     * built-in HTTP Authentication Handler (value is "disabled").
     */
    private static final String HTTP_AUTH_DISABLED = "disabled";

    /**
     * Value of the {@link #PAR_HTTP_AUTH} property to enable extracting the
     * credentials if the HTTP Basic authentication header is present (value is
     * "preemptive"). In <i>preemptive</i> mode, though, the
     * <code>requestCredentials</code> and <code>dropCredentials</code> methods
     * will not send back a 401 response.
     */
    private static final String HTTP_AUTH_PREEMPTIVE = "preemptive";

    @Property(value = HTTP_AUTH_PREEMPTIVE, options = {
        @PropertyOption(name = HTTP_AUTH_ENABLED, value = "Enabled"),
        @PropertyOption(name = HTTP_AUTH_PREEMPTIVE, value = "Enabled (Preemptive)"),
        @PropertyOption(name = HTTP_AUTH_DISABLED, value = "Disabled") })
    private static final String PAR_HTTP_AUTH = "auth.http";

    /**
     * The default realm for the built-in HTTP Basic authentication handler.
     */
    private static final String DEFAULT_REALM = "Sling (Development)";

    /**
     * The name of the configuration property used to set the Realm of the
     * built-in HTTP Basic authentication handler.
     */
    @Property(value = DEFAULT_REALM)
    public static final String PAR_REALM_NAME = "auth.http.realm";

    /**
     * Default request URI suffix to expect to be handled by authentication
     * handlers and not expecting to cause
     * {@link #handleSecurity(HttpServletRequest, HttpServletResponse)} to
     * return <code>true</code>.
     */
    private static final String DEFAULT_AUTH_URI_SUFFIX = "/j_security_check";
    
    /**
     * The name of the form submission parameter providing the new password of
     * the user (value is "j_newpassword").
     */
    private static final String PAR_NEW_PASSWORD = "j_newpassword";

    /**
     * The name of the configuration property used to set a (potentially
     * empty) list of request URI suffixes intended to be handled by
     * authentication handlers.
     */
    @Property(value = DEFAULT_AUTH_URI_SUFFIX, unbounded = PropertyUnbounded.ARRAY)
    public static final String PAR_AUTH_URI_SUFFIX = "auth.uri.suffix";

    /**
     * The name of the {@link AuthenticationInfo} property providing the option
     * {@link org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler}
     * handler to be called back on login failure or success.
     */
    private static final String AUTH_INFO_PROP_FEEDBACK_HANDLER = "$$sling.auth.AuthenticationFeedbackHandler$$";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private PathBasedHolderCache<AbstractAuthenticationHandlerHolder> authHandlerCache = new PathBasedHolderCache<AbstractAuthenticationHandlerHolder>();

    // package protected for access in inner class ...
    PathBasedHolderCache<AuthenticationRequirementHolder> authRequiredCache = new PathBasedHolderCache<AuthenticationRequirementHolder>();

    /** The name of the impersonation parameter */
    private String sudoParameterName;

    /** The name of the impersonation cookie */
    private String sudoCookieName;

    /** Cache control flag */
    private boolean cacheControl;

    /**
     * The configured URI suffices indicating a authentication requests and
     * requiring redirects and thus returning <code>false</code> from the
     * #handleSecurity method.
     * <p>
     * This will be <code>null</code> if there are no suffices to consider.
     */
    private String[] authUriSuffices;

    /**
     * The name of the user to assume for anonymous access. By default this is
     * <code>null</code> to use <code>null</code> credentials and thus use the
     * system provided identification.
     *
     * @see #getAnonymousCredentials()
     */
    private String anonUser;

    /**
     * The password to use for anonymous access. This property is only used if
     * the {@link #anonUser} field is not <code>null</code>.
     *
     * @see #getAnonymousCredentials()
     */
    private char[] anonPassword;

    /** HTTP Basic authentication handler */
    private HttpBasicAuthenticationHandler httpBasicHandler;

    /** Web Console Plugin service registration */
    private ServiceRegistration webConsolePlugin;

    /**
     * The listener for services registered with "sling.auth.requirements" to
     * update the internal authentication requirements
     */
    private SlingAuthenticatorServiceListener serviceListener;

    /**
     * ServiceTracker tracking AuthenticationHandler services
     */
    private ServiceTracker authHandlerTracker;

    /**
     * ServiceTracker tracking old Sling Engine AuthenticationHandler services
     */
    private ServiceTracker engineAuthHandlerTracker;

    /**
     * ServiceTracker tracking AuthenticationInfoPostProcessor services
     */
    private ServiceTracker authInfoPostProcessorTracker;

    /**
     * The event admin service.
     */
    @Reference(policy=ReferencePolicy.DYNAMIC)
    private volatile EventAdmin eventAdmin;

    // ---------- SCR integration

    @SuppressWarnings("unused")
    @Activate
    private void activate(final BundleContext bundleContext,
            final Map<String, Object> properties) {
        modified(properties);

        AuthenticatorWebConsolePlugin plugin = new AuthenticatorWebConsolePlugin(
            this);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("felix.webconsole.label", plugin.getLabel());
        props.put("felix.webconsole.title", plugin.getTitle());
        props.put(Constants.SERVICE_DESCRIPTION,
            "Sling Request Authenticator WebConsole Plugin");
        props.put(Constants.SERVICE_VENDOR,
            properties.get(Constants.SERVICE_VENDOR));

        webConsolePlugin = bundleContext.registerService(
            "javax.servlet.Servlet", plugin, props);

        serviceListener = SlingAuthenticatorServiceListener.createListener(
            bundleContext, this);

        authHandlerTracker = new AuthenticationHandlerTracker(bundleContext,
            authHandlerCache);
        engineAuthHandlerTracker = new EngineAuthenticationHandlerTracker(
            bundleContext, authHandlerCache);
        authInfoPostProcessorTracker = new ServiceTracker(bundleContext, AuthenticationInfoPostProcessor.SERVICE_NAME, null);
        authInfoPostProcessorTracker.open();
    }

    @Modified
    private void modified(Map<String, Object> properties) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }

        String newCookie = (String) properties.get(PAR_IMPERSONATION_COOKIE_NAME);
        if (newCookie == null || newCookie.length() == 0) {
            newCookie = DEFAULT_IMPERSONATION_COOKIE;
        }
        if (!newCookie.equals(this.sudoCookieName)) {
            log.info(
                "modified: Setting new cookie name for impersonation {} (was {})",
                newCookie, this.sudoCookieName);
            this.sudoCookieName = newCookie;
        }

        String newPar = (String) properties.get(PAR_IMPERSONATION_PAR_NAME);
        if (newPar == null || newPar.length() == 0) {
            newPar = DEFAULT_IMPERSONATION_PARAMETER;
        }
        if (!newPar.equals(this.sudoParameterName)) {
            log.info(
                "modified: Setting new parameter name for impersonation {} (was {})",
                newPar, this.sudoParameterName);
            this.sudoParameterName = newPar;
        }

        authRequiredCache.clear();

        final boolean anonAllowed = OsgiUtil.toBoolean(properties.get(PAR_ANONYMOUS_ALLOWED), DEFAULT_ANONYMOUS_ALLOWED);
        authRequiredCache.addHolder(new AuthenticationRequirementHolder("/", !anonAllowed, null));

        String[] authReqs = OsgiUtil.toStringArray(properties.get(PAR_AUTH_REQ));
        if (authReqs != null) {
            for (String authReq : authReqs) {
                if (authReq != null && authReq.length() > 0) {
                    authRequiredCache.addHolder(AuthenticationRequirementHolder.fromConfig(
                        authReq, null));
                }
            }
        }

        final String anonUser = OsgiUtil.toString(properties.get(PAR_ANONYMOUS_USER), "");
        if (anonUser.length() > 0) {
            this.anonUser = anonUser;
            this.anonPassword = OsgiUtil.toString(properties.get(PAR_ANONYMOUS_PASSWORD), "").toCharArray();
        } else {
            this.anonUser = null;
            this.anonPassword = null;
        }

        authUriSuffices = OsgiUtil.toStringArray(properties.get(PAR_AUTH_URI_SUFFIX),
            new String[] { DEFAULT_AUTH_URI_SUFFIX });

        // don't require authentication for login/logout servlets
        authRequiredCache.addHolder(new AuthenticationRequirementHolder(
            LoginServlet.SERVLET_PATH, false, null));
        authRequiredCache.addHolder(new AuthenticationRequirementHolder(
            LogoutServlet.SERVLET_PATH, false, null));

        // add all registered services
        if (serviceListener != null) {
            serviceListener.registerServices();
        }

        final String http;
        if (anonAllowed) {
            http = OsgiUtil.toString(properties.get(PAR_HTTP_AUTH), HTTP_AUTH_PREEMPTIVE);
        } else {
            http = HTTP_AUTH_ENABLED;
            log.debug("modified: Anonymous Access is denied thus HTTP Basic Authentication is fully enabled");
        }

        if (HTTP_AUTH_DISABLED.equals(http)) {
            httpBasicHandler = null;
        } else {
            final String realm = OsgiUtil.toString(properties.get(PAR_REALM_NAME), DEFAULT_REALM);
            httpBasicHandler = new HttpBasicAuthenticationHandler(realm, HTTP_AUTH_ENABLED.equals(http));
        }
    }

    @SuppressWarnings("unused")
    @Deactivate
    private void deactivate(final BundleContext bundleContext) {
        if (engineAuthHandlerTracker != null) {
            engineAuthHandlerTracker.close();
            engineAuthHandlerTracker = null;
        }

        if (authHandlerTracker != null) {
            authHandlerTracker.close();
            authHandlerTracker = null;
        }

        if (serviceListener != null) {
            bundleContext.removeServiceListener(serviceListener);
            serviceListener = null;
        }

        if (webConsolePlugin != null) {
            webConsolePlugin.unregister();
            webConsolePlugin = null;
        }
    }

    // --------- AuthenticationSupport interface

    /**
     * Checks the authentication contained in the request. This check is only
     * based on the original request object, no URI translation has taken place
     * yet.
     * <p>
     *
     * @param request The request object containing the information for the
     *            authentication.
     * @param response The response object which may be used to send the
     *            information on the request failure to the user.
     * @return <code>true</code> if request processing should continue assuming
     *         successful authentication. If <code>false</code> is returned it
     *         is assumed a response has been sent to the client and the request
     *         is terminated.
     */
    public boolean handleSecurity(HttpServletRequest request,
            HttpServletResponse response) {

        // 0. Nothing to do, if the session is also in the request
        // this might be the case if the request is handled as a result
        // of a servlet container include inside another Sling request
        Object sessionAttr = request.getAttribute(REQUEST_ATTRIBUTE_RESOLVER);
        if (sessionAttr instanceof ResourceResolver) {
            log.debug("handleSecurity: Request already authenticated, nothing to do");
            return true;
        } else if (sessionAttr != null) {
            // warn and remove existing non-session
            log.warn("handleSecurity: Overwriting existing ResourceResolver attribute ({})", sessionAttr);
            request.removeAttribute(REQUEST_ATTRIBUTE_RESOLVER);
        }

        boolean process = doHandleSecurity(request, response);
        if (process && expectAuthenticationHandler(request)) {
            log.warn("handleSecurity: AuthenticationHandler did not block request; access denied");
            request.removeAttribute(AuthenticationHandler.FAILURE_REASON);
            request.removeAttribute(AuthenticationHandler.FAILURE_REASON_CODE);
            AuthUtil.sendInvalid(request, response);
            return false;
        }

        return process;
    }

    private boolean doHandleSecurity(HttpServletRequest request, HttpServletResponse response) {

        // 1. Ask all authentication handlers to try to extract credentials
        final AuthenticationInfo authInfo = getAuthenticationInfo(request, response);

        // 2. PostProcess credentials
        try {
            postProcess(authInfo, request, response);
        } catch (LoginException e) {
            handleLoginFailure(request, response, authInfo, e);
            return false;
        }

        // 3. Check Credentials
        if (authInfo == AuthenticationInfo.DOING_AUTH) {

            log.debug("doHandleSecurity: ongoing authentication in the handler");
            return false;

        } else if (authInfo == AuthenticationInfo.FAIL_AUTH) {

            log.debug("doHandleSecurity: Credentials present but not valid, request authentication again");
            AuthUtil.setLoginResourceAttribute(request, request.getRequestURI());
            doLogin(request, response);
            return false;

        } else if (authInfo.getAuthType() == null) {

            log.debug("doHandleSecurity: No credentials in the request, anonymous");
            return getAnonymousResolver(request, response, authInfo);

        } else {

            log.debug("doHandleSecurity: Trying to get a session for {}", authInfo.getUser());
            return getResolver(request, response, authInfo);

        }
    }

    // ---------- Authenticator interface

    /**
     * Requests authentication information from the client. Returns
     * <code>true</code> if the information has been requested and request
     * processing can be terminated. Otherwise the request information could not
     * be requested and the request should be terminated with a 403/FORBIDDEN
     * response.
     * <p>
     * Any response sent by the handler is also handled by the error handler
     * infrastructure.
     *
     * @param request The request object
     * @param response The response object to which to send the request
     * @throws IllegalStateException If response is already committed
     * @throws NoAuthenticationHandlerException If no authentication handler
     *             claims responsibility to authenticate the request.
     */
    public void login(HttpServletRequest request, HttpServletResponse response) {

        // ensure the response is not committed yet
        if (response.isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }

        // select path used for authentication handler selection
        final Collection<AbstractAuthenticationHandlerHolder>[] holdersArray = this.authHandlerCache
                .findApplicableHolder(request);
        final String path = getHandlerSelectionPath(request);
        boolean done = false;
        for (int m = 0; !done && m < holdersArray.length; m++) {
            final Collection<AbstractAuthenticationHandlerHolder> holderList = holdersArray[m];
            if ( holderList != null ) {
                for (AbstractAuthenticationHandlerHolder holder : holderList) {
                    if (path.startsWith(holder.path)) {
                        log.debug("login: requesting authentication using handler: {}",
                            holder);

                        try {
                            done = holder.requestCredentials(request, response);
                        } catch (IOException ioe) {
                            log.error(
                                "login: Failed sending authentication request through handler "
                                    + holder + ", access forbidden", ioe);
                            done = true;
                        }
                        if (done) {
                            break;
                        }
                    }
                }
            }
        }

        // fall back to HTTP Basic handler (if not done already)
        if (!done && httpBasicHandler != null) {
            done = httpBasicHandler.requestCredentials(request, response);
        }

        // no handler could send an authentication request, throw
        if (!done) {
            int size = 0;
            for (int m = 0; m < holdersArray.length; m++) {
                if (holdersArray[m] != null) {
                    size += holdersArray[m].size();
                }
            }
            log.info("login: No handler for request ({} handlers available)", size);
            throw new NoAuthenticationHandlerException();
        }
    }

    /**
     * Logs out the user calling all applicable
     * {@link org.apache.sling.auth.core.spi.AuthenticationHandler}
     * authentication handlers.
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        // ensure the response is not committed yet
        if (response.isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }

        // make sure impersonation is dropped
        setSudoCookie(request, response, new AuthenticationInfo("dummy", request.getRemoteUser()));

        final String path = getHandlerSelectionPath(request);
        final Collection<AbstractAuthenticationHandlerHolder>[] holdersArray = this.authHandlerCache
                .findApplicableHolder(request);
        for (int m = 0; m < holdersArray.length; m++) {
            final Collection<AbstractAuthenticationHandlerHolder> holderSet = holdersArray[m];
            if (holderSet != null) {
                for (AbstractAuthenticationHandlerHolder holder : holderSet) {
                    if (path.startsWith(holder.path)) {
                        log.debug("logout: dropping authentication using handler: {}",
                            holder);

                        try {
                            holder.dropCredentials(request, response);
                        } catch (IOException ioe) {
                            log.error(
                                "logout: Failed dropping authentication through handler "
                                    + holder, ioe);
                        }
                    }
                }
            }
        }

        if (httpBasicHandler != null) {
            httpBasicHandler.dropCredentials(request, response);
        }

        redirectAfterLogout(request, response);
    }

    // ---------- ServletRequestListener

    public void requestInitialized(ServletRequestEvent sre) {
        // don't care
    }

    public void requestDestroyed(ServletRequestEvent sre) {
        ServletRequest request = sre.getServletRequest();
        Object resolverAttr = request.getAttribute(REQUEST_ATTRIBUTE_RESOLVER);
        if (resolverAttr instanceof ResourceResolver) {
            ((ResourceResolver) resolverAttr).close();
            request.removeAttribute(REQUEST_ATTRIBUTE_RESOLVER);
        }
    }

    // ---------- WebConsolePlugin support

    /**
     * Returns the list of registered authentication handlers as a map
     */
    Map<String, List<String>> getAuthenticationHandler() {
        List<AbstractAuthenticationHandlerHolder> registeredHolders = authHandlerCache.getHolders();
        LinkedHashMap<String, List<String>> handlerMap = new LinkedHashMap<String, List<String>>();
        for (AbstractAuthenticationHandlerHolder holder : registeredHolders) {
            List<String> provider = handlerMap.get(holder.fullPath);
            if (provider == null) {
                provider = new ArrayList<String>();
                handlerMap.put(holder.fullPath, provider);
            }
            provider.add(holder.getProvider());
        }
        if (httpBasicHandler != null) {
            List<String> provider = handlerMap.get("/");
            if (provider == null) {
                provider = new ArrayList<String>();
                handlerMap.put("/", provider);
            }
            provider.add(httpBasicHandler.toString());
        }
        return handlerMap;
    }

    List<AuthenticationRequirementHolder> getAuthenticationRequirements() {
        return authRequiredCache.getHolders();
    }

    /**
     * Returns the name of the user to assume for requests without credentials.
     * This may be <code>null</code> if not configured and the default anonymous
     * user is to be used.
     * <p>
     * The configured password cannot be requested.
     */
    String getAnonUserName() {
        return anonUser;
    }

    String getSudoCookieName() {
        return sudoCookieName;
    }

    String getSudoParameterName() {
        return sudoParameterName;
    }

    // ---------- internal

    private String getPath(HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        if (request.getServletPath() != null) {
            sb.append(request.getServletPath());
        }
        if (request.getPathInfo() != null) {
            sb.append(request.getPathInfo());
        }
        return sb.toString();
    }

    private AuthenticationInfo getAuthenticationInfo(HttpServletRequest request, HttpServletResponse response) {

        // Get the path used to select the authenticator, if the SlingServlet
        // itself has been requested without any more info, this will be empty
        // and we assume the root (SLING-722)
        String path = getPath(request);
        if (path.length() == 0) {
            path = "/";
        }

        final Collection<AbstractAuthenticationHandlerHolder>[] localArray = this.authHandlerCache
                .findApplicableHolder(request);
        for (int m = 0; m < localArray.length; m++) {
            final Collection<AbstractAuthenticationHandlerHolder> local = localArray[m];
            if (local != null) {
                for (AbstractAuthenticationHandlerHolder holder : local) {
                    if (path.startsWith(holder.path)) {
                        final AuthenticationInfo authInfo = holder.extractCredentials(
                            request, response);

                        if (authInfo != null) {
                            // add the feedback handler to the info (may be null)
                            authInfo.put(AUTH_INFO_PROP_FEEDBACK_HANDLER,
                                holder.getFeedbackHandler());

                            return authInfo;
                        }
                    }
                }
            }
        }

        // check whether the HTTP Basic handler can extract the header
        if (httpBasicHandler != null) {
            final AuthenticationInfo authInfo = httpBasicHandler.extractCredentials(
                request, response);
            if (authInfo != null) {
                authInfo.put(AUTH_INFO_PROP_FEEDBACK_HANDLER, httpBasicHandler);
                return authInfo;
            }
        }

        // no handler found for the request ....
        log.debug("getAuthenticationInfo: no handler could extract credentials; assuming anonymous");
        return getAnonymousCredentials();
    }

    /**
     * Run through the available post processors.
     */
    private void postProcess(AuthenticationInfo info, HttpServletRequest request, HttpServletResponse response)
    		throws LoginException {
        Object[] services = authInfoPostProcessorTracker.getServices();
        if (services != null) {
            for (Object serviceObj : services) {
                ((AuthenticationInfoPostProcessor)serviceObj).postProcess(info, request, response);
            }
        }
    }


    /**
     * Try to acquire a ResourceResolver as indicated by authInfo
     *
     * @return <code>true</code> if request processing should continue assuming
     *         successful authentication. If <code>false</code> is returned it
     *         is assumed a response has been sent to the client and the request
     *         is terminated.
     */
    private boolean getResolver(final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationInfo authInfo) {

        // prepare the feedback handler
        final AuthenticationFeedbackHandler feedbackHandler = (AuthenticationFeedbackHandler) authInfo.remove(AUTH_INFO_PROP_FEEDBACK_HANDLER);
        final Object sendLoginEvent = authInfo.remove(AuthConstants.AUTH_INFO_LOGIN);

        // try to connect
        try {
            handleImpersonation(request, authInfo);
            handlePasswordChange(request, authInfo);
            ResourceResolver resolver = resourceResolverFactory.getResourceResolver(authInfo);
            final boolean impersChanged = setSudoCookie(request, response, authInfo);

            if (sendLoginEvent != null) {
                postLoginEvent(authInfo);
            }

            // provide the resource resolver to the feedback handler
            request.setAttribute(REQUEST_ATTRIBUTE_RESOLVER, resolver);

            boolean processRequest = true;

            // custom feedback handler with option to redirect
            if (feedbackHandler != null) {
                processRequest = !feedbackHandler.authenticationSucceeded(request, response, authInfo);
            }

            if (processRequest) {
                if (AuthUtil.isValidateRequest(request)) {
                    AuthUtil.sendValid(response);
                    processRequest = false;
                } else if (impersChanged || feedbackHandler == null) {
                    processRequest = !DefaultAuthenticationFeedbackHandler.handleRedirect(request, response);
                }
            }

            if (processRequest) {
                // process: set required attributes
                setAttributes(resolver, authInfo.getAuthType(), request);
            } else {
                // terminate: cleanup
                resolver.close();
            }

            return processRequest;

        } catch (LoginException re) {

            // handle failure feedback before proceeding to handling the
            // failed login internally
            if (feedbackHandler != null) {
                feedbackHandler.authenticationFailed(request, response,
                    authInfo);
            }

            // now find a way to get credentials unless the feedback handler
            // has committed a response to the client already
            if (!response.isCommitted()) {
                return handleLoginFailure(request, response, authInfo, re);
            }

        }

        // end request
        return false;

    }

    private boolean expectAuthenticationHandler(final HttpServletRequest request) {
        if (this.authUriSuffices != null) {
            final String requestUri = request.getRequestURI();
            for (final String uriSuffix : this.authUriSuffices) {
                if (requestUri.endsWith(uriSuffix)) {
                    return true;
                }
            }
        }
        return false;
   }

    /** Try to acquire an anonymous ResourceResolver */
    private boolean getAnonymousResolver(final HttpServletRequest request,
            final HttpServletResponse response, final AuthenticationInfo authInfo) {

        // Get an anonymous session if allowed, or if we are handling
        // a request for the login servlet
        if (isAnonAllowed(request)) {

            try {
                ResourceResolver resolver = resourceResolverFactory.getResourceResolver(authInfo);

                // check whether the client asked for redirect after
                // authentication and/or impersonation
                if (DefaultAuthenticationFeedbackHandler.handleRedirect(
                    request, response)) {

                    // request will now be terminated, so close the resolver
                    // to release resources
                    resolver.close();

                    return false;
                }

                // set the attributes for further processing
                setAttributes(resolver, null, request);

                return true;

            } catch (LoginException re) {

                // cannot login > fail login, do not try to authenticate
                handleLoginFailure(request, response, new AuthenticationInfo(null, "anonymous user"), re);
                return false;

            }
        }

        // If we get here, anonymous access is not allowed: redirect
        // to the login servlet
        log.info("getAnonymousResolver: Anonymous access not allowed by configuration - requesting credentials");
        doLogin(request, response);

        // fallback to no session
        return false;
    }

    private boolean isAnonAllowed(HttpServletRequest request) {

        String path = getPath(request);
        if (path.length() == 0) {
            path = "/";
        }

        final Collection<AuthenticationRequirementHolder>[] holderSetArray = authRequiredCache
                .findApplicableHolder(request);
        for (int m = 0; m < holderSetArray.length; m++) {
            final Collection<AuthenticationRequirementHolder> holders = holderSetArray[m];
            if (holders != null) {
                for (AuthenticationRequirementHolder holder : holders) {
                    if (path.startsWith(holder.path)) {
                        return !holder.requiresAuthentication();
                    }
                }
            }
        }

        // fallback to anonymous not allowed (aka authentication required)
        return false;
    }

    /**
     * Returns credentials to use for anonymous resource access. If an anonymous
     * user is configued, this returns an {@link AuthenticationInfo} instance
     * whose authentication type is <code>null</code> and the user name and
     * password are set according to the {@link #PAR_ANONYMOUS_USER} and
     * {@link #PAR_ANONYMOUS_PASSWORD} configurations. Otherwise
     * the user name and password fields are just <code>null</code>.
     */
    private AuthenticationInfo getAnonymousCredentials() {
        AuthenticationInfo info = new AuthenticationInfo(null);
        if (this.anonUser != null) {
            info.setUser(this.anonUser);
            info.setPassword(this.anonPassword);
        }
        return info;
    }

    private boolean handleLoginFailure(final HttpServletRequest request,
            final HttpServletResponse response, final AuthenticationInfo authInfo,
            final Exception reason) {

        String user = authInfo.getUser();
        boolean processRequest = false;
        if (reason.getClass().getName().contains("TooManySessionsException")) {

            // to many users, send a 503 Service Unavailable
            log.info("handleLoginFailure: Too many sessions for {}: {}", user,
                reason.getMessage());

            try {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SlingAuthenticator: Too Many Users");
            } catch (IOException ioe) {
                log.error(
                    "handleLoginFailure: Cannot send status 503 to client", ioe);
            }

        } else if (reason instanceof LoginException) {
            log.info("handleLoginFailure: Unable to authenticate {}: {}", user,
                    reason.getMessage());
            if (isAnonAllowed(request) && !expectAuthenticationHandler(request) && !AuthUtil.isValidateRequest(request)) {
                log.debug("handleLoginFailure: LoginException on an anonymous resource, fallback to getAnonymousResolver");
                processRequest = getAnonymousResolver(request, response, new AuthenticationInfo(null));
            } else {
                // request authentication information and send 403 (Forbidden)
                // if no handler can request authentication information.            

                AuthenticationHandler.FAILURE_REASON_CODES code = AuthenticationHandler.FAILURE_REASON_CODES.INVALID_LOGIN;
                String message = "User name and password do not match";

                if (reason.getCause() instanceof CredentialExpiredException) {
                    // force failure attribute to be set so handlers can
                    // react to this special circumstance
                    Object creds = authInfo.get("user.jcr.credentials");
                    if (creds instanceof SimpleCredentials && ((SimpleCredentials) creds).getAttribute("PasswordHistoryException") != null) {
                        code = AuthenticationHandler.FAILURE_REASON_CODES.PASSWORD_EXPIRED_AND_NEW_PASSWORD_IN_HISTORY;
                        message = "Password expired and new password found in password history";
                    } else {
                        code = AuthenticationHandler.FAILURE_REASON_CODES.PASSWORD_EXPIRED;
                        message = "Password expired";
                    }
                } else if (reason.getCause() instanceof AccountLockedException) {
                    code = AuthenticationHandler.FAILURE_REASON_CODES.ACCOUNT_LOCKED;
                    message = "Account is locked";
                } else if (reason.getCause() instanceof AccountNotFoundException) {
                    code = AuthenticationHandler.FAILURE_REASON_CODES.ACCOUNT_NOT_FOUND;
                    message = "Account was not found";
                }

                // preset a reason for the login failure
                request.setAttribute(AuthenticationHandler.FAILURE_REASON_CODE, code);
                ensureAttribute(request, AuthenticationHandler.FAILURE_REASON, message);

                doLogin(request, response);
            }

        } else {

            // general problem, send a 500 Internal Server Error
            log.error("handleLoginFailure: Unable to authenticate " + user,
                reason);

            try {
                response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "SlingAuthenticator: data access error, reason="
                        + reason.getClass().getSimpleName());
            } catch (IOException ioe) {
                log.error(
                    "handleLoginFailure: Cannot send status 500 to client", ioe);
            }
        }
        return processRequest;

    }

    /**
     * Tries to request credentials from the client. The following mechanisms
     * are implemented by this method:
     * <ul>
     * <li>If the request is a credentials validation request (see
     * {@link AbstractAuthenticationHandler#isValidateRequest(HttpServletRequest)}
     * ) a 403/FORBIDDEN response is sent back.</li>
     * <li>If the request is not considered a
     * {@link #isBrowserRequest(HttpServletRequest) browser request} and the
     * HTTP Basic Authentication Handler is at least enabled for preemptive
     * credentials processing, a 401/UNAUTHORIZED response is sent back. This
     * helps implementing HTTP Basic authentication with WebDAV clients. If HTTP
     * Basic Authentication is completely switched of a 403/FORBIDDEN response
     * is sent back instead.</li>
     * <li>If the request is considered an
     * {@link #isAjaxRequest(HttpServletRequest) Ajax request} a 403/FORBIDDEN
     * response is simply sent back because we assume an Ajax requestor cannot
     * properly handle any request for credentials graciously.</li>
     * <li>Otherwise the {@link #login(HttpServletRequest, HttpServletResponse)}
     * method is called to try to find and call an authentication handler to
     * request credentials from the client. If none is available or willing to
     * request credentials, a 403/FORBIDDEN response is also sent back to the
     * client.</li>
     * </ul>
     * <p>
     * If a 403/FORBIDDEN response is sent back the {@link AbstractAuthenticationHandler#X_REASON} header is
     * set to a either the value of the
     * {@link AuthenticationHandler#FAILURE_REASON} request attribute or to some
     * generic description describing the reason. To actually send the response
     * the
     * {@link AbstractAuthenticationHandler#sendInvalid(HttpServletRequest, HttpServletResponse)}
     * method is called.
     * <p>
     * This method is called in three situations:
     * <ul>
     * <li>If the request contains no credentials but anonymous login is not
     * allowed</li>
     * <li>If the request contains credentials but getting the Resource Resolver
     * using the provided credentials fails</li>
     * <li>If the selected authentication handler indicated any presented
     * credentials are not valid</li>
     * </ul>
     *
     * @param request The current request
     * @param response The response to send the credentials request (or access
     *            denial to)
     * @see AbstractAuthenticationHandler#isValidateRequest(HttpServletRequest)
     * @see #isBrowserRequest(HttpServletRequest)
     * @see #isAjaxRequest(HttpServletRequest)
     * @see AbstractAuthenticationHandler#sendInvalid(HttpServletRequest,
     *      HttpServletResponse)
     */
    private void doLogin(HttpServletRequest request,
            HttpServletResponse response) {

        if (!AuthUtil.isValidateRequest(request)) {

            if (AuthUtil.isBrowserRequest(request)) {

                if (!AuthUtil.isAjaxRequest(request) && !isLoginLoop(request)) {
                    try {

                        login(request, response);
                        return;

                    } catch (IllegalStateException ise) {

                        log.error("doLogin: Cannot login: Response already committed");
                        return;

                    } catch (NoAuthenticationHandlerException nahe) {

                        /*
                         * Don't set the failureReason for missing
                         * authentication handlers to not disclose this setup
                         * information.
                         */

                        log.error("doLogin: Cannot login: No AuthenticationHandler available to handle the request");

                    }
                }

            } else {

                // Presumably this is WebDAV. If HTTP Basic is fully enabled or
                // enabled for preemptive credential support, we just request
                // HTTP Basic credentials. Otherwise (HTTP Basic is fully
                // switched off, 403 is sent back)
                if (httpBasicHandler != null) {
                    httpBasicHandler.sendUnauthorized(response);
                    return;
                }

            }
        }

        // if we are here, we cannot redirect to the login form because it is
        // an XHR request or because there is no authentication handler willing
        // request credentials from the client or because it is a failed
        // credential validation

        // ensure a failure reason
        ensureAttribute(request, AuthenticationHandler.FAILURE_REASON,
            "Authentication Failed");

        AuthUtil.sendInvalid(request, response);
    }

    /**
     * Returns <code>true</code> if the current request was referred to by the
     * same URL as the current request has. This is assumed to be caused by a
     * loop in requesting credentials from the client. Such a loop will probably
     * never cause the request for credentials to succeed, so it must be broken.
     *
     * @param request The request to check
     * @return <code>true</code> if the request is considered to be a loop;
     *         <code>false</code> otherwise
     */
    private boolean isLoginLoop(final HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            StringBuffer sb = request.getRequestURL();
            if (request.getQueryString() != null) {
                sb.append('?').append(request.getQueryString());
            }
            return referer.equals(sb.toString());
        }

        // no referer means no loop
        return false;
    }

    /**
     * Sets the name request attribute to the given value unless the request
     * attribute is already set a non-<code>null</code> value.
     *
     * @param request The request on which to set the attribute
     * @param attribute The name of the attribute to check/set
     * @param value The value to set the attribute to if it is not already set
     */
    private void ensureAttribute(final HttpServletRequest request,
            final String attribute, final Object value) {
        if (request.getAttribute(attribute) == null) {
            request.setAttribute(attribute, value);
        }
    }

    /**
     * Sets the request attributes required by the OSGi HttpContext interface
     * specification for the <code>handleSecurity</code> method. In addition the
     * {@link SlingAuthenticator#REQUEST_ATTRIBUTE_RESOLVER} request attribute
     * is set to the ResourceResolver.
     */
    private void setAttributes(final ResourceResolver resolver, final String authType,
            final HttpServletRequest request) {

        // HttpService API required attributes
        request.setAttribute(HttpContext.REMOTE_USER, resolver.getUserID());
        request.setAttribute(HttpContext.AUTHENTICATION_TYPE, authType);

        // resource resolver for down-stream use
        request.setAttribute(REQUEST_ATTRIBUTE_RESOLVER, resolver);

        log.debug(
            "setAttributes: ResourceResolver stored as request attribute: user={}",
            resolver.getUserID());
    }

    /**
     * Sends the session cookie for the name session with the given age in
     * seconds. This sends a Version 1 cookie.
     *
     * @param response The {@link HttpServletResponse} on which to send
     *            back the cookie.
     * @param user The name of the user to impersonate as. This will be quoted
     *            and used as the cookie value.
     * @param maxAge The maximum age of the cookie in seconds. Positive values
     *            are persisted on the browser for the indicated number of
     *            seconds, setting the age to 0 (zero) causes the cookie to be
     *            deleted in the browser and using a negative value defines a
     *            temporary cookie to be deleted when the browser exits.
     * @param path The cookie path to use. This is intended to be the web
     *            application's context path. If this is empty or
     *            <code>null</code> the root path will be used assuming the web
     *            application is registered at the root context.
     * @param owner The name of the user originally authenticated in the request
     *            and who is now impersonating as <i>user</i>.
     */
    private void sendSudoCookie(HttpServletResponse response,
            final String user, final int maxAge, final String path,
            final String owner) {

        final String quotedUser;
        try {
            quotedUser = quoteCookieValue(user);
        } catch (IllegalArgumentException iae) {
            log.error(
                "sendSudoCookie: Failed to quote value '{}' of cookie {}: {}",
                new Object[] { user, this.sudoCookieName, iae.getMessage() });
            return;
        }

        if (quotedUser != null) {
            Cookie cookie = new Cookie(this.sudoCookieName, quotedUser);
            cookie.setMaxAge(maxAge);
            cookie.setPath((path == null || path.length() == 0) ? "/" : path);
            try {
                cookie.setComment(owner + " impersonates as " + user);
            } catch (IllegalArgumentException iae) {
                // ignore
            }

            response.addCookie(cookie);

            // Tell a potential proxy server that this request is uncacheable
            // due to the Set-Cookie header being sent
            if (this.cacheControl) {
                response.addHeader("Cache-Control", "no-cache=\"Set-Cookie\"");
            }
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
     * place for this request. Else the parameter is assumed to contain the name
     * of a user to impersonate as.
     *
     * @param req The {@link HttpServletRequest} optionally containing
     *            the sudo parameter.
     * @param authInfo The authentication info into which the
     *            <code>sudo.user.id</code> property is set to the impersonator
     *            user.
     */
    private void handleImpersonation(HttpServletRequest req,
            AuthenticationInfo authInfo) {
        String currentSudo = getSudoCookieValue(req);

        /**
         * sudo parameter : empty or missing to continue to use the setting
         * already stored in the session; or "-" to remove impersonation
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
            authInfo.put(ResourceResolverFactory.USER_IMPERSONATION, sudo);
        }
    }

    /**
     * Handles password change based on the request parameter for the new password
     * (see {@link #newPasswordParameterName}).
     * <p>
     * If the new password request parameter is present, it is added to the authInfo
     * object, which is later transformed to SimpleCredentials attributes.
     *
     * @param req The {@link HttpServletRequest} optionally containing
     *            the new password parameter.
     * @param authInfo The authentication info into which the
     *            <code>newPassword</code> property is set.
     */
    private void handlePasswordChange(HttpServletRequest req, AuthenticationInfo authInfo) {
        String newPassword = req.getParameter(PAR_NEW_PASSWORD );
        if (newPassword != null && newPassword.length() > 0) {
            authInfo.put("user.newpassword", newPassword);
        }
    }

    private String getSudoCookieValue(HttpServletRequest req) {
        // the current state of impersonation
        String currentSudo = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int i = 0; currentSudo == null && i < cookies.length; i++) {
                if (sudoCookieName.equals(cookies[i].getName())) {
                    currentSudo = unquoteCookieValue(cookies[i].getValue());
                }
            }
        }
        return currentSudo;
    }

    /**
     * Sets the impersonation cookie on the response if impersonation actually
     * changed and returns whether the cookie has been set (or cleared) or not.
     * <p>
     * The current impersonation state is taken from the sudo cookie value
     * while the desired state is taken from the user.impersonation
     * property of the auth info. If they don't match, the sudo cookie
     * is set according to the user.impersonation property where the
     * cookie is actually cleared if the property is <code>null</code>.
     *
     * @param req Providing the current sudo cookie value
     * @param res For setting the sudo cookie
     * @param authInfo Providing information about desired impersonation
     * @return <code>true</code> if the cookie has been set or cleared or
     *         <code>false</code> if the cookie is not modified.
     */
    private boolean setSudoCookie(HttpServletRequest req,
            HttpServletResponse res, AuthenticationInfo authInfo) {
        String sudo = (String) authInfo.get(ResourceResolverFactory.USER_IMPERSONATION);
        String currentSudo = getSudoCookieValue(req);

        // set the (new) impersonation
        final boolean setCookie = sudo != currentSudo;
        if (setCookie) {
            if (sudo == null) {
                // Parameter set to "-" to clear impersonation, which was
                // active due to cookie setting

                // clear impersonation
                this.sendSudoCookie(res, "", 0, req.getContextPath(), authInfo.getUser());

            } else if (currentSudo == null || !currentSudo.equals(sudo)) {
                // Parameter set to a name. As the cookie is not set yet
                // or is set to another name, send the cookie with current sudo

                // (re-)set impersonation
                this.sendSudoCookie(res, sudo, -1, req.getContextPath(),
                        sudo);
            }
        }

        return setCookie;
    }

    /**
     * Returns the path to be used to select the authentication handler to login
     * or logout with.
     * <p>
     * This method uses the {@link Authenticator#LOGIN_RESOURCE} request
     * attribute. If this attribute is not set (or is not a string), the request
     * path info is used. If this is not set either, or is the empty string, "/"
     * is returned.
     *
     * @param request The request providing the request attribute or path info.
     * @return The path as set by the request attribute or the path info or "/"
     *         if neither is set.
     */
    private String getHandlerSelectionPath(HttpServletRequest request) {
        final Object loginPathO = request.getAttribute(Authenticator.LOGIN_RESOURCE);
        String path;
        if (loginPathO instanceof String) {
            path = (String) loginPathO;
            final String ctxPath = request.getContextPath();
            if (ctxPath !=  null && path.startsWith(ctxPath)) {
                path = path.substring(ctxPath.length());
            }
        } else {
            path = request.getPathInfo();
        }

        if (path == null || path.length() == 0) {
            path = "/";
        }

        return path;
    }

    /**
     * If the response has not been committed yet, redirect to target requested
     * by the <code>resource</code> request attribute or parameter. If neither
     * is set to a non-null string, the request is redirected to the context
     * root.
     * <p>
     * The response is not reset though, since the handler may have set states
     * such as an updated HTTP session or some Cookie
     *
     * @param request The request providing the redirect target
     * @param response The response to send the redirect to
     */
    private void redirectAfterLogout(final HttpServletRequest request,
            final HttpServletResponse response) {

        // nothing more to do if the response has already been committed
        if (response.isCommitted()) {
            log.debug("redirectAfterLogout: Response has already been committed, not redirecting");
            return;
        }

        // find the redirect target from the resource attribute or parameter
        // falling back to the request context path (or /) if not set or invalid
        String target = AuthUtil.getLoginResource(request, request.getContextPath());
        if (!AuthUtil.isRedirectValid(request, target)) {
            log.warn("redirectAfterLogout: Desired redirect target '{}' is invalid; redirecting to '/'", target);
            target = "/";
        }

        // redirect to there
        try {
            response.sendRedirect(request.getContextPath() + target);
        } catch (IOException e) {
            log.error("Failed to redirect to the page: " + target, e);
        }
    }

    private void postLoginEvent(final AuthenticationInfo authInfo) {
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(SlingConstants.PROPERTY_USERID, authInfo.getUser());
        properties.put(AuthenticationInfo.AUTH_TYPE, authInfo.getAuthType());

        EventAdmin localEA = this.eventAdmin;
        if (localEA != null) {
            localEA.postEvent(new Event(AuthConstants.TOPIC_LOGIN, properties));
        }
    }

    /**
     * Ensures the cookie value is properly quoted for transmission to the
     * client.
     * <p>
     * The problem is, that the character set of cookie values is limited by
     * RFC 2109 defining that a cookie value must be token or quoted-string
     * according to RFC-2616:
     * <pre>
     * token = 1*<any CHAR except CTLs or separators>
     * separators = "(" | ")" | "<" | ">" | "@"
     *                | "," | ";" | ":" | "\" | <">
     *                | "/" | "[" | "]" | "?" | "="
     *                | "{" | "}" | SP | HT
     *
     * quoted-string = ( <"> *(qdtext | quoted-pair ) <"> )
     * qdtext = <any TEXT except <">>
     * quoted-pair = "\" CHAR
     *
     * @param value The cookie value to quote
     * @return The quoted cookie value
     * @throws IllegalArgumentException If the cookie value is <code>null</code>
     *             or cannot be quoted, primarily because it contains a quote
     *             sign.
     */
    static String quoteCookieValue(final String value) {
        // method is package private to enable unit testing

        if (value == null) {
            throw new IllegalArgumentException("Cookie value may not be null");
        }

        StringBuilder builder = new StringBuilder(value.length() * 2);
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                builder.append("\\\"");
            } else if (c == 127 || (c < 32 && c != '\t')) {
                throw new IllegalArgumentException(
                    "Cookie value may not contain CTL character");
            } else {
                builder.append(c);
            }
        }
        builder.append('"');

        return builder.toString();
    }

    /**
     * Removes (optional) quotes from a cookie value to get the raw value of the
     * cookie.
     *
     * @param value The cookie value to unquote
     * @return The unquoted cookie value
     */
    static String unquoteCookieValue(final String value) {
        // method is package private to enable unit testing

        // return value unmodified if null, empty or not starting with a quote
        if (value == null || value.length() == 0 || value.charAt(0) != '"') {
            return value;
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 1; i < value.length() - 1; i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    private static class SlingAuthenticatorServiceListener implements
            AllServiceListener {

        private final SlingAuthenticator authenticator;

        private final HashMap<Object, AuthenticationRequirementHolder[]> props = new HashMap<Object, AuthenticationRequirementHolder[]>();

        static SlingAuthenticatorServiceListener createListener(
                final BundleContext context,
                final SlingAuthenticator authenticator) {
            SlingAuthenticatorServiceListener listener = new SlingAuthenticatorServiceListener(
                authenticator);
            try {
                final String filter = "(" + AuthConstants.AUTH_REQUIREMENTS + "=*)";
                context.addServiceListener(listener, filter);
                ServiceReference[] refs = context.getAllServiceReferences(null,
                    filter);
                if (refs != null) {
                    for (ServiceReference ref : refs) {
                        listener.addService(ref);
                    }
                }
                return listener;
            } catch (InvalidSyntaxException ise) {
            }
            return null;
        }

        private SlingAuthenticatorServiceListener(
                final SlingAuthenticator authenticator) {
            this.authenticator = authenticator;
        }

        public void serviceChanged(final ServiceEvent event) {
            synchronized ( props ) {
                // modification of service properties, unregistration of the
                // service or service properties does not contain requirements
                // property any longer (new event with type 8 added in OSGi Core
                // 4.2)
                if ((event.getType() & (ServiceEvent.MODIFIED
                    | ServiceEvent.UNREGISTERING | 8)) != 0) {
                    removeService(event.getServiceReference());
                }

                // add requirements for newly registered services and for
                // updated services
                if ((event.getType() & (ServiceEvent.REGISTERED | ServiceEvent.MODIFIED)) != 0) {
                    addService(event.getServiceReference());
                }
            }
        }

        void registerServices() {
            AuthenticationRequirementHolder[][] authReqsList;
            authReqsList = props.values().toArray(new AuthenticationRequirementHolder[props.size()][]);

            for (AuthenticationRequirementHolder[] authReqs : authReqsList) {
                registerService(authReqs);
            }
        }

        private void registerService(
                final AuthenticationRequirementHolder[] authReqs) {
            for (AuthenticationRequirementHolder authReq : authReqs) {
                authenticator.authRequiredCache.addHolder(authReq);
            }
        }

        private void addService(final ServiceReference ref) {
            final String[] authReqPaths = OsgiUtil.toStringArray(ref.getProperty(PAR_AUTH_REQ));

            ArrayList<AuthenticationRequirementHolder> authReqList = new ArrayList<AuthenticationRequirementHolder>();
            for (String authReq : authReqPaths) {
                if (authReq != null && authReq.length() > 0) {
                    authReqList.add(AuthenticationRequirementHolder.fromConfig(
                        authReq, ref));
                }
            }

            final AuthenticationRequirementHolder[] authReqs = authReqList.toArray(new AuthenticationRequirementHolder[authReqList.size()]);

            registerService(authReqs);
            props.put(ref.getProperty(Constants.SERVICE_ID), authReqs);
        }

        private void removeService(final ServiceReference ref) {
            final AuthenticationRequirementHolder[] authReqs = props.remove(ref.getProperty(Constants.SERVICE_ID));
            if (authReqs != null) {
                for (AuthenticationRequirementHolder authReq : authReqs) {
                    authenticator.authRequiredCache.removeHolder(authReq);
                }
            }
        }
    }

    private static class AuthenticationHandlerTracker extends ServiceTracker {

        private final PathBasedHolderCache<AbstractAuthenticationHandlerHolder> authHandlerCache;

        private final HashMap<Object, AbstractAuthenticationHandlerHolder[]> handlerMap = new HashMap<Object, AbstractAuthenticationHandlerHolder[]>();

        AuthenticationHandlerTracker(
                final BundleContext context,
                final PathBasedHolderCache<AbstractAuthenticationHandlerHolder> authHandlerCache) {
            this(context, AuthenticationHandler.SERVICE_NAME, authHandlerCache);
        }

        protected AuthenticationHandlerTracker(
                final BundleContext context,
                final String className,
                final PathBasedHolderCache<AbstractAuthenticationHandlerHolder> authHandlerCache) {
            super(context, className, null);
            this.authHandlerCache = authHandlerCache;

            open();
        }

        @Override
        public Object addingService(ServiceReference reference) {
            final Object service = super.addingService(reference);
            if (service != null) {
                bindAuthHandler(service, reference);
            }
            return service;
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
            unbindAuthHandler(reference);
            if (service != null) {
                bindAuthHandler(service, reference);
            }
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            unbindAuthHandler(reference);
            super.removedService(reference, service);
        }

        protected AbstractAuthenticationHandlerHolder createHolder(
                final String path, final Object handler,
                final ServiceReference serviceReference) {
            return new AuthenticationHandlerHolder(path,
                (AuthenticationHandler) handler, serviceReference);
        }

        private void bindAuthHandler(final Object handler, final ServiceReference ref) {
            final String paths[] = OsgiUtil.toStringArray(ref.getProperty(AuthenticationHandler.PATH_PROPERTY));
            if (paths != null && paths.length > 0) {

                // generate the holders
                ArrayList<AbstractAuthenticationHandlerHolder> holderList = new ArrayList<AbstractAuthenticationHandlerHolder>();
                for (String path : paths) {
                    if (path != null && path.length() > 0) {
                        holderList.add(createHolder(path, handler, ref));
                    }
                }

                // register the holders
                AbstractAuthenticationHandlerHolder[] holders = holderList.toArray(new AbstractAuthenticationHandlerHolder[holderList.size()]);
                for (AbstractAuthenticationHandlerHolder holder : holders) {
                    authHandlerCache.addHolder(holder);
                }

                // keep a copy of them for unregistration later
                synchronized (handlerMap) {
                    handlerMap.put(ref.getProperty(Constants.SERVICE_ID),
                        holders);
                }
            }
        }

        private void unbindAuthHandler(ServiceReference ref) {
            final AbstractAuthenticationHandlerHolder[] holders;
            synchronized (handlerMap) {
                holders = handlerMap.remove(ref.getProperty(Constants.SERVICE_ID));
            }

            if (holders != null) {
                for (AbstractAuthenticationHandlerHolder holder : holders) {
                    authHandlerCache.removeHolder(holder);
                }
            }
        }
    }

    private static class EngineAuthenticationHandlerTracker extends
            AuthenticationHandlerTracker {

        EngineAuthenticationHandlerTracker(
                final BundleContext context,
                final PathBasedHolderCache<AbstractAuthenticationHandlerHolder> authHandlerCache) {
            super(context,
                "org.apache.sling.engine.auth.AuthenticationHandler",
                authHandlerCache);
        }

        @SuppressWarnings("deprecation")
        @Override
        protected AbstractAuthenticationHandlerHolder createHolder(String path,
                Object handler, final ServiceReference serviceReference) {
            return new EngineAuthenticationHandlerHolder(path,
                (org.apache.sling.engine.auth.AuthenticationHandler) handler,
                serviceReference);
        }
    }

}
