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
package org.apache.sling.jcr.base.internal;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;

/**
 * The <code>PooledJackrabbitSession</code> class implements the Jackrabbit 1.5
 * <code>JackrabbitSession</code> interface as a wrapper to a delegatee session.
 * Methods are just delegated to the delegatee session.
 */
public class PooledJackrabbitSession extends PooledSession implements
        JackrabbitSession {

    public PooledJackrabbitSession(SessionPool sessionPool,
            JackrabbitSession delegatee) {
        super(sessionPool, delegatee);
    }

    /**
     * Returns the <code>PrincipalManager</code> of the underlying Jackrabbit
     * Session.
     */
    public PrincipalManager getPrincipalManager() throws AccessDeniedException,
            UnsupportedRepositoryOperationException, RepositoryException {
        return ((JackrabbitSession) getSession()).getPrincipalManager();
    }

    /**
     * Returns the <code>UserManager</code> of the underlying Jackrabbit
     * Session.
     */
    public UserManager getUserManager() throws AccessDeniedException,
            UnsupportedRepositoryOperationException, RepositoryException {
        return ((JackrabbitSession) getSession()).getUserManager();
    }
}
