/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.jcr.jackrabbit.server.security.accessmanager;

import org.apache.jackrabbit.core.security.authorization.Permission;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

/**
 * A simplified AccessManager interface. 
 */
public interface AccessManagerPlugin {

    public static final int READ = Permission.READ;
    public static final int ADD_NODE = Permission.ADD_NODE;
    public static final int REMOVE_NODE = Permission.REMOVE_NODE;
    public static final int SET_PROPERTY = Permission.SET_PROPERTY;
    public static final int REMOVE_PROPERTY = Permission.REMOVE_PROPERTY;
    public static final int ALL = Permission.ALL;
    public static final int NONE = Permission.NONE;

    /**
     * Initialize this access manager. An <code>AccessDeniedException</code> will
     * be thrown if the subject of the given <code>context</code> is not
     * granted access to the specified workspace.
     *
     * @param subject The authenticated Subject
     * @param session The current JCR session
     */
    void init(Subject subject, Session session) throws AccessDeniedException, Exception;

    /**
     * Close this access manager. After having closed an access manager,
     * further operations on this object are treated as illegal and throw
     *
     * @throws Exception if an error occurs
     */
    void close() throws Exception;

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on the item with the specified <code>absPath</code> (i.e. the <i>target</i>
     * item, that may or may not yet exist).
     *
     * @param absPath     the absolute path to test
     * @param permissions A combination of one or more of the following constants
     *                    encoded as a bitmask value:
     * <ul>
     * <li>{@link org.apache.jackrabbit.core.security.authorization.Permission#READ READ}</li>
     * <li>{@link org.apache.jackrabbit.core.security.authorization.Permission#ADD_NODE ADD_NODE}</code></li>
     * <li>{@link org.apache.jackrabbit.core.security.authorization.Permission#REMOVE_NODE REMOVE_NODE}</li>
     * <li>{@link org.apache.jackrabbit.core.security.authorization.Permission#SET_PROPERTY SET_PROPERTY}</li>
     * <li>{@link org.apache.jackrabbit.core.security.authorization.Permission#REMOVE_PROPERTY REMOVE_PROPERTY}</li>
     * </ul>
     * @return <code>true</code> if the specified permissions are granted;
     * otherwise <code>false</code>.
     * @throws RepositoryException if an error occurs.
     */
    boolean isGranted(String absPath, int permissions) throws RepositoryException;

    /**
     * Determines whether the item at the specified absolute path can be read.
     *
     * @param itemPath Absolute path to the item being accessed
     * @return <code>true</code> if the item can be read; otherwise <code>false</code>.
     * @throws RepositoryException if an error occurs.
     */
    boolean canRead(String itemPath) throws RepositoryException;

    /**
     * Returns the <code>WorkspaceAccessManagerPlugin</code> to be used for checking Workspace access.
     * If <code>null</code> is returned, the default <code>WorkspaceAccessManager</code> will be used.
     * @return An implementation of <code>WorkspaceAccessManagerPlugin</code>, or <code>null</code> to use
     * the default <code>WorkspaceAccessManager</code>.
     *  */
    WorkspaceAccessManagerPlugin getWorkspaceAccessManager();

}
