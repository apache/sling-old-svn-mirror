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
package org.apache.sling.distribution.serialization.impl.kryo;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.InputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.impl.FileDistributionPackage;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link KryoResourceDistributionPackageBuilder}
 */
public class KryoResourceDistributionPackageBuilderTest {

    private MockHelper helper;
    private ResourceResolver resourceResolver;

    @Before
    public void setUp() throws Exception {
        resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);
        helper = MockHelper.create(resourceResolver).resource("/libs").p("prop", "value")
                .resource("sub").p("sub", "hello")
                .resource(".sameLevel")
                .resource("/apps").p("foo", "baa");
        helper.commit();
    }

    @Test
    public void testPackageLifecycle() throws Exception {
        KryoResourceDistributionPackageBuilder kryoResourceDistributionPackageBuilder = new KryoResourceDistributionPackageBuilder();
        DistributionPackage kryoPackage = kryoResourceDistributionPackageBuilder.createPackage(resourceResolver, new DistributionRequest() {
            @Nonnull
            public DistributionRequestType getRequestType() {
                return DistributionRequestType.ADD;
            }

            public String[] getPaths() {
                return new String[]{"/libs"};
            }

            public boolean isDeep(String path) {
                return true;
            }

            @Nonnull
            @Override
            public String[] getFilters(String path) {
                return new String[0];
            }
        });
        assertNotNull(kryoPackage);
        DistributionPackage readPackage = kryoResourceDistributionPackageBuilder.readPackage(resourceResolver, kryoPackage.createInputStream());
        assertTrue(kryoResourceDistributionPackageBuilder.installPackage(resourceResolver, readPackage));
    }

    @Test
    public void testCreatePackage() throws Exception {
        KryoResourceDistributionPackageBuilder kryoResourceDistributionPackageBuilder = new KryoResourceDistributionPackageBuilder();
        DistributionPackage kryoPackage = kryoResourceDistributionPackageBuilder.createPackage(resourceResolver, new DistributionRequest() {
            @Nonnull
            public DistributionRequestType getRequestType() {
                return DistributionRequestType.ADD;
            }

            public String[] getPaths() {
                return new String[]{"/libs"};
            }

            public boolean isDeep(String path) {
                return true;
            }

            @Nonnull
            @Override
            public String[] getFilters(String path) {
                return new String[0];
            }
        });
        assertNotNull(kryoPackage);
        assertNotNull(kryoPackage.getInfo());
    }

    @Test
    public void testReadPackage() throws Exception {
        KryoResourceDistributionPackageBuilder kryoResourceDistributionPackageBuilder = new KryoResourceDistributionPackageBuilder();
        InputStream stream = getClass().getResourceAsStream("/kryo/dp.kryo");
        DistributionPackage distributionPackage = kryoResourceDistributionPackageBuilder.readPackage(resourceResolver, stream);
        assertNotNull(distributionPackage);
    }

    @Test
    public void testGetPackage() throws Exception {
        KryoResourceDistributionPackageBuilder kryoResourceDistributionPackageBuilder = new KryoResourceDistributionPackageBuilder();
        InputStream stream = getClass().getResourceAsStream("/kryo/dp.kryo");
        DistributionPackage distributionPackage = kryoResourceDistributionPackageBuilder.getPackage(resourceResolver, getClass().getResource("/kryo/dp.kryo").getFile());
        assertNotNull(distributionPackage);
    }

    @Test
    public void testInstallPackage() throws Exception {
        KryoResourceDistributionPackageBuilder kryoResourceDistributionPackageBuilder = new KryoResourceDistributionPackageBuilder();
        File file = new File(getClass().getResource("/kryo/dp.kryo").getFile());
        DistributionPackage distributionPackage = new FileDistributionPackage(file, "kryo");
        boolean succes = kryoResourceDistributionPackageBuilder.installPackage(resourceResolver, distributionPackage);
        assertTrue(succes);
    }
}