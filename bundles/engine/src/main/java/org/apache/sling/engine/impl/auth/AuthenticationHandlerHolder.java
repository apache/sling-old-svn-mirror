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
package org.apache.sling.engine.impl.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.engine.RequestUtil;
import org.apache.sling.engine.auth.AuthenticationHandler;
import org.apache.sling.engine.auth.AuthenticationHandler2;
import org.apache.sling.engine.auth.AuthenticationInfo;

/**
 * The <code>AuthenticationHandlerHolder</code> class represents an
 * authentication handler service in the internal data structure of the
 * {@link SlingAuthenticator}.
 *
 * @since 2.1
 */
final class AuthenticationHandlerHolder implements AuthenticationHandler2,
        Comparable<AuthenticationHandlerHolder> {

    // full path of the service registration
    private final String fullPath;

    // file path part of the service registration full path
    final String path;

    // host element of the service registration full path
    final String host;

    // protocol element of the service registration full path
    final String protocol;

    // the actual authentication handler
    private final AuthenticationHandler handler;

    AuthenticationHandlerHolder(final String fullPath,
            final AuthenticationHandler handler) {

        String path = fullPath;
        String host = "";
        String protocol = "";

        // check for protocol prefix in the full path
        if (path.startsWith("http://") || path.startsWith("https://")) {
            int idxProtocolEnd = path.indexOf("://");
            protocol = path.substring(0, idxProtocolEnd);
            path = path.substring(idxProtocolEnd + 1);
        }

        // check for host prefix in the full path
        if (path.startsWith("//")) {
            int idxHostEnd = path.indexOf("/", 2);
            idxHostEnd = idxHostEnd == -1 ? path.length() : idxHostEnd;

            if (path.length() > 2) {
                host = path.substring(2, idxHostEnd);
                if (idxHostEnd < path.length()) {
                    path = path.substring(idxHostEnd);
                } else {
                    path = "/";
                }
            } else {
                path = "/";
            }
        }

        // assign the fields
        this.fullPath = fullPath;
        this.path = path;
        this.host = host;
        this.protocol = protocol;
        this.handler = handler;
    }

    public AuthenticationInfo authenticate(HttpServletRequest request,
            HttpServletResponse response) {

        final Object oldPathAttr = setPath(request);
        try {
            return handler.authenticate(request, response);
        } finally {
            resetPath(request, oldPathAttr);
        }

    }

    public boolean requestAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        final Object oldPathAttr = setPath(request);
        try {
            return handler.requestAuthentication(request, response);
        } finally {
            resetPath(request, oldPathAttr);
        }
    }

    public void dropAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (handler instanceof AuthenticationHandler2) {
            final Object oldPathAttr = setPath(request);
            try {
                final AuthenticationHandler2 handler2 = (AuthenticationHandler2) handler;
                handler2.dropAuthentication(request, response);
            } finally {
                resetPath(request, oldPathAttr);
            }
        }
    }

    public int compareTo(AuthenticationHandlerHolder other) {
        return other.path.compareTo(path);
    }

    private Object setPath(final HttpServletRequest request) {
        return RequestUtil.setRequestAttribute(request,
            AuthenticationHandler.PATH_PROPERTY, fullPath);
    }

    private void resetPath(final HttpServletRequest request, Object oldValue) {
        RequestUtil.setRequestAttribute(request,
            AuthenticationHandler.PATH_PROPERTY, oldValue);
    }
}