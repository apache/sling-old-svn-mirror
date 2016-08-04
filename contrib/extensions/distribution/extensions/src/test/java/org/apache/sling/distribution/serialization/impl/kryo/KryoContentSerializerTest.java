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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.serialization.impl.avro.AvroContentSerializer;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link KryoContentSerializer}
 */
public class KryoContentSerializerTest {

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
    public void testExtract() throws Exception {
        KryoContentSerializer kryoContentSerializer = new KryoContentSerializer("kryo");
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/libs");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        kryoContentSerializer.exportToStream(resourceResolver, request, outputStream);
        byte[] bytes = outputStream.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    public void testImport() throws Exception {
        KryoContentSerializer kryoContentSerializer = new KryoContentSerializer("avro");
        InputStream inputStream = getClass().getResourceAsStream("/kryo/dp.kryo");
        kryoContentSerializer.importFromStream(resourceResolver, inputStream);
    }
}
