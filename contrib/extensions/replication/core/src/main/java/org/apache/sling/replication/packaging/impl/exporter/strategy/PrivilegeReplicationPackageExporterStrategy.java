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
package org.apache.sling.replication.packaging.impl.exporter.strategy;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageExporterStrategy;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.List;
import java.util.Map;

public class PrivilegeReplicationPackageExporterStrategy implements ReplicationPackageExporterStrategy {
    public static final String NAME = "privilege";
    public static final String JCR_PRIVILEGE = "jcrPrivilege";

    private final String jcrPrivilege;

    public PrivilegeReplicationPackageExporterStrategy(Map<String, Object> config) {
        this(PropertiesUtil.toString(config.get(JCR_PRIVILEGE), null));
    }

    public PrivilegeReplicationPackageExporterStrategy(String jcrPrivilege) {
        if (jcrPrivilege == null) {
            throw new IllegalArgumentException("Jcr Privilege is required");
        }

        this.jcrPrivilege = jcrPrivilege;
    }
    public List<ReplicationPackage> exportPackages(ResourceResolver resourceResolver, ReplicationRequest replicationRequest, ReplicationPackageExporter packageExporter) throws ReplicationPackageBuildingException {
        Session session = resourceResolver.adaptTo(Session.class);

        try {
           if (ReplicationActionType.ADD.equals(replicationRequest.getAction())) {
               checkPermissionForAdd(session, replicationRequest.getPaths());
           }
           else if (ReplicationActionType.DELETE.equals(replicationRequest.getAction())) {
               checkPermissionForDelete(session, replicationRequest.getPaths());
           }

        }
        catch (RepositoryException e) {
            throw new ReplicationPackageBuildingException("Not enough privileges");
        }

        return packageExporter.exportPackage(resourceResolver, replicationRequest);

    }

    private void checkPermissionForAdd(Session session, String[] paths)
            throws RepositoryException, ReplicationPackageBuildingException {
        AccessControlManager acMgr = session.getAccessControlManager();

        Privilege[] privileges = new Privilege[] { acMgr.privilegeFromName(jcrPrivilege), acMgr.privilegeFromName(Privilege.JCR_READ) };
        for (String path : paths) {
            if(!acMgr.hasPrivileges(path, privileges)) {
                throw new ReplicationPackageBuildingException("Not enough privileges");
            };
        }

    }

    private void checkPermissionForDelete(Session session, String[] paths)
            throws RepositoryException, ReplicationPackageBuildingException {
        AccessControlManager acMgr = session.getAccessControlManager();

        Privilege[] privileges = new Privilege[] { acMgr.privilegeFromName(jcrPrivilege), acMgr.privilegeFromName(Privilege.JCR_REMOVE_NODE)  };
        for (String path : paths) {
            if(session.nodeExists(path) && !acMgr.hasPrivileges(path, privileges)) {
                throw new ReplicationPackageBuildingException("Not enough privileges");
            };
        }

    }


}
