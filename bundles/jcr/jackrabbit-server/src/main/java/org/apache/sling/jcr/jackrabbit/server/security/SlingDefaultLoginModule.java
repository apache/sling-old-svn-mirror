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

package org.apache.sling.jcr.jackrabbit.server.security;

import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.jackrabbit.core.security.authentication.DefaultLoginModule;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AdministrativeCredentials;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AnonCredentials;
import org.apache.sling.jcr.jackrabbit.server.impl.security.CallbackHandlerWrapper;
import org.apache.sling.jcr.jackrabbit.server.impl.security.TrustedCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

/**
 * User: chetanm
 * Date: 9/9/12
 * Time: 2:45 AM
 */
public class SlingDefaultLoginModule extends DefaultLoginModule{
    private static final Logger log = LoggerFactory.getLogger(SlingDefaultLoginModule.class);
    /**
     * captured call back hander in use.
     */
    private CallbackHandler pluggableCallackHander;
    @Override
    protected Principal getPrincipal(Credentials creds) {
        if ( creds instanceof TrustedCredentials) {
            return ((TrustedCredentials) creds).getPrincipal();
        }
        return super.getPrincipal(creds);
    }

    /**
     * {@inheritDoc}
     * @see org.apache.jackrabbit.core.security.authentication.AbstractLoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        CallbackHandlerWrapper wrappedCallbackHandler = new CallbackHandlerWrapper(subject, callbackHandler);
        this.pluggableCallackHander = callbackHandler;
        super.initialize(subject, wrappedCallbackHandler, sharedState, options);
    }

    @Override
    protected Authentication getAuthentication(Principal principal, Credentials creds) throws RepositoryException {
        if ( creds instanceof TrustedCredentials ) {
            return ((TrustedCredentials) creds).getTrustedAuthentication();
        }
        return super.getAuthentication(principal, creds);
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
    protected boolean impersonate(Principal principal, Credentials creds) throws RepositoryException, FailedLoginException {
        if ( creds instanceof AdministrativeCredentials) {
            return true;
        }
        if ( creds instanceof AnonCredentials) {
            return false;
        }

        return super.impersonate(principal, creds);
    }

    @Override
    protected boolean supportsCredentials(Credentials creds) {
        if (creds instanceof TrustedCredentials) {
            return true;
        }
        return super.supportsCredentials(creds);
    }
}
