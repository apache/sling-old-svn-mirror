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
package org.apache.sling.testing.mock.sling.loader;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.NodeTypeDefinitionScanner;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public abstract class AbstractContentLoaderJsonDamTest {

    private ResourceResolver resourceResolver;

    protected abstract ResourceResolverType getResourceResolverType();

    protected ResourceResolver newResourceResolver() {
        ResourceResolver resolver = MockSling.newResourceResolver(getResourceResolverType());

        try {
            NodeTypeDefinitionScanner.get().register(resolver.adaptTo(Session.class), 
                    ImmutableList.of("SLING-INF/nodetypes/app.cnd"),
                    getResourceResolverType().getNodeTypeMode());
        }
        catch (RepositoryException ex) {
            throw new RuntimeException("Unable to register namespaces.", ex);
        }

        return resolver;
    }

    @Before
    public final void setUp() {
        this.resourceResolver = newResourceResolver();
        ContentLoader contentLoader = new ContentLoader(this.resourceResolver);
        contentLoader.json("/json-import-samples/dam.json", "/content/dam/sample");
    }

    @After
    public final void tearDown() throws Exception {
        // make sure all changes from ContentLoader are committed
        assertFalse(resourceResolver.hasChanges());
        // remove everything below /content
        Resource content = resourceResolver.getResource("/content");
        if (content != null) {
            resourceResolver.delete(content);
            resourceResolver.commit();
        }
        this.resourceResolver.close();
    }
            
    @Test
    public void testDamAssetMetadata() throws IOException {
        Resource assetMetadata = this.resourceResolver
                .getResource("/content/dam/sample/portraits/scott_reynolds.jpg/jcr:content/metadata");
        ValueMap props = ResourceUtil.getValueMap(assetMetadata);

        assertEquals("Canon\u0000", props.get("tiff:Make", String.class));
        assertEquals((Long) 807L, props.get("tiff:ImageWidth", Long.class));
        assertEquals((Integer) 595, props.get("tiff:ImageLength", Integer.class));
        assertEquals(4.64385986328125d, props.get("dam:ApertureValue", Double.class), 0.00000000001d);

        assertArrayEquals(new String[] { "stockphotography:business/business_people", "properties:style/color",
                "properties:orientation/landscape" }, props.get("app:tags", String[].class));

        // validate that a binary data node is present, but empty
        Resource binaryMetadata = this.resourceResolver
                .getResource("/content/dam/sample/portraits/scott_reynolds.jpg/jcr:content/renditions/original/jcr:content");
        ValueMap binaryProps = ResourceUtil.getValueMap(binaryMetadata);
        InputStream is = binaryProps.get(JcrConstants.JCR_DATA, InputStream.class);
        assertNotNull(is);
        byte[] binaryData = IOUtils.toByteArray(is);
        assertEquals(0, binaryData.length);
    }

}
