/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.jackrabbit.server.impl.security;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.jackrabbit.core.security.authentication.DefaultLoginModule;
import org.apache.sling.jcr.jackrabbit.server.impl.Activator;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends
 * {@link org.apache.jackrabbit.core.security.authentication.DefaultLoginModule}
 * to provide implementations registered with the OSGI framework implementing
 * {@link LoginModulePlugin}. Like the DefaultLoginModule, this LoginModule
 * inherits the core of its functionality from
 * {@link org.apache.jackrabbit.core.security.authentication.AbstractLoginModule}
 */
public class PluggableDefaultLoginModule extends DefaultLoginModule {

    private static final Logger log = LoggerFactory.getLogger(PluggableDefaultLoginModule.class);
    /**
     * captured call back hander in use.
     */
    private CallbackHandler pluggableCallackHander;


    /**
     * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#doInit
     */
    @SuppressWarnings("unchecked")
    protected void doInit(CallbackHandler callbackHandler, Session session,
            Map options) throws LoginException {
        LoginModulePlugin[] modules = Activator.getLoginModules();
        for (int i = 0; i < modules.length; i++) {
            modules[i].doInit(callbackHandler, session, options);
        }

        super.doInit(callbackHandler, session, options);
        this.pluggableCallackHander = callbackHandler;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.jackrabbit.core.security.authentication.AbstractLoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        CallbackHandlerWrapper wrappedCallbackHandler = new CallbackHandlerWrapper(subject, callbackHandler);

        super.initialize(subject, wrappedCallbackHandler, sharedState, options);
    }

    /**
     * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#getPrincipal
     */
    protected Principal getPrincipal(Credentials creds) {
        if ( creds instanceof TrustedCredentials ) {
            return ((TrustedCredentials) creds).getPrincipal();
        }
        LoginModulePlugin[] modules = Activator.getLoginModules();
        for (int i = 0; i < modules.length; i++) {
            if (modules[i].canHandle(creds)) {
                Principal p = modules[i].getPrincipal(creds);
                if (p != null) {
                    return p;
                }
            }
        }

        return super.getPrincipal(creds);
    }

    /**
     * @see org.apache.jackrabbit.core.security.authentication.AbstractLoginModule#getPrincipals
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Set getPrincipals() {
        Set principals = super.getPrincipals();
        LoginModulePlugin[] modules = Activator.getLoginModules();
        for (int i = 0; i < modules.length; i++) {
            modules[i].addPrincipals(principals);
        }
        return principals;
    }

    /**
     * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#getAuthentication
     */
    protected Authentication getAuthentication(Principal principal,
            Credentials creds) throws RepositoryException {
        if ( creds instanceof TrustedCredentials ) {
            return ((TrustedCredentials) creds).getTrustedAuthentication();
        }
        LoginModulePlugin[] modules = Activator.getLoginModules();
        for (int i = 0; i < modules.length; i++) {
            if (modules[i].canHandle(creds)) {
                AuthenticationPlugin pa = modules[i].getAuthentication(
                    principal, creds);
                if (pa != null) {
                    return new AuthenticationPluginWrapper(pa, modules[i]);
                }
            }
        }

        return super.getAuthentication(principal, creds);
    }

    /**
     * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#impersonate
     */
    protected boolean impersonate(Principal principal, Credentials creds)
            throws RepositoryException, FailedLoginException {
        if ( creds instanceof AdministrativeCredentials ) {
            return true;
        }
        if ( creds instanceof AnonCredentials ) {
            return false;
        }

        LoginModulePlugin[] modules = Activator.getLoginModules();
        for (int i = 0; i < modules.length; i++) {
            if (modules[i].canHandle(creds)) {
                int result = modules[i].impersonate(principal, creds);
                if (result != LoginModulePlugin.IMPERSONATION_DEFAULT) {
                    return result == LoginModulePlugin.IMPERSONATION_SUCCESS;
                }
            }
        }

        return super.impersonate(principal, creds);
    }


    /**
     * Since the AbstractLoginModule getCredentials does not know anything about TrustedCredentials we have to re-try here.
     */
    @Override
    protected Credentials getCredentials() {
        Credentials creds = super.getCredentials();
        if ( creds == null ) {
            CredentialsCallback callback = new CredentialsCallback();
            try {
                pluggableCallackHander.handle(new Callback[]{callback});
                Credentials callbackCreds = callback.getCredentials();
                if ( callbackCreds instanceof TrustedCredentials ) {
                    creds = callbackCreds;
                }
            } catch (UnsupportedCallbackException e) {
                log.warn("Credentials-Callback not supported try Name-Callback");
            } catch (IOException e) {
                log.error("Credentials-Callback failed: " + e.getMessage() + ": try Name-Callback");
            }
        }
        return creds;
    }

    @Override
    protected boolean supportsCredentials(Credentials creds) {
        if (creds instanceof TrustedCredentials) {
            return true;
        }
        return super.supportsCredentials(creds);
    }
    
}
