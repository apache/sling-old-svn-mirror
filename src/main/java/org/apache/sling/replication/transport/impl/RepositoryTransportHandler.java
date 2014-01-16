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
package org.apache.sling.replication.transport.impl;

import java.io.InputStream;
import java.net.URI;
import java.util.Dictionary;
import java.util.Properties;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = false)
@Service(value = TransportHandler.class)
@Property(name = "name", value = RepositoryTransportHandler.NAME)
public class RepositoryTransportHandler implements TransportHandler {

    public static final String NAME = "repository";

    private static final String REPO_SCHEME = "repo";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private SlingRepository repository;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    public void transport(ReplicationPackage replicationPackage,
                          ReplicationEndpoint replicationEndpoint,
                          TransportAuthenticationProvider<?, ?> transportAuthenticationProvider)
            throws ReplicationTransportException {
        if (validateEndpoint(replicationEndpoint)) {
            Session session = null;
            try {
                TransportAuthenticationContext transportAuthenticationContext = new TransportAuthenticationContext();
                String path = replicationEndpoint.getUri().toString().replace("repo:/", "");
                transportAuthenticationContext.addAttribute("path", path);
                session = ((TransportAuthenticationProvider<SlingRepository, Session>) transportAuthenticationProvider)
                        .authenticate(repository, transportAuthenticationContext);
                int lastSlash = replicationPackage.getId().lastIndexOf('/');
                String nodeName = Text.escape(lastSlash < 0 ? replicationPackage.getId() : replicationPackage.getId().substring(lastSlash + 1));
                if (log.isInfoEnabled()) {
                    log.info("creating node {} in {}", replicationPackage.getId(), nodeName);
                }
                if (session != null) {
                    Node addedNode = session.getNode(path).addNode(nodeName,
                            NodeType.NT_FILE);
                    Node contentNode = addedNode.addNode(JcrConstants.JCR_CONTENT, NodeType.NT_RESOURCE);
                    if (contentNode != null) {
                        InputStream inputStream = null;
                        try {
                            inputStream = replicationPackage.createInputStream();
                            contentNode.setProperty(JcrConstants.JCR_DATA, session.getValueFactory().createBinary(inputStream));
                            session.save();
                        }
                        finally {
                            IOUtils.closeQuietly(inputStream);
                        }
                    }
                    if (log.isInfoEnabled()) {
                        log.info("package {} delivered to the repository as node {} ",
                                replicationPackage.getId(), addedNode.getPath());
                    }
                    Dictionary<Object, Object> props = new Properties();
                    props.put("transport", NAME);
                    props.put("path", replicationPackage.getPaths());
                    replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_REPLICATED, props);

                } else {
                    throw new ReplicationTransportException(
                            "could not get a Session to deliver package to the repository");
                }
            } catch (Exception e) {
                throw new ReplicationTransportException(e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        } else {
            throw new ReplicationTransportException("invalid endpoint "
                    + replicationEndpoint.getUri());
        }
    }

    private boolean validateEndpoint(ReplicationEndpoint replicationEndpoint) {
        URI uri = replicationEndpoint.getUri();
        return REPO_SCHEME.equals(uri.getScheme()) && uri.getHost() != null;
    }

    public boolean supportsAuthenticationProvider(TransportAuthenticationProvider<?, ?> transportAuthenticationProvider) {
        return transportAuthenticationProvider.canAuthenticate(SlingRepository.class);
    }
}
