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

import java.security.Principal;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.callback.CallbackHandler;

import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.dyuproject.openid.OpenIdUser;

/**
 * The <code>OpenIDLoginModulePlugin</code> is a simple Sling LoginModulePlugin
 * enabling authentication of OpenID identifiers as Jackrabbit Repository users
 */
class OpenIDLoginModulePlugin implements LoginModulePlugin {

    private final OpenIDAuthenticationHandler authHandler;

    /**
     * Creates an instance of this class and registers it as a
     * <code>LoginModulePlugin</code> service to handle login requests with
     * <code>SimpleCredentials</code> provided by the
     * {@link OpenIDAuthenticationHandler}.
     *
     * @param authHandler The {@link OpenIDAuthenticationHandler} providing
     *            support to validate the credentials
     * @param bundleContext The <code>BundleContext</code> to register the
     *            service
     * @return The <code>ServiceRegistration</code> of the registered service
     *         for the {@link OpenIDAuthenticationHandler} to unregister the
     *         service on shutdown.
     */
    static ServiceRegistration register(
            final OpenIDAuthenticationHandler authHandler,
            final BundleContext bundleContext) {
        OpenIDLoginModulePlugin plugin = new OpenIDLoginModulePlugin(
            authHandler);

        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_DESCRIPTION,
            "LoginModulePlugin Support for OpenIDAuthenticationHandler");
        properties.put(Constants.SERVICE_VENDOR,
            bundleContext.getBundle().getHeaders().get(Constants.BUNDLE_VENDOR));

        return bundleContext.registerService(LoginModulePlugin.class.getName(),
            plugin, properties);
    }

    private OpenIDLoginModulePlugin(
            final OpenIDAuthenticationHandler authHandler) {
        this.authHandler = authHandler;
    }

    /**
     * This implementation does nothing.
     */
    @SuppressWarnings("unchecked")
    public void doInit(final CallbackHandler callbackHandler,
            final Session session, final Map options) {
        return;
    }

    /**
     * Returns <code>true</code> indicating support if the credentials is a
     * <code>SimplerCredentials</code> object and has an authentication data
     * attribute.
     * <p>
     * This method does not validate the data just checks its presence.
     *
     * @see CookieAuthenticationHandler#hasAuthData(Credentials)
     */
    public boolean canHandle(Credentials credentials) {
        return authHandler.getOpenIdUser(credentials) != null;
    }

    /**
     * Returns an authentication plugin which validates the authentication data
     * contained as an attribute in the credentials object. The
     * <code>authenticate</code> method returns <code>true</code> only if
     * authentication data is contained in the credentials (expected because
     * this method should only be called if {@link #canHandle(Credentials)}
     * returns <code>true</code>) and the authentication data is valid.
     */
    public AuthenticationPlugin getAuthentication(final Principal principal,
            final Credentials creds) {
        return new AuthenticationPlugin() {
            public boolean authenticate(Credentials credentials)
                    throws RepositoryException {
                OpenIdUser user = authHandler.getOpenIdUser(credentials);
                if (user != null) {
                    return user.isAssociated();
                }
                throw new RepositoryException(
                    "Can't authenticate credentials of type: "
                        + credentials.getClass());
            }

        };
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
    public void addPrincipals(final Set principals) {
    }

    /**
     * Returns <code>LoginModulePlugin.IMPERSONATION_DEFAULT</code> to indicate
     * that this plugin does not itself handle impersonation requests.
     */
    public int impersonate(final Principal principal,
            final Credentials credentials) {
        return LoginModulePlugin.IMPERSONATION_DEFAULT;
    }

}