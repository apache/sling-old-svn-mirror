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

import java.net.URI;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.AuthenticationContext;
import org.apache.sling.replication.transport.authentication.AuthenticationHandler;
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

    public void transport(ReplicationPackage replicationPackage,
                    ReplicationEndpoint replicationEndpoint,
                    AuthenticationHandler<?, ?> authenticationHandler)
                    throws ReplicationTransportException {
        if (validateEndpoint(replicationEndpoint)) {
            try {
                AuthenticationContext authenticationContext = new AuthenticationContext();
                String path = new StringBuilder(replicationEndpoint.getUri().getHost()).append(
                                replicationEndpoint.getUri().getPath()).toString();
                authenticationContext.addAttribute("path", path);
                @SuppressWarnings("unchecked")
                Session session = ((AuthenticationHandler<SlingRepository, Session>) authenticationHandler)
                                .authenticate(repository, authenticationContext);
                if (session != null) {
                    Node addedNode = session.getNode(path).addNode(replicationPackage.getId(),
                                    NodeType.NT_FILE);
                    if (log.isInfoEnabled()) {
                        log.info("package {} delivered to the repository as node {} ",
                                        replicationPackage.getId(), addedNode.getPath());
                    }
                    // TODO : trigger event, this event can be used to ask an author to get the persisted package
                } else {
                    throw new ReplicationTransportException(
                                    "could not get a Session to deliver package to the repository");
                }
            } catch (Exception e) {
                throw new ReplicationTransportException(e);
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

    public boolean supportsAuthenticationHandler(AuthenticationHandler<?, ?> authenticationHandler) {
        return authenticationHandler.canAuthenticate(SlingRepository.class);
    }
}
