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

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.impl.PackagingImpl;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.packaging.impl.FileDistributionPackageBuilder;
import org.apache.sling.distribution.serialization.impl.vlt.FileVaultContentSerializer;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link LocalDistributionPackageImporter}
 */
public class LocalDistributionPackageImporterTest {

    @Rule
    public SlingContext slingContext = new SlingContext(ResourceResolverType.JCR_OAK);

    @Test
    public void testDummyImport() throws Exception {
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        LocalDistributionPackageImporter localdistributionPackageImporter =
                new LocalDistributionPackageImporter("mockImporter", distributionEventFactory, packageBuilder);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        localdistributionPackageImporter.importPackage(resourceResolver, distributionPackage);
    }

    @Test
    public void importPackageWithLargeHeader() throws Exception {
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        LocalDistributionPackageImporter localdistributionPackageImporter =
                new LocalDistributionPackageImporter("mockImporter", distributionEventFactory, packageBuilder);

        FileVaultContentSerializer vaultSerializer = new FileVaultContentSerializer(
                "importPackageWithLargeHeader",
                new PackagingImpl(),
                ImportMode.UPDATE,
                AccessControlHandling.IGNORE,
                new String[0],
                new String[0],
                new String[0],
                false,
                -1,
                null
        );

        DistributionPackageBuilder builder =
                new FileDistributionPackageBuilder(DistributionRequestType.ADD.name(), vaultSerializer, null, null, null, null);

        ResourceResolver resourceResolver = slingContext.resourceResolver();

        String[] paths = createPaths(resourceResolver, 1000, "/content/company/de/press-releases/2016/11/04/message");


        DistributionPackage pkg = builder.createPackage(
                resourceResolver,
                new SimpleDistributionRequest(DistributionRequestType.ADD, paths)
        );

        InputStream streamWithHeader = DistributionPackageUtils.createStreamWithHeader(pkg);
        localdistributionPackageImporter.importStream(resourceResolver, streamWithHeader);
    }

    private String[] createPaths(final ResourceResolver resourceResolver, final int numberOfPaths, final String prefix)
            throws PersistenceException {
        Map<String, Object> primaryType = Collections.<String, Object>singletonMap("jcr:primaryType", "sling:Folder");
        String ancestors = ResourceUtil.getParent(prefix);
        Resource parent = resourceResolver.getResource("/");
        for (final String name : StringUtils.split(ancestors, '/')) {
            parent = resourceResolver.create(parent, name, primaryType);
        }

        final String[] paths = new String[numberOfPaths];
        for (int i = 0; i < numberOfPaths; i++) {
            paths[i] = prefix + "_" + i;
            String name = ResourceUtil.getName(paths[i]);
            resourceResolver.create(parent, name, primaryType);
        }
        resourceResolver.commit();
        return paths;
    }
}
