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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

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

    private static final Logger log = LoggerFactory.getLogger(AccessControlUtil.class);

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

    /**
     * Replaces existing access control entries in the ACL for the specified
     * <code>principal</code> and <code>resourcePath</code>. Any existing granted
     * or denied privileges which do not conflict with the specified privileges
     * are maintained. Where conflicts exist, existing privileges are dropped.
     * The end result will be at most two ACEs for the principal: one for grants
     * and one for denies. Aggregate privileges are disaggregated before checking
     * for conflicts.
     * @param session
     * @param resourcePath
     * @param principal
     * @param grantedPrivilegeNames
     * @param deniedPrivilegeNames
     * @param removedPrivilegeNames privileges which, if they exist, should be
     * removed for this principal and resource
     * @throws RepositoryException
     * @deprecated use @link {@link #replaceAccessControlEntry(Session, String, Principal, String[], String[], String[], String)} instead.
     */
    public static void replaceAccessControlEntry(Session session, String resourcePath, Principal principal, 
			String[] grantedPrivilegeNames, String[] deniedPrivilegeNames, String[] removedPrivilegeNames)
    		throws RepositoryException {
    	replaceAccessControlEntry(session, 
    			resourcePath, 
    			principal, 
    			grantedPrivilegeNames, 
    			deniedPrivilegeNames, 
    			removedPrivilegeNames, 
    			null);
    }    
    
    /**
     * Replaces existing access control entries in the ACL for the specified
     * <code>principal</code> and <code>resourcePath</code>. Any existing granted
     * or denied privileges which do not conflict with the specified privileges
     * are maintained. Where conflicts exist, existing privileges are dropped.
     * The end result will be at most two ACEs for the principal: one for grants
     * and one for denies. Aggregate privileges are disaggregated before checking
     * for conflicts.
     * @param session
     * @param resourcePath
     * @param principal
     * @param grantedPrivilegeNames
     * @param deniedPrivilegeNames
     * @param removedPrivilegeNames privileges which, if they exist, should be
     * removed for this principal and resource
     * @param order where the access control entry should go in the list.  
     *         Value should be one of these:
     *         <table>
     *          <tr><td>null</td><td>If the ACE for the principal doesn't exist add at the end, otherwise leave the ACE at it's current position.</td></tr>
     * 			<tr><td>first</td><td>Place the target ACE as the first amongst its siblings</td></tr>
	 *			<tr><td>last</td><td>Place the target ACE as the last amongst its siblings</td></tr>
	 * 			<tr><td>before xyz</td><td>Place the target ACE immediately before the sibling whose name is xyz</td></tr>
	 * 			<tr><td>after xyz</td><td>Place the target ACE immediately after the sibling whose name is xyz</td></tr>
	 * 			<tr><td>numeric</td><td>Place the target ACE at the specified numeric index</td></tr>
	 *         </table>
     * @throws RepositoryException
     */
    public static void replaceAccessControlEntry(Session session, String resourcePath, Principal principal, 
    			String[] grantedPrivilegeNames, String[] deniedPrivilegeNames, String[] removedPrivilegeNames,
    			String order)
        		throws RepositoryException {
    	AccessControlManager accessControlManager = getAccessControlManager(session);
    	Set<String> specifiedPrivilegeNames = new HashSet<String>();
    	Set<String> newGrantedPrivilegeNames = disaggregateToPrivilegeNames(accessControlManager, grantedPrivilegeNames, specifiedPrivilegeNames);
    	Set<String> newDeniedPrivilegeNames = disaggregateToPrivilegeNames(accessControlManager, deniedPrivilegeNames, specifiedPrivilegeNames);
    	disaggregateToPrivilegeNames(accessControlManager, removedPrivilegeNames, specifiedPrivilegeNames);

    	// Get or create the ACL for the node.
    	AccessControlList acl = null;
    	AccessControlPolicy[] policies = accessControlManager.getPolicies(resourcePath);
    	for (AccessControlPolicy policy : policies) {
    		if (policy instanceof AccessControlList) {
    			acl = (AccessControlList) policy;
    			break;
    		}
    	}
    	if (acl == null) {
    		AccessControlPolicyIterator applicablePolicies = accessControlManager.getApplicablePolicies(resourcePath);
    		while (applicablePolicies.hasNext()) {
    			AccessControlPolicy policy = applicablePolicies.nextAccessControlPolicy();
    			if (policy instanceof AccessControlList) {
    				acl = (AccessControlList) policy;
    				break;
    			}
    		}
    	}
    	if (acl == null) {
    		throw new RepositoryException("Could not obtain ACL for resource " + resourcePath);
    	}
    	// Used only for logging.
    	Set<Privilege> oldGrants = null;
    	Set<Privilege> oldDenies = null;
    	if (log.isDebugEnabled()) {
    		oldGrants = new HashSet<Privilege>();
    		oldDenies = new HashSet<Privilege>();
    	}
      
    	// Combine all existing ACEs for the target principal.
    	AccessControlEntry[] accessControlEntries = acl.getAccessControlEntries();
    	for (int i=0; i < accessControlEntries.length; i++) {
    		AccessControlEntry ace = accessControlEntries[i];
    		if (principal.equals(ace.getPrincipal())) {
    			if (log.isDebugEnabled()) {
    				log.debug("Found Existing ACE for principal {} on resource {}", new Object[] {principal.getName(), resourcePath});
    			}
    			if (order == null || order.length() == 0) {
    				//order not specified, so keep track of the original ACE position.
    				order = String.valueOf(i);
    			}
    			
    			boolean isAllow = isAllow(ace);
    			Privilege[] privileges = ace.getPrivileges();
    			if (log.isDebugEnabled()) {
    				if (isAllow) {
    					oldGrants.addAll(Arrays.asList(privileges));
    				} else {
    					oldDenies.addAll(Arrays.asList(privileges));
    				}
    			}
    			for (Privilege privilege : privileges) {
    				Set<String> maintainedPrivileges = disaggregateToPrivilegeNames(privilege);
    				// If there is any overlap with the newly specified privileges, then
    				// break the existing privilege down; otherwise, maintain as is.
    				if (!maintainedPrivileges.removeAll(specifiedPrivilegeNames)) {
    					// No conflicts, so preserve the original.
    					maintainedPrivileges.clear();
    					maintainedPrivileges.add(privilege.getName());
    				}
    				if (!maintainedPrivileges.isEmpty()) {
    					if (isAllow) {
    						newGrantedPrivilegeNames.addAll(maintainedPrivileges);
    					} else {
    						newDeniedPrivilegeNames.addAll(maintainedPrivileges);
    					}
    				}
    			}
    			// Remove the old ACE.
    			acl.removeAccessControlEntry(ace);
    		}
    	}

    	//add a fresh ACE with the granted privileges
    	List<Privilege> grantedPrivilegeList = new ArrayList<Privilege>();
    	for (String name : newGrantedPrivilegeNames) {
    		Privilege privilege = accessControlManager.privilegeFromName(name);
    		grantedPrivilegeList.add(privilege);
    	}
    	if (grantedPrivilegeList.size() > 0) {
    		acl.addAccessControlEntry(principal, grantedPrivilegeList.toArray(new Privilege[grantedPrivilegeList.size()]));
    	}

   		//add a fresh ACE with the denied privileges
   		List<Privilege> deniedPrivilegeList = new ArrayList<Privilege>();
   		for (String name : newDeniedPrivilegeNames) {
   			Privilege privilege = accessControlManager.privilegeFromName(name);
   			deniedPrivilegeList.add(privilege);
   		}        
   		if (deniedPrivilegeList.size() > 0) {
   			addEntry(acl, principal, deniedPrivilegeList.toArray(new Privilege[deniedPrivilegeList.size()]), false);
   		}

   		
   		//order the ACL
   		reorderAccessControlEntries(acl, principal, order);
   		
    	accessControlManager.setPolicy(resourcePath, acl);
    	if (log.isDebugEnabled()) {
    		List<String> oldGrantedNames = new ArrayList<String>(oldGrants.size());
    		for (Privilege privilege : oldGrants) {
    			oldGrantedNames.add(privilege.getName());
    		}
    		List<String> oldDeniedNames = new ArrayList<String>(oldDenies.size());
    		for (Privilege privilege : oldDenies) {
    			oldDeniedNames.add(privilege.getName());
    		}
    		log.debug("Updated ACE for principalName {} for resource {} from grants {}, denies {} to grants {}, denies {}", new Object [] {
    				principal.getName(), resourcePath, oldGrantedNames, oldDeniedNames, newGrantedPrivilegeNames, newDeniedPrivilegeNames
    			});
    	}
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
  
	/**
	 * Helper routine to transform an input array of privilege names into a set in
	 * a null-safe way while also adding its disaggregated privileges to an input set.
	 */
	private static Set<String> disaggregateToPrivilegeNames(AccessControlManager accessControlManager, 
			String[] privilegeNames, Set<String> disaggregatedPrivilegeNames)
      throws RepositoryException {
		Set<String> originalPrivilegeNames = new HashSet<String>();
		if (privilegeNames != null) {
			for (String privilegeName : privilegeNames) {
				originalPrivilegeNames.add(privilegeName);
				Privilege privilege = accessControlManager.privilegeFromName(privilegeName);
				disaggregatedPrivilegeNames.addAll(disaggregateToPrivilegeNames(privilege));
			}
		}
		return originalPrivilegeNames;
	}

	/**
	 * Transform an aggregated privilege into a set of disaggregated privilege
	 * names. If the privilege is not an aggregate, the set will contain the
	 * original name.
	 */
	private static Set<String> disaggregateToPrivilegeNames(Privilege privilege) {
		Set<String> disaggregatedPrivilegeNames = new HashSet<String>();
		if (privilege.isAggregate()) {
			Privilege[] privileges = privilege.getAggregatePrivileges();
			for (Privilege disaggregate : privileges) {
				if (disaggregate.isAggregate()) {
					continue; //nested aggregate, so skip it since the privileges are already included.
				}
				disaggregatedPrivilegeNames.add(disaggregate.getName());
			}
		} else {
			disaggregatedPrivilegeNames.add(privilege.getName());
		}
		return disaggregatedPrivilegeNames;
	}

	/**
	 * Move the ACE(s) for the specified principal to the position specified by the 'order'
	 * parameter. 
	 * 
	 * @param acl the acl of the node containing the ACE to position
	 * @param principal the user or group of the ACE to position
     * @param order where the access control entry should go in the list.  
     *         Value should be one of these:
     *         <table>
     * 			<tr><td>first</td><td>Place the target ACE as the first amongst its siblings</td></tr>
	 *			<tr><td>last</td><td>Place the target ACE as the last amongst its siblings</td></tr>
	 * 			<tr><td>before xyz</td><td>Place the target ACE immediately before the sibling whose name is xyz</td></tr>
	 * 			<tr><td>after xyz</td><td>Place the target ACE immediately after the sibling whose name is xyz</td></tr>
	 * 			<tr><td>numeric</td><td>Place the target ACE at the specified index</td></tr>
	 *         </table>
	 * @throws RepositoryException 
	 * @throws UnsupportedRepositoryOperationException 
	 * @throws AccessControlException 
	 */
	private static void reorderAccessControlEntries(AccessControlList acl, 
														Principal principal, 
														String order) 
							throws RepositoryException {
		if (order == null || order.length() == 0) {
			return; //nothing to do
		}
		if (acl instanceof JackrabbitAccessControlList) {
			JackrabbitAccessControlList jacl = (JackrabbitAccessControlList)acl;
			
			AccessControlEntry[] accessControlEntries = jacl.getAccessControlEntries();
			if (accessControlEntries.length <= 1) {
				return; //only one ACE, so nothing to reorder.
			}

			AccessControlEntry beforeEntry = null;
			if ("first".equals(order)) {
				beforeEntry = accessControlEntries[0];
			} else if ("last".equals(order)) {
				beforeEntry = null;
			} else if (order.startsWith("before ")) {
				String beforePrincipalName = order.substring(7);
				
				//find the index of the ACE of the 'before' principal
				for (int i=0; i < accessControlEntries.length; i++) {
					if (beforePrincipalName.equals(accessControlEntries[i].getPrincipal().getName())) {
						//found it!
						beforeEntry = accessControlEntries[i];
						break;
					} 
				}
				
				if (beforeEntry == null) {
					//didn't find an ACE that matched the 'before' principal
					throw new IllegalArgumentException("No ACE was found for the specified principal: " + beforePrincipalName);
				}
			} else if (order.startsWith("after ")) {
				String afterPrincipalName = order.substring(6);
				
				//find the index of the ACE of the 'after' principal
				for (int i = accessControlEntries.length - 1; i >= 0; i--) {
					if (afterPrincipalName.equals(accessControlEntries[i].getPrincipal().getName())) {
						//found it!
						
						// the 'before' ACE is the next one after the 'after' ACE
						if (i >= accessControlEntries.length - 1) {
							//the after is the last one in the list
							beforeEntry = null;
						} else {
							beforeEntry = accessControlEntries[i + 1];
						}
						break;
					} 
				}
				
				if (beforeEntry == null) {
					//didn't find an ACE that matched the 'after' principal
					throw new IllegalArgumentException("No ACE was found for the specified principal: " + afterPrincipalName);
				}
			} else {
				try {
					int index = Integer.parseInt(order);
					if (index > accessControlEntries.length) {
						//invalid index
						throw new IndexOutOfBoundsException("Index value is too large: " + index);
					}
					
					if (index == 0) {
						beforeEntry = accessControlEntries[0];
					} else {
						//the index value is the index of the principal.  A principal may have more
						// than one ACEs (deny + grant), so we need to compensate.
						Set<Principal> processedPrincipals = new HashSet<Principal>();
						for (int i = 0; i < accessControlEntries.length; i++) {
							Principal principal2 = accessControlEntries[i].getPrincipal();
							if (processedPrincipals.size() == index &&
									!processedPrincipals.contains(principal2)) {
								//we are now at the correct position in the list
								beforeEntry = accessControlEntries[i];
								break;
							}

							processedPrincipals.add(principal2);
						}					
					}
				} catch (NumberFormatException nfe) {
					//not a number.
					throw new IllegalArgumentException("Illegal value for the order parameter: " + order);
				}
			}
			
			//now loop through the entries to move the affected ACEs to the specified
			// position.
			for (int i = accessControlEntries.length - 1; i >= 0; i--) {
				AccessControlEntry ace = accessControlEntries[i];
				if (principal.equals(ace.getPrincipal())) {
					//this ACE is for the specified principal.
					jacl.orderBefore(ace, beforeEntry);
				}
			}
		} else {
			throw new IllegalArgumentException("The acl must be an instance of JackrabbitAccessControlList");
		}
	}
}
