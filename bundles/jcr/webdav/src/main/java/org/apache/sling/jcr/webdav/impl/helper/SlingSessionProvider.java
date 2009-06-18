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
package org.apache.sling.jcr.webdav.impl.helper;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.server.SessionProvider;

/**
 * The <code>SlingSessionProvider</code> is a Jackrabbit WebDAV server
 * <code>SessionProvider</code> which returns the session stored as the
 * <code>javax.jcr.Session</code> request attribute. This request attribute is
 * set by the Sling Authenticator when the request is authenticated. If the
 * request is not authenticated, the request attribute is not set and hence no
 * session is returned.
 * <p>
 * This class expects an authenticated request, which is identified by the
 * request authentication type to not be <code>null</code>. Otherwise the
 * {@link #getSession(HttpServletRequest, Repository, String)} method throws a
 * <code>LoginException</code> to force authentication.
 */
public class SlingSessionProvider implements SessionProvider {

    /**
     * The name of the request attribute providing the JCR session (value is
     * "javax.jcr.Session").
     */
    private static final String ATTR_SESSION_NAME = Session.class.getName();

    /**
     * Returns the value of the <code>javax.jcr.Session</code> request
     * attribute or <code>null</code> if the request attribute is not set. If
     * the request is not authenticated, that is the authentication type is
     * <code>null</code>, a <code>LoginException</code> is thrown to force
     * authentication.
     */
    public Session getSession(HttpServletRequest request, Repository rep,
            String workspace) throws LoginException {

        // we do not accept the anonymous session for WebDAV !
        if (request.getAuthType() == null) {
            throw new LoginException("Authentication required for WebDAV");
        }

        // otherwise return the session from the request attribute
        return (Session) request.getAttribute(ATTR_SESSION_NAME);
    }

    /**
     * Does nothing as the session is taken from the Sling request and hence the
     * session will be released by Sling.
     */
    public void releaseSession(Session session) {
        // nothing to do, we must not logout the Sling session
    }

}
