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
package org.apache.sling.distribution.agent.impl;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.common.DistributionException;

/**
 * {@link DistributionRequestAuthorizationStrategy} based on JCR privileges over a certain {@link Session}
 */
public class PrivilegeDistributionRequestAuthorizationStrategy implements DistributionRequestAuthorizationStrategy {

    private final String jcrPrivilege;

    public PrivilegeDistributionRequestAuthorizationStrategy(String jcrPrivilege) {
        if (jcrPrivilege == null) {
            throw new IllegalArgumentException("Jcr Privilege is required");
        }

        this.jcrPrivilege = jcrPrivilege;
    }

    public void checkPermission(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionException {
        Session session = resourceResolver.adaptTo(Session.class);

        if (session == null) {
            throw new DistributionException("cannot obtain a Session");
        }

        try {
            if (DistributionRequestType.ADD.equals(distributionRequest.getRequestType())) {
                checkPermissionForAdd(session, distributionRequest.getPaths());
            } else if (DistributionRequestType.DELETE.equals(distributionRequest.getRequestType())) {
                checkPermissionForDelete(session, distributionRequest.getPaths());
            }

        } catch (RepositoryException e) {
            throw new DistributionException("Not enough privileges");
        }

    }

    private void checkPermissionForAdd(Session session, String[] paths)
            throws RepositoryException, DistributionException {
        AccessControlManager acMgr = session.getAccessControlManager();

        Privilege[] privileges = new Privilege[]{acMgr.privilegeFromName(jcrPrivilege), acMgr.privilegeFromName(Privilege.JCR_READ)};
        for (String path : paths) {
            if (!acMgr.hasPrivileges(path, privileges)) {
                throw new DistributionException("Not enough privileges");
            }
        }

    }

    private void checkPermissionForDelete(Session session, String[] paths)
            throws RepositoryException, DistributionException {
        AccessControlManager acMgr = session.getAccessControlManager();

        Privilege[] privileges = new Privilege[]{acMgr.privilegeFromName(jcrPrivilege), acMgr.privilegeFromName(Privilege.JCR_REMOVE_NODE)};
        for (String path : paths) {

            String closestParentPath = getClosestParent(session, path);

            if (closestParentPath == null || !acMgr.hasPrivileges(closestParentPath, privileges)) {
                throw new DistributionException("Not enough privileges");
            }
        }
    }

    private String getClosestParent(Session session, String path) throws RepositoryException {
        do {
            if (session.nodeExists(path)) {
                return path;
            }
            path = Text.getRelativeParent(path, 1);
        }
        while (path != null && path.length() > 0);

        return null;
    }


}
