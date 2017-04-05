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
package org.apache.sling.distribution.serialization.impl.avro;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.FileDistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportFilter;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link AvroContentSerializer}
 */
public class AvroContentSerializerTest {

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
    public void testExtractDeep() throws Exception {
        AvroContentSerializer avroContentSerializer = new AvroContentSerializer("avro");
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, true, "/libs");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        NavigableMap<String, List<String>> nodeFilters = new TreeMap<String, List<String>>();
        NavigableMap<String, List<String>> propertyFilters = new TreeMap<String, List<String>>();
        try {
            DistributionExportFilter filter = DistributionExportFilter.createFilter(request, nodeFilters, propertyFilters);
            avroContentSerializer.exportToStream(resourceResolver, new DistributionExportOptions(request, filter), outputStream);
            byte[] bytes = outputStream.toByteArray();
            assertNotNull(bytes);
            assertTrue(bytes.length > 0);
        } finally {
            outputStream.close();
        }
    }

    @Test
    public void testExtractShallow() throws Exception {
        AvroContentSerializer avroContentSerializer = new AvroContentSerializer("avro");
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/libs");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        NavigableMap<String, List<String>> nodeFilters = new TreeMap<String, List<String>>();
        NavigableMap<String, List<String>> propertyFilters = new TreeMap<String, List<String>>();
        try {
            DistributionExportFilter filter = DistributionExportFilter.createFilter(request, nodeFilters, propertyFilters);
            avroContentSerializer.exportToStream(resourceResolver, new DistributionExportOptions(request, filter), outputStream);
            byte[] bytes = outputStream.toByteArray();
            assertNotNull(bytes);
            assertTrue(bytes.length > 0);
        } finally {
            outputStream.close();
        }
    }

    @Test
    public void testImport() throws Exception {
        AvroContentSerializer avroContentSerializer = new AvroContentSerializer("avro");
        InputStream inputStream = getClass().getResourceAsStream("/avro/dp.avro");
        avroContentSerializer.importFromStream(resourceResolver, inputStream);
    }

    @Test
    public void testBuildAndInstallOnSingleDeepPath() throws Exception {
        String type = "avro";
        DistributionContentSerializer contentSerializer = new AvroContentSerializer(type);
        String tempFilesFolder = "target";
        String[] nodeFilters = new String[0];
        String[] propertyFilters = new String[0];
        DistributionPackageBuilder packageBuilder = new FileDistributionPackageBuilder(type, contentSerializer,
                tempFilesFolder, null, nodeFilters, propertyFilters);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, true, "/libs");
        DistributionPackage distributionPackage = packageBuilder.createPackage(resourceResolver, request);

        Resource resource = resourceResolver.getResource("/libs/sub");
        resourceResolver.delete(resource);
        resourceResolver.commit();

        assertTrue(packageBuilder.installPackage(resourceResolver, distributionPackage));

        assertNotNull(resourceResolver.getResource("/libs"));
        assertNotNull(resourceResolver.getResource("/libs/sub"));
        assertNotNull(resourceResolver.getResource("/libs/sameLevel"));
    }

    @Test
    public void testBuildAndInstallOnSingleShallowPath() throws Exception {
        String type = "avro";
        DistributionContentSerializer contentSerializer = new AvroContentSerializer(type);
        String tempFilesFolder = "target";
        String[] nodeFilters = new String[0];
        String[] propertyFilters = new String[0];
        DistributionPackageBuilder packageBuilder = new FileDistributionPackageBuilder(type, contentSerializer,
                tempFilesFolder, null, nodeFilters, propertyFilters);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/libs/sub");
        DistributionPackage distributionPackage = packageBuilder.createPackage(resourceResolver, request);

        Resource resource = resourceResolver.getResource("/libs/sub");
        resourceResolver.delete(resource);
        resourceResolver.commit();

        assertTrue(packageBuilder.installPackage(resourceResolver, distributionPackage));

        assertNotNull(resourceResolver.getResource("/libs"));
        assertNotNull(resourceResolver.getResource("/libs/sub"));
        assertNotNull(resourceResolver.getResource("/libs/sameLevel"));
    }

    @Test
    public void testBuildAndInstallOnMultipleShallowPaths() throws Exception {
        String type = "avro";
        DistributionContentSerializer contentSerializer = new AvroContentSerializer(type);
        String tempFilesFolder = "target";
        String[] nodeFilters = new String[0];
        String[] propertyFilters = new String[0];
        DistributionPackageBuilder packageBuilder = new FileDistributionPackageBuilder(type, contentSerializer,
                tempFilesFolder, null, nodeFilters, propertyFilters);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/libs/sub", "/libs/sameLevel");
        DistributionPackage distributionPackage = packageBuilder.createPackage(resourceResolver, request);

        Resource resource = resourceResolver.getResource("/libs/sub");
        resourceResolver.delete(resource);
        resource = resourceResolver.getResource("/libs/sameLevel");
        resourceResolver.delete(resource);
        resourceResolver.commit();

        assertTrue(packageBuilder.installPackage(resourceResolver, distributionPackage));

        assertNotNull(resourceResolver.getResource("/libs"));
        assertNotNull(resourceResolver.getResource("/libs/sub"));
        assertNotNull(resourceResolver.getResource("/libs/sameLevel"));
    }
}
