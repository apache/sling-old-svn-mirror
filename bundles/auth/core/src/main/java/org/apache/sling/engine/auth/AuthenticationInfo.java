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
package org.apache.sling.engine.auth;

import javax.jcr.Credentials;

/**
 * The <code>AuthenticationInfo</code> defines the data returned from the
 * {@link AuthenticationHandler#authenticate(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * method.
 *
 * @deprecated see {@link AuthenticationHandler}
 */
public class AuthenticationInfo {

    /**
     * This object is returned by the
     * {@link AuthenticationHandler#authenticate(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * method to indicate an ongoing authentication transaction.
     */
    public static final AuthenticationInfo DOING_AUTH = new AuthenticationInfo();

    /** The type of authentication */
    private final String authType;

    /** The <code>javax.jcr.Credentials</code> extracted from the request */
    private final Credentials credentials;

    /**
     * The name of the workspace this user is wishing to login,
     * <code>null</code> means the default workspace.
     */
    private final String workspaceName;

    /** Creates an empty instance, used for the {@link #DOING_AUTH} constants */
    private AuthenticationInfo() {
        this(null, null, null);
    }

    /**
     * Creates an instance of this class with the given authentication type and
     * credentials connecting to the default workspace as if the
     * {@link #AuthenticationInfo(String, Credentials, String)} method would be
     * called with a <code>null</code> workspace name.
     *
     * @param authType The authentication type, must not be <code>null</code>.
     * @param credentials The credentials, must not be <code>null</code>.
     * @see #getAuthType()
     * @see #getCredentials()
     */
    public AuthenticationInfo(String authType, Credentials credentials) {
        this(authType, credentials, null);
    }

    /**
     * Creates an instance of this class with the given authentication type and
     * credentials.
     *
     * @param authType The authentication type, must not be <code>null</code>.
     * @param credentials The credentials, must not be <code>null</code>.
     * @param workspaceName The name of the workspace to connect to, may be
     *            <code>null</code> to connect to the default workspace.
     * @see #getAuthType()
     * @see #getCredentials()
     */
    public AuthenticationInfo(String authType, Credentials credentials,
            String workspaceName) {
        this.authType = authType;
        this.credentials = credentials;
        this.workspaceName = workspaceName;
    }

    /**
     * Returns type of authentication provisioning.
     * <p>
     * If authentication is taking place through one of the standard ways, such
     * as Basic or Digest, the return value is one of the predefined constants
     * of the <code>HttpServletRequest</code> interface. Otherwise the value
     * may be specific to the {@link AuthenticationHandler} implementation.
     */
    public String getAuthType() {
        return authType;
    }

    /**
     * Returns the credentials extracted from the client request to use for
     * authentication.
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the name of the workspace the user contained in this instance
     * wishes to connect to. This may be <code>null</code>, in which case the
     * user is connected to the default workspace.
     */
    public String getWorkspaceName() {
        return workspaceName;
    }
}
