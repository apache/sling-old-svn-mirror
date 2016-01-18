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
package org.apache.sling.distribution.packaging.impl.importer;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.packaging.DistributionPackageImporter} importing
 * {@link DistributionPackage} stream + type into an underlying JCR repository.
 */
public class RepositoryDistributionPackageImporter implements DistributionPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SlingRepository repository;
    private final String serviceName;
    private final String path;
    private final String privilegeName;

    public RepositoryDistributionPackageImporter(SlingRepository repository,
                                                 String serviceName, String path,
                                                 String privilegeName) {
        this.repository = repository;
        this.serviceName = serviceName;
        this.path = path;
        this.privilegeName = privilegeName;
    }

    public void importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException {

        Session session = null;
        try {
            session = authenticate();
            int lastSlash = distributionPackage.getId().lastIndexOf('/');
            String nodeName = Text.escape(lastSlash < 0 ? distributionPackage.getId() : distributionPackage.getId().substring(lastSlash + 1));
            log.debug("importing package {} in {}", distributionPackage.getId(), nodeName);

            if (session != null) {
                Node addedNode = session.getNode(path).addNode(nodeName,
                        NodeType.NT_FILE);
                Node contentNode = addedNode.addNode(JcrConstants.JCR_CONTENT, NodeType.NT_RESOURCE);
                if (contentNode != null) {
                    InputStream inputStream = null;
                    try {
                        inputStream = distributionPackage.createInputStream();
                        contentNode.setProperty(JcrConstants.JCR_DATA, session.getValueFactory().createBinary(inputStream));
                        contentNode.setProperty("package.type", distributionPackage.getType());
                        session.save();
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                    }
                }
                log.debug("package {} imported into the repository as node {} ",
                        distributionPackage.getId(), addedNode.getPath());

            } else {
                throw new Exception("could not get a Session to deliver package to the repository");
            }
        } catch (Exception e) {
            throw new DistributionException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Nonnull
    public DistributionPackageInfo importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException {
        throw new DistributionException("not supported");
    }

    private Session authenticate() throws Exception {
        Session session = repository.loginService(serviceName, null);

        if (session != null) {
            AccessControlManager accessControlManager = session.getAccessControlManager();
            Privilege privilege = accessControlManager.privilegeFromName(privilegeName);

            if (!accessControlManager.hasPrivileges(path, new Privilege[]{privilege})) {
                session.logout();
                throw new Exception("failed to access path " + path + " with privilege " + privilege);
            }
        }

        log.debug("authenticated path {} with privilege {}", path, privilegeName);
        return session;
    }
}
