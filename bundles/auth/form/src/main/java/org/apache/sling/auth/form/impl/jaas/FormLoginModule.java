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

package org.apache.sling.auth.form.impl.jaas;

import java.util.Collections;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.oak.spi.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.oak.spi.security.authentication.PreAuthenticatedLogin;
import org.apache.sling.auth.form.impl.FormAuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FormLoginModule extends AbstractLoginModule {
    private static final Logger log = LoggerFactory.getLogger(FormLoginModule.class);

    /**
     * The set of supported credentials. required by the {@link org.apache.jackrabbit.oak.spi.security.authentication.AbstractLoginModule}
     */
    private static final Set<Class> SUPPORTED_CREDENTIALS = Collections.<Class>singleton(FormCredentials.class);
    private static final char[] EMPTY_PWD = new char[0];

    /**
     * Extracted userId during login call.
     */
    private String userId;

    @Override
    protected Set<Class> getSupportedCredentials() {
        return SUPPORTED_CREDENTIALS;
    }

    /**
     * The {@link org.apache.sling.auth.form.impl.FormAuthenticationHandler} used to validate the credentials
     * and its contents.
     */
    private final FormAuthenticationHandler authHandler;

    FormLoginModule(FormAuthenticationHandler authHandler) {
        this.authHandler = authHandler;
    }

    @SuppressWarnings("unchecked")
    public boolean login() throws LoginException {
        Credentials credentials = getCredentials();
        if (credentials instanceof FormCredentials) {
            FormCredentials cred = (FormCredentials) credentials;
            userId = cred.getUserId();

            if (!authHandler.isValid(cred)){
                log.debug("Invalid credentials");
                return false;
            }

            if (userId == null) {
                log.debug("Could not extract userId/credentials");
            } else {
                // we just set the login name and rely on the following login modules to populate the subject
                sharedState.put(SHARED_KEY_PRE_AUTH_LOGIN, new PreAuthenticatedLogin(userId));
                sharedState.put(SHARED_KEY_CREDENTIALS, new SimpleCredentials(userId, EMPTY_PWD));
                sharedState.put(SHARED_KEY_LOGIN_NAME, userId);
                log.debug("login succeeded with trusted user: {}", userId);
            }
        }
        return false;
    }

    public boolean commit() throws LoginException {
        if (userId == null) {
            // login attempt in this login module was not successful
            clearState();
        }
        return false;
    }

    @Override
    protected void clearState() {
        userId = null;
        super.clearState();
    }
}
