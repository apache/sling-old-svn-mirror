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
package org.apache.sling.distribution.transport.authentication.impl;

import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.sling.distribution.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationException;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.distribution.transport.authentication.impl.RepositoryTransportAuthenticationProvider}
 */
public class RepositoryTransportAuthenticationProviderTest {

    @Test
    public void testCanAuthenticateSlingRepo() throws Exception {
        String serviceName = "service";
        RepositoryTransportAuthenticationProvider repositoryTransportAuthenticationProvider = new RepositoryTransportAuthenticationProvider(serviceName);
        assertTrue(repositoryTransportAuthenticationProvider.canAuthenticate(SlingRepository.class));
    }

    @Test
    public void testAuthenticateWithoutPath() throws Exception {
        String serviceName = "service";
        RepositoryTransportAuthenticationProvider repositoryTransportAuthenticationProvider = new RepositoryTransportAuthenticationProvider(serviceName);

        SlingRepository repo = mock(SlingRepository.class);
        TransportAuthenticationContext context = mock(TransportAuthenticationContext.class);
        try {
            repositoryTransportAuthenticationProvider.authenticate(repo, context);
            fail("cannot authenticate a null path");
        } catch (TransportAuthenticationException e) {
            // expected to fail
        }
    }

    @Test
    public void testAuthenticateWithPathAndNoPrivilege() throws Exception {
        String serviceName = "service";
        RepositoryTransportAuthenticationProvider repositoryTransportAuthenticationProvider = new RepositoryTransportAuthenticationProvider(serviceName);

        SlingRepository repo = mock(SlingRepository.class);
        TransportAuthenticationContext context = new TransportAuthenticationContext();
        context.addAttribute("path", "/foo/bar");
        try {
            repositoryTransportAuthenticationProvider.authenticate(repo, context);
            fail("cannot authenticate a without privileges");
        } catch (TransportAuthenticationException e) {
            // expected to fail
        }
    }

    @Test
    public void testAuthenticateWithPathAndPrivilege() throws Exception {
        String serviceName = "service";

        TransportAuthenticationContext context = new TransportAuthenticationContext();
        String path = "/foo/bar";
        context.addAttribute("path", path);
        String privilege = Privilege.JCR_WRITE;
        context.addAttribute("privilege", privilege);

        SlingRepository repo = mock(SlingRepository.class);
        Session authenticatedSession = mock(Session.class);
        when(authenticatedSession.hasPermission(path, privilege)).thenReturn(true);
        when(repo.loginService(serviceName, null)).thenReturn(authenticatedSession);

        RepositoryTransportAuthenticationProvider repositoryTransportAuthenticationProvider = new RepositoryTransportAuthenticationProvider(serviceName);
        Session session = repositoryTransportAuthenticationProvider.authenticate(repo, context);
        assertNotNull(session);
    }
}