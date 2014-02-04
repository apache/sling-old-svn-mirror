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
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTransportHandler extends AbstractTransportHandler
        implements TransportHandler {

    private static final String REPO_SCHEME = "repo";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SlingRepository repository;
    private final ReplicationEventFactory replicationEventFactory;
    private final TransportAuthenticationProvider<SlingRepository, Session> transportAuthenticationProvider;


    public RepositoryTransportHandler(SlingRepository repository, ReplicationEventFactory replicationEventFactory,
                                      TransportAuthenticationProvider<SlingRepository, Session> transportAuthenticationProvider,
                                      ReplicationEndpoint[] replicationEndpoints){
        super(replicationEndpoints, TransportEndpointStrategyType.All);


        this.repository = repository;
        this.replicationEventFactory = replicationEventFactory;
        this.transportAuthenticationProvider = transportAuthenticationProvider;
    }

    @Override
    public void deliverPackageToEndpoint(ReplicationPackage replicationPackage, ReplicationEndpoint replicationEndpoint)
            throws Exception {

        Session session = null;
        try {
            TransportAuthenticationContext transportAuthenticationContext = new TransportAuthenticationContext();
            String path = replicationEndpoint.getUri().toString().replace("repo:/", "");
            transportAuthenticationContext.addAttribute("path", path);
            session =  transportAuthenticationProvider.authenticate(repository, transportAuthenticationContext);
            int lastSlash = replicationPackage.getId().lastIndexOf('/');
            String nodeName = Text.escape(lastSlash < 0 ? replicationPackage.getId() : replicationPackage.getId().substring(lastSlash + 1));
            log.info("creating node {} in {}", replicationPackage.getId(), nodeName);

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
                log.info("package {} delivered to the repository as node {} ",
                        replicationPackage.getId(), addedNode.getPath());

                Dictionary<Object, Object> props = new Properties();
                props.put("path", replicationPackage.getPaths());
                replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_REPLICATED, props);

            } else {
                throw new Exception("could not get a Session to deliver package to the repository");
            }
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    protected boolean validateEndpoint(ReplicationEndpoint replicationEndpoint) {
        URI uri = replicationEndpoint.getUri();
        return REPO_SCHEME.equals(uri.getScheme()) && uri.getHost() != null;
    }
}
