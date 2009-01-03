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
package org.apache.sling.jcr.base.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.sling.jcr.base.internal.PooledSession;

/**
 * The <code>RepositoryUtil</code> is a simple utility class providing utilities
 * with respect to repositories.
 */
public class RepositoryUtil {

    // the name of the accessor method for the AccessControlManager
    private static final String METHOD_GET_ACCESS_CONTROL_MANAGER = "getAccessControlManager";

    /**
     * Returns the <code>AccessControlManager</code> for the given
     * <code>session</code>. If the session does not have a
     * <code>getAccessControlManager</code> method, a
     * <code>UnsupportedRepositoryOperationException</code> is thrown. Otherwise
     * the <code>AccessControlManager</code> is returned or if the call fails,
     * the respective exception is thrown.
     * 
     * @param session The JCR Session whose <code>AccessControlManager</code> is
     *            to be returned. If the session is a pooled session, the
     *            session underlying the pooled session is actually used.
     * @return The <code>AccessControlManager</code> of the session
     * @throws UnsupportedRepositoryOperationException If the session has no
     *             <code>getAccessControlManager</code> method or the exception
     *             thrown by the method.
     * @throws RepositoryException Forwarded from the
     *             <code>getAccessControlManager</code> method call.
     */
    public static AccessControlManager getAccessControlManager(Session session)
            throws UnsupportedRepositoryOperationException, RepositoryException {

        // unwrap a pooled session
        if (session instanceof PooledSession) {
            session = ((PooledSession) session).getSession();
        }

        try {
        
            Method m = session.getClass().getMethod(
                METHOD_GET_ACCESS_CONTROL_MANAGER);
            return (AccessControlManager) m.invoke(session);

        } catch (InvocationTargetException ite) {
            
            // wraps the exception thrown by the method
            
            Throwable t = ite.getCause();
            if (t instanceof UnsupportedRepositoryOperationException) {
                throw (UnsupportedRepositoryOperationException) t;
            } else if (t instanceof RepositoryException) {
                throw (RepositoryException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new UnsupportedRepositoryOperationException(
                    METHOD_GET_ACCESS_CONTROL_MANAGER, t);
            }
            
        } catch (Throwable t) {
         
            // any other problem is just encapsulated
            throw new UnsupportedRepositoryOperationException(
                METHOD_GET_ACCESS_CONTROL_MANAGER, t);
            
        }
    }
}
