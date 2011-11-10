/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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