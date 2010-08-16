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
package org.apache.sling.auth.form.impl;

import java.security.Principal;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.security.auth.callback.CallbackHandler;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>FormLoginModulePlugin</code> is a LoginModulePlugin which handles
 * <code>SimpleCredentials</code> attributed with the special authentication
 * data provided by the {@link FormAuthenticationHandler}.
 * <p>
 * This class is instantiated by the {@link FormAuthenticationHandler} calling
 * the {@link #register(FormAuthenticationHandler, BundleContext)} method. If
 * the OSGi framework does not provide the <code>LoginModulePlugin</code>
 * interface (such as when the Sling Jackrabbit Server bundle is not used to
 * provide the JCR Repository), loading this class fails, which is caught by the
 * {@link FormAuthenticationHandler}.
 */
final class FormLoginModulePlugin implements LoginModulePlugin {

    /**
     * The {@link FormAuthenticationHandler} used to validate the credentials
     * and its contents.
     */
    private final FormAuthenticationHandler authHandler;

    /**
     * Creates an instance of this class and registers it as a
     * <code>LoginModulePlugin</code> service to handle login requests with
     * <code>SimpleCredentials</code> provided by the
     * {@link FormAuthenticationHandler}.
     *
     * @param authHandler The {@link FormAuthenticationHandler} providing
     *            support to validate the credentials
     * @param bundleContext The <code>BundleContext</code> to register the
     *            service
     * @return The <code>ServiceRegistration</code> of the registered service for
     *         the {@link FormAuthenticationHandler} to unregister the service
     *         on shutdown.
     */
    static ServiceRegistration register(
            final FormAuthenticationHandler authHandler,
            final BundleContext bundleContext) {
        FormLoginModulePlugin plugin = new FormLoginModulePlugin(authHandler);

        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_DESCRIPTION,
            "LoginModulePlugin Support for FormAuthenticationHandler");
        properties.put(Constants.SERVICE_VENDOR,
            bundleContext.getBundle().getHeaders().get(Constants.BUNDLE_VENDOR));

        return bundleContext.registerService(LoginModulePlugin.class.getName(),
            plugin, properties);
    }

    /**
     * Private constructor called from
     * {@link #register(FormAuthenticationHandler, BundleContext)} to create an
     * instance of this class.
     *
     * @param authHandler The {@link FormAuthenticationHandler} used to validate
     *            the credentials attribute
     */
    private FormLoginModulePlugin(final FormAuthenticationHandler authHandler) {
        this.authHandler = authHandler;
    }

    /**
     * Returns <code>true</code> indicating support if the credentials is a
     * <code>SimplerCredentials</code> object and has an authentication data
     * attribute.
     *
     * @see CookieAuthenticationHandler#hasAuthData(Credentials)
     */
    public boolean canHandle(Credentials credentials) {
        return authHandler.hasAuthData(credentials);
    }

    /**
     * This implementation does nothing.
     */
    @SuppressWarnings("unchecked")
    public void doInit(CallbackHandler callbackHandler, Session session,
            Map options) {
    }

    /**
     * Returns <code>null</code> to have the <code>DefaultLoginModule</code>
     * provide a principal based on an existing user defined in the repository.
     */
    public Principal getPrincipal(final Credentials credentials) {
        return null;
    }

    /**
     * This implementation does nothing.
     */
    @SuppressWarnings("unchecked")
    public void addPrincipals(@SuppressWarnings("unused") Set principals) {
    }

    /**
     * Returns an <code>AuthenticationPlugin</code> which authenticates the
     * credentials if the contain authentication data and the authentication
     * data can is valid.
     *
     * @see CookieAuthenticationHandler#isValid(Credentials)
     */
    public AuthenticationPlugin getAuthentication(Principal principal,
            Credentials creds) {
        return new AuthenticationPlugin() {
            public boolean authenticate(Credentials credentials) {
                return authHandler.isValid(credentials);
            }
        };
    }

    /**
     * Returns <code>LoginModulePlugin.IMPERSONATION_DEFAULT</code> to indicate
     * that this plugin does not itself handle impersonation requests.
     */
    public int impersonate(Principal principal, Credentials credentials) {
        return LoginModulePlugin.IMPERSONATION_DEFAULT;
    }
}