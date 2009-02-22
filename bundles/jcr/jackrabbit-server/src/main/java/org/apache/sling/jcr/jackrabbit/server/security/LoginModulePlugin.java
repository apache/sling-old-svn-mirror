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
package org.apache.sling.jcr.jackrabbit.server.security;

import java.security.Principal;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

/**
 * Provide login module functionality that extends
 * {@link org.apache.jackrabbit.core.security.authentication.DefaultLoginModule}
 * for a specific type of {@link java.jcr.Credentials}. Does not rely explicitly
 * on any classes from org.apache.jackrabbit.core.*
 */
public interface LoginModulePlugin {

    public static final int IMPERSONATION_DEFAULT = 0;

    public static final int IMPERSONATION_SUCCESS = 1;

    public static final int IMPERSONATION_FAILED = 2;

    /**
     * Determine if this LoginModule can process this set of Credentials.
     * Currently, due to Jackrabbit internals, these Credentials will always be
     * an instance of {@link javax.jcr.SimpleCredentials}. A co-operating
     * {@link org.apache.sling.engine.auth.AuthenticationHandler} object can set
     * properties on these credentials at creation time that this class can use
     * to make this determination
     * 
     * @param credentials
     * @return
     */
    public abstract boolean canHandle(Credentials credentials);

    /**
     * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#doInit
     */
    public void doInit(CallbackHandler callbackHandler, Session session,
            Map options) throws LoginException;

    /**
     * Return a Principal object, or null. If null is returned, and no other
     * PluggableLoginModule that can handle these Credentials can provide a
     * Principal, the Principal will be provided by
     * {@link org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#getPrincipal}
     * 
     * @return an instance of the Principal associated with these Credentials
     * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#getPrincipal
     */
    public Principal getPrincipal(Credentials credentials);

    /**
     * Return a PluggableAuthentication object that can authenticate the give
     * Principal and Credentials. If null is returned, and no other
     * PluggableLoginModule that can handle these Credentials can provide a
     * PluggableAuthentication instance, the authentication will be handled by
     * {@link org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#getAuthentication}
     * 
     * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#getAuthentication
     * @return An instance of PluggableAuthentication, or null
     */
    public AuthenticationPlugin getAuthentication(Principal principal,
            Credentials creds) throws RepositoryException;

    /**
     * Returns a code indicating either the status of the impersonation attempt,
     * or {@link IMPERSONATION_DEFAULT} if the impersonation should be handled
     * by
     * {@link org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#impersonate}
     * .
     * 
     * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule#impersonate
     * @return one of {@link IMPERSONATION_DEFAULT},
     *         {@link IMPERSONATION_SUCCESS} or {@link IMPERSONATION_FAILED}
     */
    public int impersonate(Principal principal, Credentials credentials)
            throws RepositoryException, FailedLoginException;

}
