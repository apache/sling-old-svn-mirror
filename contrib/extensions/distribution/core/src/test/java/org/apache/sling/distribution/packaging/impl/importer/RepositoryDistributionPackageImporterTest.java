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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;

/**
 * Testcase for {@link org.apache.sling.distribution.packaging.impl.importer.RepositoryDistributionPackageImporter}
 */
public class RepositoryDistributionPackageImporterTest {

    @Test(expected = DistributionException.class)
    public void testImportPackageWithUnauthorizedServiceUser() throws Exception {
        SlingRepository repository = mock(SlingRepository.class);
        String serviceName = "admin";
        String path = "/var/something";
        String privilegeName = "jcr:read";
        RepositoryDistributionPackageImporter repositoryDistributionPackageImporter =
                new RepositoryDistributionPackageImporter(repository, serviceName, path, privilegeName);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        when(distributionPackage.getId()).thenReturn("someid");
        repositoryDistributionPackageImporter.importPackage(resourceResolver, distributionPackage);
    }

    @Test(expected = DistributionException.class)
    public void testImportPackageWithoutRequiredPrivileges() throws Exception {
        SlingRepository repository = mock(SlingRepository.class);
        String serviceName = "admin";
        Session session = mock(Session.class);
        AccessControlManager acm = mock(AccessControlManager.class);
        String privilegeName = "jcr:read";
        Privilege privilege = mock(Privilege.class);
        when(acm.privilegeFromName(privilegeName)).thenReturn(privilege);
        when(session.getAccessControlManager()).thenReturn(acm);
        when(repository.loginService(serviceName, null)).thenReturn(session);
        String path = "/var/something";
        RepositoryDistributionPackageImporter repositoryDistributionPackageImporter =
                new RepositoryDistributionPackageImporter(repository, serviceName, path, privilegeName);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        when(distributionPackage.getId()).thenReturn("someid");
        repositoryDistributionPackageImporter.importPackage(resourceResolver, distributionPackage);
    }

    @Test
    public void testImportPackageWithRequiredPrivileges() throws Exception {
        SlingRepository repository = mock(SlingRepository.class);
        String serviceName = "admin";
        Session session = mock(Session.class);
        ValueFactory vf = mock(ValueFactory.class);
        when(session.getValueFactory()).thenReturn(vf);
        AccessControlManager acm = mock(AccessControlManager.class);
        String privilegeName = "jcr:read";
        Privilege privilege = mock(Privilege.class);
        String path = "/var/something";
        Node rootNode = mock(Node.class);
        Node createdNode = mock(Node.class);
        Node jcrContentNode = mock(Node.class);
        when(createdNode.addNode(JcrConstants.JCR_CONTENT, NodeType.NT_RESOURCE)).thenReturn(jcrContentNode);
        when(rootNode.addNode(any(String.class), any(String.class))).thenReturn(createdNode);
        when(session.getNode(path)).thenReturn(rootNode);
        when(acm.hasPrivileges(path, new Privilege[]{privilege})).thenReturn(true);
        when(acm.privilegeFromName(privilegeName)).thenReturn(privilege);
        when(session.getAccessControlManager()).thenReturn(acm);
        when(repository.loginService(serviceName, null)).thenReturn(session);
        RepositoryDistributionPackageImporter repositoryDistributionPackageImporter =
                new RepositoryDistributionPackageImporter(repository, serviceName, path, privilegeName);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        when(distributionPackage.getId()).thenReturn("someid");

        InputStream stream = mock(InputStream.class);
        when(distributionPackage.createInputStream()).thenReturn(stream);
        repositoryDistributionPackageImporter.importPackage(resourceResolver, distributionPackage);
    }
}
