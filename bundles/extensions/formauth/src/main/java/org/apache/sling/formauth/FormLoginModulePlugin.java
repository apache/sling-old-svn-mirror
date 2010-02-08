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
package org.apache.sling.formauth;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;

final class FormLoginModulePlugin implements LoginModulePlugin {

    private final FormAuthenticationHandler authHandler;

    FormLoginModulePlugin(final FormAuthenticationHandler authHandler) {
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
     * Returns a simple <code>Principal</code> just providing the user id
     * contained in the <code>SimpleCredentials</code> object. If the
     * credentials is not a <code>SimpleCredentials</code> instance,
     * <code>null</code> is returned.
     */
    public Principal getPrincipal(final Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            return new Principal() {
                public String getName() {
                    return ((SimpleCredentials) credentials).getUserID();
                }
            };
        }
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