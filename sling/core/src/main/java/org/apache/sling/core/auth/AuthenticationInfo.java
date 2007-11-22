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
package org.apache.sling.core.auth;

import javax.jcr.Credentials;

/**
 * The <code>AuthenticationInfo</code> defines the data returned from the
 * {@link AuthenticationHandler#authenticate(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * method.
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

    /** Creates an empty instance, used for the {@link #DOING_AUTH} constants */
    private AuthenticationInfo() {
        authType = null;
        credentials = null;
    }

    /**
     * Creates an instance of this class with the given authentication type and
     * credentials.
     *
     * @param authType The authentication type, must not be <code>null</code>.
     * @param credentials The credentials, must not be <code>null</code>.
     * @see #getAuthType()
     * @see #getCredentials()
     */
    public AuthenticationInfo(String authType, Credentials credentials) {
        this.authType = authType;
        this.credentials = credentials;
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

}
