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
package org.apache.sling.auth.openid;

/**
 * The <code>OpenIDFailure</code> defines the OpenID authentication failure
 * codes which may be set as the
 * <code>{@link OpenIDConstants#OPENID_FAILURE_REASON j_reason}</code> request
 * parameter for the login form.
 * <p>
 * Note that the
 * <code>{@link OpenIDConstants#OPENID_FAILURE_REASON j_reason}</code> request
 * attribute provides the name of the constant, which can be converted to the
 * actual constant by calling the <code>OpenIDFailure.valueOf(String)</code>
 * method. Internationalization is not built into the OpenID authentication
 * handler. Providers of login forms should implement their own mechanism and
 * may either use the constant name or the {@link #toString() message} of the
 * constant as a key for translation.
 */
public enum OpenIDFailure {

    /**
     * Indicates failure to discover an OpenID Provider for the supplied OpenID
     * identifier.
     */
    DISCOVERY("Failed discovering OpenID Provider for identifier"),

    /**
     * Indicates failure to associate with the OpenID provider to validate the
     * identifier.
     */
    ASSOCIATION("Failed associating with OpenID Provider with idenfier"),

    /**
     * Indicates a generic communication problem with the OpenID Provider.
     */
    COMMUNICATION("Generic communication problem"),

    /**
     * Indicates failure of the user to authenticate with the OpenID Provider.
     */
    AUTHENTICATION("Authentication failed"),

    /**
     * Indicates failure of the verification of the supplied authentication
     * information with the OpenID Provider.
     */
    VERIFICATION("Authentication verification failed"),

    /**
     * Indicates failure to find a matching Repository user for the supplied
     * OpenID identifier.
     */
    REPOSITORY("Cannot associate Repository User with OpenID identifier"),

    /**
     * Indicates any other failure during authentication which is not captured
     * by the other failure reasons.
     */
    OTHER("Generic OpenID authentication failure");

    // The user readable (english) error message
    private final String message;

    /**
     * Creates an instance of the reason conveying the given descriptive reason.
     *
     * @param message The descriptive reason.
     */
    private OpenIDFailure(String message) {
        this.message = message;
    }

    /**
     * Returns the message set when constructing this instance. To get the
     * official name call the <code>name()</code> method.
     */
    @Override
    public String toString() {
        return message;
    }
}