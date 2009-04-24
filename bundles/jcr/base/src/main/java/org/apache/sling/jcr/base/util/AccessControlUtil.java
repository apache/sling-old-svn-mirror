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
import java.security.Principal;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlList;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.internal.PooledSession;

/**
 * A simple utility class providing utilities with respect to 
 * access control over repositories.
 */
public class AccessControlUtil {
	
	// the name of the accessor method for the AccessControlManager
    private static final String METHOD_GET_ACCESS_CONTROL_MANAGER = "getAccessControlManager";
    // the name of the accessor method for the UserManager
    private static final String METHOD_GET_USER_MANAGER = "getUserManager";
    // the name of the accessor method for the PrincipalManager
    private static final String METHOD_GET_PRINCIPAL_MANAGER = "getPrincipalManager";
    // the name of the JackrabbitAccessControlList method getPath
    private static final String METHOD_JACKRABBIT_ACL_GET_PATH = "getPath";
    // the name of the JackrabbitAccessControlList method 
    private static final String METHOD_JACKRABBIT_ACL_IS_EMPTY = "isEmpty";
    // the name of the JackrabbitAccessControlList method 
    private static final String METHOD_JACKRABBIT_ACL_SIZE = "size";
    // the name of the JackrabbitAccessControlList method 
    private static final String METHOD_JACKRABBIT_ACL_ADD_ENTRY = "addEntry";
    // the name of the JackrabbitAccessControlEntry method 
    private static final String METHOD_JACKRABBIT_ACE_IS_ALLOW = "isAllow";


	
    // ---------- SessionImpl methods -----------------------------------------------------
    
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
        
        return safeInvokeRepoMethod(session, METHOD_GET_ACCESS_CONTROL_MANAGER, AccessControlManager.class);
	}
	
	// ---------- JackrabbitSession methods -----------------------------------------------
	
	/**
	 * Returns the <code>UserManager</code> for the given
     * <code>session</code>. If the session does not have a
     * <code>getUserManager</code> method, a
     * <code>UnsupportedRepositoryOperationException</code> is thrown. Otherwise
     * the <code>UserManager</code> is returned or if the call fails,
     * the respective exception is thrown.
	 * 
	 * @param session  The JCR Session whose <code>UserManager</code> is
     *            to be returned. If the session is not a <code>JackrabbitSession</code>
     *            uses reflection to retrive the manager from the repository.
	 * @return The <code>UserManager</code> of the session.
	 * @throws AccessDeniedException If this session is not allowed 
	 * 			  to access user data.
	 * @throws UnsupportedRepositoryOperationException If the session has no
     *            <code>getUserManager</code> method or the exception
     *            thrown by the method.
	 * @throws RepositoryException Forwarded from the
     *             <code>getUserManager</code> method call.
	 */
	public static UserManager getUserManager(Session session) 
										throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
		JackrabbitSession jackrabbitSession = getJackrabbitSession(session);
		if(jackrabbitSession != null) {
			return jackrabbitSession.getUserManager();
		} else {
			return safeInvokeRepoMethod(session, METHOD_GET_USER_MANAGER, UserManager.class);
		}
	}
	
	/**
	 * Returns the <code>PrincipalManager</code> for the given
     * <code>session</code>. If the session does not have a
     * <code>PrincipalManager</code> method, a
     * <code>UnsupportedRepositoryOperationException</code> is thrown. Otherwise
     * the <code>PrincipalManager</code> is returned or if the call fails,
     * the respective exception is thrown.
	 * 
	 * @param session  The JCR Session whose <code>PrincipalManager</code> is
     *            to be returned. If the session is not a <code>JackrabbitSession</code>
     *            uses reflection to retrive the manager from the repository.
	 * @return The <code>PrincipalManager</code> of the session.
	 * @throws AccessDeniedException
	 * @throws UnsupportedRepositoryOperationException If the session has no 
	 * 				<code>PrincipalManager</code> method or the exception
     *             	thrown by the method.
	 * @throws RepositoryException Forwarded from the
     *             <code>PrincipalManager</code> method call.
	 */
	public static PrincipalManager getPrincipalManager(Session session)
										throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
		JackrabbitSession jackrabbitSession = getJackrabbitSession(session);
		if(jackrabbitSession != null) {
			return jackrabbitSession.getPrincipalManager();
		} else {
			return safeInvokeRepoMethod(session, METHOD_GET_PRINCIPAL_MANAGER, PrincipalManager.class);
		}
	}
	
	// ---------- AccessControlList methods -----------------------------------------------
	
	/**
	 * Returns the path of the node <code>AccessControlList</code> acl
	 * has been created for.
	 */
	public static String getPath(AccessControlList acl) throws RepositoryException {
			return safeInvokeRepoMethod(acl, METHOD_JACKRABBIT_ACL_GET_PATH, String.class);
	}
	
	/**
	 * Returns <code>true</code> if <code>AccessControlList</code> acl
	 * does not yet define any entries.
	 */
    public static boolean isEmpty(AccessControlList acl) throws RepositoryException {
		return safeInvokeRepoMethod(acl, METHOD_JACKRABBIT_ACL_IS_EMPTY, Boolean.class);
    }
    
    /**
     * Returns the number of acl entries or 0 if the acl is empty.
     */
    public static int size(AccessControlList acl) throws RepositoryException {
		return safeInvokeRepoMethod(acl, METHOD_JACKRABBIT_ACL_SIZE, Integer.class);
    }
    
    /**
     * Same as {@link #addEntry(AccessControlList, Principal, Privilege[], boolean, Map)} using
     * some implementation specific restrictions.
     */
    @SuppressWarnings("unchecked")
	public static boolean addEntry(AccessControlList acl, Principal principal, Privilege privileges[], boolean isAllow)
        							throws AccessControlException, RepositoryException {
    	Object[] args = new Object[] {principal, privileges, isAllow};
    	Class[] types = new Class[] {Principal.class, Privilege[].class, boolean.class};
		return safeInvokeRepoMethod(acl, METHOD_JACKRABBIT_ACL_ADD_ENTRY, Boolean.class, args, types);
    }
    
    /**
     * Adds an access control entry to the acl consisting of the specified
     * <code>principal</code>, the specified <code>privileges</code>, the
     * <code>isAllow</code> flag and an optional map containing additional
     * restrictions.
     * <p/>
     * This method returns <code>true</code> if this policy was modified,
     * <code>false</code> otherwise.
     */
    @SuppressWarnings("unchecked")
	public static boolean addEntry(AccessControlList acl, Principal principal, Privilege privileges[], boolean isAllow, Map restrictions)
    															throws UnsupportedRepositoryOperationException, RepositoryException {
    	Object[] args = new Object[] {principal, privileges, isAllow, restrictions};
    	Class[] types = new Class[] {Principal.class, Privilege[].class, boolean.class, Map.class};
		return safeInvokeRepoMethod(acl, METHOD_JACKRABBIT_ACL_ADD_ENTRY, Boolean.class, args, types);
    }
    
    // ---------- AccessControlEntry methods -----------------------------------------------
    
    /**
     * Returns true if the AccessControlEntry represents 'allowed' rights or false 
     * it it represents 'denied' rights.
     */
    public static boolean isAllow(AccessControlEntry ace) throws RepositoryException {
		return safeInvokeRepoMethod(ace, METHOD_JACKRABBIT_ACE_IS_ALLOW, Boolean.class);
    }
    
    // ---------- internal -----------------------------------------------------
    
    /**
     * Use reflection to invoke a repository method.
     */
    @SuppressWarnings("unchecked")
	private static <T> T safeInvokeRepoMethod(Object target, String methodName, Class<T> returnType, Object[] args, Class[] argsTypes) 
    													throws UnsupportedRepositoryOperationException, RepositoryException {
    	try {
    		Method m = target.getClass().getMethod(methodName, argsTypes);
    		if (!m.isAccessible()) {
    			m.setAccessible(true);
    		}
			return (T) m.invoke(target, args);
    	} catch (InvocationTargetException ite) {
            // wraps the exception thrown by the method
            Throwable t = ite.getCause();
            if (t instanceof UnsupportedRepositoryOperationException) {
                throw (UnsupportedRepositoryOperationException) t;
            } else if (t instanceof AccessDeniedException) {
                throw (AccessDeniedException) t;
            } else if (t instanceof AccessControlException) {
                throw (AccessControlException) t;
            } else if (t instanceof RepositoryException) {
                throw (RepositoryException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new RepositoryException(methodName, t);
            }
        } catch (Throwable t) {
            // any other problem is just encapsulated
            throw new RepositoryException(methodName, t);
        }
	}
    
    private static <T> T safeInvokeRepoMethod(Object target, String methodName, Class<T> returnType, Object... args) 
																		throws UnsupportedRepositoryOperationException, RepositoryException {
    	return safeInvokeRepoMethod(target, methodName, returnType, args, new Class[0]);
    }
    
    /**
     * Unwrap the jackrabbit session.
     */
	private static JackrabbitSession getJackrabbitSession(Session session) {
		if (session instanceof JackrabbitSession)
			return (JackrabbitSession) session;
		else 
			return null;
	}
}
