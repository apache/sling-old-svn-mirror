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
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.event.DistributionEventType;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImportException;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.DistributionEndpoint;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.packaging.DistributionPackageImporter} importing
 * {@link org.apache.sling.distribution.packaging.DistributionPackage} stream + type into an underlying JCR repository.
 */
public class RepositoryDistributionPackageImporter implements DistributionPackageImporter {

    static final String NAME = "repository";

    private static final String REPO_SCHEME = "repo";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private SlingRepository repository;

    private DistributionEventFactory distributionEventFactory;

    private DistributionTransportSecretProvider distributionTransportSecretProvider;

    private String serviceName;
    private String path;
    private String privilege;

    public void deliverPackageToEndpoint(DistributionPackage distributionPackage, DistributionEndpoint distributionEndpoint)
            throws Exception {

        Session session = null;
        try {
            String path = distributionEndpoint.getUri().toString().replace("repo:/", "");
            session = authenticate();
            int lastSlash = distributionPackage.getId().lastIndexOf('/');
            String nodeName = Text.escape(lastSlash < 0 ? distributionPackage.getId() : distributionPackage.getId().substring(lastSlash + 1));
            log.info("creating node {} in {}", distributionPackage.getId(), nodeName);

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
                log.info("package {} imported into the repository as node {} ",
                        distributionPackage.getId(), addedNode.getPath());

            } else {
                throw new Exception("could not get a Session to deliver package to the repository");
            }
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    public void importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageImportException {
        // do nothing
    }

    public DistributionPackage importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageImportException {
        throw new DistributionPackageImportException("not supported");
    }


    private Session authenticate() throws Exception {
        Session session = repository.loginService(serviceName, null);

        if (!session.hasPermission(path, privilege)) {
            session.logout();
            throw new Exception("failed to access path " + path + " with privilege " + privilege);
        }

        log.info("authenticated path {} with privilege {}", path, privilege);
        return session;
    }
}
