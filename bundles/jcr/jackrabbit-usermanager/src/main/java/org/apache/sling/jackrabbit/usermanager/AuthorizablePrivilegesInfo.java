package org.apache.sling.jackrabbit.usermanager;

import javax.jcr.Session;

public interface AuthorizablePrivilegesInfo {

	/**
	 * Checks whether the current user has been granted privileges
	 * to add a new user.
	 *  
	 * @param jcrSession the JCR session of the current user
	 * @return true if the current user has the privileges, false otherwise
	 */
	boolean canAddUser(Session jcrSession);

	/**
	 * Checks whether the current user has been granted privileges
	 * to add a new group.
	 *  
	 * @param jcrSession the JCR session of the current user
	 * @return true if the current user has the privileges, false otherwise
	 */
	boolean canAddGroup(Session jcrSession);
	
	/**
	 * Checks whether the current user has been granted privileges
	 * to update the properties of the specified user or group.
	 *  
	 * @param jcrSession the JCR session of the current user
	 * @param principalId the user or group id to check
	 * @return true if the current user has the privileges, false otherwise
	 */
	boolean canUpdateProperties(Session jcrSession,
			String principalId);

	/**
	 * Checks whether the current user has been granted privileges
	 * to remove the specified user or group.
	 *  
	 * @param jcrSession the JCR session of the current user
	 * @param principalId the user or group id to check
	 * @return true if the current user has the privileges, false otherwise
	 */
	boolean canRemove(Session jcrSession,
			String principalId);
	
	/**
	 * Checks whether the current user has been granted privileges
	 * to update the membership of the specified group.
	 *  
	 * @param jcrSession the JCR session of the current user
	 * @param groupId the group id to check
	 * @return true if the current user has the privileges, false otherwise
	 */
	boolean canUpdateGroupMembers(Session jcrSession,
			String groupId);

}