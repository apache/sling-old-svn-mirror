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

import java.lang.reflect.Field;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Session;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.transport.authentication.impl.RepositoryTransportAuthenticationProvider;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link RepositoryTransportHandler}
 */
public class RepositoryTransportHandlerTest {

    @Test
    public void testDeliveryWithoutAuthenticatedSession() throws Exception {
        RepositoryTransportHandler handler = new RepositoryTransportHandler();
        @SuppressWarnings("unchecked")
        TransportAuthenticationProvider<SlingRepository, Session> transportAuthenticationProvider = mock(TransportAuthenticationProvider.class);
        try {
            handler.transport(null, new ReplicationEndpoint("repo://var/outbox/replication/rev1"),
                    transportAuthenticationProvider);
            fail("cannot deliver without a proper session");
        } catch (ReplicationTransportException re) {
            // failure expected
        }
    }

    @Test
    public void testDeliveryWithAuthenticatedSession() throws Exception {
        String repoPath = "/var/outbox/replication/rev1";

        Node addedNode = mock(Node.class);
        when(addedNode.getPath()).thenReturn(repoPath + "/some-id");

        Node node = mock(Node.class);
        when(node.addNode(any(String.class), any(String.class))).thenReturn(addedNode);

        Session session = mock(Session.class);
        when(session.getNode(repoPath)).thenReturn(node);
        when(session.nodeExists(repoPath)).thenReturn(true);

        SlingRepository repo = mock(SlingRepository.class);
        when(repo.login(any(Credentials.class))).thenReturn(session);

        RepositoryTransportHandler handler = new RepositoryTransportHandler();
        Field repositoryField = handler.getClass().getDeclaredField("repository");
        repositoryField.setAccessible(true);
        repositoryField.set(handler, repo);

        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        Field replicationEventFactoryField = handler.getClass().getDeclaredField("replicationEventFactory");
        replicationEventFactoryField.setAccessible(true);
        replicationEventFactoryField.set(handler, replicationEventFactory);

        TransportAuthenticationProvider<SlingRepository, Session> transportAuthenticationProvider = new RepositoryTransportAuthenticationProvider("user-123", "p455w0rd");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(replicationPackage.getId()).thenReturn("some-id");
        when(replicationPackage.getPaths()).thenReturn(new String[]{"/apps", "/libs"});
        handler.transport(replicationPackage, new ReplicationEndpoint("repo:/" + repoPath),
                transportAuthenticationProvider);
    }
}
