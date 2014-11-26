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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.agent.DistributionRequestAuthorizationException;
import org.apache.sling.distribution.agent.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionRequestType;

public class PrivilegeDistributionRequestAuthorizationStrategy implements DistributionRequestAuthorizationStrategy {

    private final String jcrPrivilege;

    public PrivilegeDistributionRequestAuthorizationStrategy(String jcrPrivilege) {
        if (jcrPrivilege == null) {
            throw new IllegalArgumentException("Jcr Privilege is required");
        }

        this.jcrPrivilege = jcrPrivilege;
    }

    public void checkPermission(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionRequestAuthorizationException {
        Session session = resourceResolver.adaptTo(Session.class);

        try {
           if (DistributionRequestType.ADD.equals(distributionRequest.getRequestType())) {
               checkPermissionForAdd(session, distributionRequest.getPaths());
           }
           else if (DistributionRequestType.DELETE.equals(distributionRequest.getRequestType())) {
               checkPermissionForDelete(session, distributionRequest.getPaths());
           }

        }
        catch (RepositoryException e) {
            throw new DistributionRequestAuthorizationException("Not enough privileges");
        }

    }

    private void checkPermissionForAdd(Session session, String[] paths)
            throws RepositoryException, DistributionRequestAuthorizationException {
        AccessControlManager acMgr = session.getAccessControlManager();

        Privilege[] privileges = new Privilege[] { acMgr.privilegeFromName(jcrPrivilege), acMgr.privilegeFromName(Privilege.JCR_READ) };
        for (String path : paths) {
            if(!acMgr.hasPrivileges(path, privileges)) {
                throw new DistributionRequestAuthorizationException("Not enough privileges");
            }
        }

    }

    private void checkPermissionForDelete(Session session, String[] paths)
            throws RepositoryException, DistributionRequestAuthorizationException {
        AccessControlManager acMgr = session.getAccessControlManager();

        Privilege[] privileges = new Privilege[] { acMgr.privilegeFromName(jcrPrivilege), acMgr.privilegeFromName(Privilege.JCR_REMOVE_NODE)  };
        for (String path : paths) {
            if(session.nodeExists(path) && !acMgr.hasPrivileges(path, privileges)) {
                throw new DistributionRequestAuthorizationException("Not enough privileges");
            }
        }

    }


}
