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
package org.apache.sling.fsprovider.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.osgi.context.AbstractContextPlugin;
import org.apache.sling.testing.mock.osgi.context.ContextPlugin;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;

class TestUtils {

    public static ContextPlugin<SlingContextImpl> REGISTER_FSRESOURCE_PLUGIN = new AbstractContextPlugin<SlingContextImpl>() {
        @Override
        public void beforeSetUp(SlingContextImpl context) throws Exception {
            context.registerInjectActivateService(new FsResourceProvider(),
                    "provider.file", "src/test/resources/fs-test",
                    "provider.root", "/fs-test",
                    "provider.checkinterval", 0);
        }
    };

    public static void assertFolder(Resource resource, String path) {
        Resource folder = resource.getChild(path);
        assertNotNull(path, folder);
        
        assertThat(folder, ResourceMatchers.props("jcr:primaryType", "nt:folder"));
        assertEquals("nt:folder", folder.getResourceType());
        
        assertNull(folder.getResourceSuperType());
        assertEquals(folder.getName(), folder.adaptTo(File.class).getName());
        assertTrue(StringUtils.contains(folder.adaptTo(URL.class).toString(), folder.getName()));
    }

    public static void assertFile(Resource resource, String path, String content) {
        Resource file = resource.getChild(path);
        assertNotNull(path, file);
        
        assertThat(file, ResourceMatchers.props("jcr:primaryType", "nt:file"));
        assertEquals("nt:file", file.getResourceType());
        
        try {
            try (InputStream is = file.adaptTo(InputStream.class)) {
                String data = IOUtils.toString(is, CharEncoding.UTF_8);
                assertEquals(content, data);
            }
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        assertNull(file.getResourceSuperType());
        assertEquals(file.getName(), file.adaptTo(File.class).getName());
        assertTrue(StringUtils.contains(file.adaptTo(URL.class).toString(), file.getName()));
    }    

}
