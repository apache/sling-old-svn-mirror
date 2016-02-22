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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractContentLoaderBinaryTest {

    private static final int SAMPLE_IMAGE_FILESIZE = 62;
    
    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    protected abstract ResourceResolverType getResourceResolverType();
    
    private String path;
    
    @Before
    public void setUp() {
        path = context.uniqueRoot().content();
    }

    @After
    public final void tearDown() throws Exception {
        // make sure all changes from ContentLoader are committed
        assertFalse(context.resourceResolver().hasChanges());
    }
    
    @Test
    public void testBinaryFile() throws IOException {
        context.load().binaryFile("/sample-image.gif", path + "/sample-image.gif");

        Resource fileResource = context.resourceResolver().getResource(path + "/sample-image.gif");
        assertSampleImageFileSize(fileResource);
        assertMimeType(fileResource.getChild(JcrConstants.JCR_CONTENT), "image/gif");
    }

    @Test
    public void testBinaryFileWithMimeType() throws IOException {
        context.load().binaryFile("/sample-image.gif", path + "/sample-image.gif", "mime/test");

        Resource fileResource = context.resourceResolver().getResource(path + "/sample-image.gif");
        assertSampleImageFileSize(fileResource);
        assertMimeType(fileResource.getChild(JcrConstants.JCR_CONTENT), "mime/test");
    }

    @Test
    public void testBinaryResource() throws IOException {
        context.load().binaryResource("/sample-image.gif", path + "/sample-image.gif");

        Resource fileResource = context.resourceResolver().getResource(path + "/sample-image.gif");
        assertSampleImageFileSize(fileResource);
        assertMimeType(fileResource, "image/gif");
    }

    @Test
    public void testBinaryResourceWithMimeType() throws IOException {
        context.load().binaryResource("/sample-image.gif", path + "/sample-image.gif", "mime/test");

        Resource fileResource = context.resourceResolver().getResource(path + "/sample-image.gif");
        assertSampleImageFileSize(fileResource);
        assertMimeType(fileResource, "mime/test");
    }

    private void assertSampleImageFileSize(Resource resource) throws IOException {
        InputStream is = resource.adaptTo(InputStream.class);
        assertNotNull("InputSteam is null for " + resource.getPath(), is);
        byte[] binaryData = IOUtils.toByteArray(is);
        assertEquals(SAMPLE_IMAGE_FILESIZE, binaryData.length);
    }

    private void assertMimeType(Resource resource, String mimeType) {
        assertNotNull(resource);
        assertEquals(mimeType, ResourceUtil.getValueMap(resource).get(JcrConstants.JCR_MIMETYPE, String.class));
    }

}
