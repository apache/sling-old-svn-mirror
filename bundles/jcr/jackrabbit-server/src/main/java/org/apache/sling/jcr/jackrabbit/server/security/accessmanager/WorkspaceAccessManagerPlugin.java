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

import javax.jcr.RepositoryException;

/**
 * An <code>AccessManagerPlugin</code> can define its own <code>WorkspaceAccessManagerPlugin</code>,
 * if desired.
 * @see org.apache.sling.jcr.jackrabbit.server.security.accessmanager.AccessManagerPlugin#getWorkspaceAccessManager()
 */
public interface WorkspaceAccessManagerPlugin {

    /**
     * Determines whether the subject of the current context is granted access
     * to the given workspace. Note that an implementation is free to test for
     * the existance of a workspace with the specified name. In this case
     * the expected return value is <code>false</code>, if no such workspace
     * exists.
     *
     * @param workspaceName name of workspace
     * @return <code>true</code> if the subject of the current context is
     *         granted access to the given workspace; otherwise <code>false</code>.
     * @throws javax.jcr.RepositoryException if an error occurs.
     */
    boolean canAccess(String workspaceName) throws RepositoryException;
}
