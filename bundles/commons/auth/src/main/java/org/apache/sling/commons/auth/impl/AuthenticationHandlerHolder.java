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

import org.apache.sling.commons.auth.spi.AuthenticationFeedbackHandler;
import org.apache.sling.commons.auth.spi.AuthenticationHandler;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;

/**
 * The <code>AuthenticationHandlerHolder</code> class represents an
 * authentication handler service in the internal data structure of the
 * {@link SlingAuthenticator}.
 *
 */
final class AuthenticationHandlerHolder extends
        AbstractAuthenticationHandlerHolder {

    // the actual authentication handler
    private final AuthenticationHandler handler;

    AuthenticationHandlerHolder(final String fullPath,
            final AuthenticationHandler handler) {
        super(fullPath);

        // assign the fields
        this.handler = handler;
    }

    @Override
    protected AuthenticationFeedbackHandler getFeedbackHandler() {
        if (handler instanceof AuthenticationFeedbackHandler) {
            return (AuthenticationFeedbackHandler) handler;
        }
        return null;
    }

    public AuthenticationInfo doExtractCredentials(HttpServletRequest request,
            HttpServletResponse response) {

        return handler.extractCredentials(request, response);

    }

    public boolean doRequestCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        return handler.requestCredentials(request, response);
    }

    public void doDropCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        handler.dropCredentials(request, response);
    }

    @Override
    public boolean equals(Object obj) {

        // equality is the base class equality (based on the fullpath)
        // and the encapsulated holders being the same.
        if (super.equals(obj)) {
            if (obj.getClass() == getClass()) {
                AuthenticationHandlerHolder other = (AuthenticationHandlerHolder) obj;
                return other.handler == handler;
            }
        }

        // handlers are not the same, so the holders are not the same
        return false;
    }

    @Override
    public String toString() {
        return handler.toString();
    }
}