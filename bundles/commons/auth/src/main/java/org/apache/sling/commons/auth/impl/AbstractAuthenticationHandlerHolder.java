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
package org.apache.sling.commons.auth.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.auth.spi.AuthenticationHandler;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;

/**
 * The <code>AuthenticationHandlerHolder</code> class represents an
 * authentication handler service in the internal data structure of the
 * {@link SlingAuthenticator}.
 *
 * @since 2.1
 */
public abstract class AbstractAuthenticationHandlerHolder extends PathBasedHolder implements AuthenticationHandler {

    protected AbstractAuthenticationHandlerHolder(final String fullPath) {
        super(fullPath);
    }

    protected abstract AuthenticationInfo doAuthenticate(HttpServletRequest request,
            HttpServletResponse response);

    public final AuthenticationInfo authenticate(HttpServletRequest request,
            HttpServletResponse response) {

        final Object oldPathAttr = setPath(request);
        try {
            return doAuthenticate(request, response);
        } finally {
            resetPath(request, oldPathAttr);
        }

    }

    protected abstract boolean doRequestAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    public final boolean requestAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        final Object oldPathAttr = setPath(request);
        try {
            return doRequestAuthentication(request, response);
        } finally {
            resetPath(request, oldPathAttr);
        }
    }

    protected abstract void doDropAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    public final void dropAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        doDropAuthentication(request, response);
    }

    private Object setPath(final HttpServletRequest request) {
        return setRequestAttribute(request,
            AuthenticationHandler.PATH_PROPERTY, fullPath);
    }

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
     *            the attribte is actually removed from the request.
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