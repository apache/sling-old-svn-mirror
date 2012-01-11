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
package org.apache.sling.auth.core.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.osgi.framework.ServiceReference;

/**
 * The <code>AbstractAuthenticationHandlerHolder</code> is a base class to
 * represent authentication handlers (both legacy and new ones) for use in the
 * {@link SlingAuthenticator}.
 */
public abstract class AbstractAuthenticationHandlerHolder extends
        PathBasedHolder implements AuthenticationHandler {

    protected AbstractAuthenticationHandlerHolder(final String fullPath,
            final ServiceReference serviceReference) {
        super(fullPath, serviceReference);
    }

    /**
     * Sets the {@link AuthenticationHandler#PATH_PROPERTY} request attribute to
     * this {@link PathBasedHolder#fullPath} and calls the
     * {@link #extractCredentials(HttpServletRequest, HttpServletResponse)} to
     * have the credentials extracted from the request.
     *
     * @param request the current request
     * @param response the current response
     * @returns the result of calling
     *          {@link #doExtractCredentials(HttpServletRequest, HttpServletResponse)}
     */
    public final AuthenticationInfo extractCredentials(
            HttpServletRequest request, HttpServletResponse response) {

        final Object oldPathAttr = setPath(request);
        try {
            return doExtractCredentials(request, response);
        } finally {
            resetPath(request, oldPathAttr);
        }

    }

    /**
     * Sets the {@link AuthenticationHandler#PATH_PROPERTY} request attribute to
     * this {@link PathBasedHolder#fullPath} and calls the
     * {@link #doRequestCredentials(HttpServletRequest, HttpServletResponse)} to
     * have the credentials requested from the client.
     *
     * @param request the current request
     * @param response the current response
     * @returns the result of calling
     *          {@link #doRequestCredentials(HttpServletRequest, HttpServletResponse)}
     * @throws IOException if an error occurs interacting with the client
     */
    public final boolean requestCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        final Object oldPathAttr = setPath(request);
        try {
            return doRequestCredentials(request, response);
        } finally {
            resetPath(request, oldPathAttr);
        }
    }

    /**
     * Sets the {@link AuthenticationHandler#PATH_PROPERTY} request attribute to
     * this {@link PathBasedHolder#fullPath} and calls the
     * {@link #doDropCredentials(HttpServletRequest, HttpServletResponse)} to
     * have the credentials dropped by the held authentication handler.
     *
     * @param request the current request
     * @param response the current response
     * @throws IOException if an error occurs interacting with the client
     */
    public final void dropCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        final Object oldPathAttr = setPath(request);
        try {
            doDropCredentials(request, response);
        } finally {
            resetPath(request, oldPathAttr);
        }
    }

    // --------- API to be implemented

    /**
     * Returns a feedback handler provided by the authentication handler held by
     * this instance or <code>null</code> if none is provided.
     */
    protected abstract AuthenticationFeedbackHandler getFeedbackHandler();

    /**
     * Calls the actual authentication handler to extract the credentials from
     * the request.
     *
     * @param request The current request
     * @param response The current response
     * @return as returned from the called authentication handler
     * @see #extractCredentials(HttpServletRequest, HttpServletResponse)
     */
    protected abstract AuthenticationInfo doExtractCredentials(
            HttpServletRequest request, HttpServletResponse response);

    /**
     * Calls the actual authentication handler to request the credentials from
     * the client.
     *
     * @param request The current request
     * @param response The current response
     * @return as returned from the called authentication handler
     * @throws IOException if an error occurs sending back any response to the
     *             client.
     * @see #requestCredentials(HttpServletRequest, HttpServletResponse)
     */
    protected abstract boolean doRequestCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    /**
     * Calls the actual authentication handler to request the credentials from
     * the client.
     *
     * @param request The current request
     * @param response The current response
     * @throws IOException if an error occurs sending back any response to the
     *             client.
     * @see #dropCredentials(HttpServletRequest, HttpServletResponse)
     */
    protected abstract void doDropCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    // ---------- internal

    /**
     * Sets the {@link PathBasedHolder#fullPath} as the
     * {@link AuthenticationHandler#PATH_PROPERTY} request attribute.
     */
    private Object setPath(final HttpServletRequest request) {
        return setRequestAttribute(request,
            AuthenticationHandler.PATH_PROPERTY, fullPath);
    }

    /**
     * Sets the given <code>odlValue</code> as the
     * {@link AuthenticationHandler#PATH_PROPERTY} request attribute.
     */
    private void resetPath(final HttpServletRequest request, Object oldValue) {
        setRequestAttribute(request, AuthenticationHandler.PATH_PROPERTY,
            oldValue);
    }

    /**
     * Sets the named request attribute to the new value and returns the
     * previous value.
     *
     * @param request The request object whose attribute is to be set.
     * @param name The name of the attribute to be set.
     * @param value The new value of the attribute. If this is <code>null</code>
     *            the attribute is actually removed from the request.
     * @return The previous value of the named request attribute or
     *         <code>null</code> if it was not set.
     */
    private static Object setRequestAttribute(HttpServletRequest request,
            String name, Object value) {
        Object oldValue = request.getAttribute(name);
        if (value == null) {
            request.removeAttribute(name);
        } else {
            request.setAttribute(name, value);
        }
        return oldValue;
    }

}