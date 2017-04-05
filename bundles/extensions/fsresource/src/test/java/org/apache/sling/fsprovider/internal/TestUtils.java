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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.context.AbstractContextPlugin;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;

class TestUtils {

    public static class RegisterFsResourcePlugin extends AbstractContextPlugin<SlingContextImpl> {
        private final Map<String,Object> props;
        public RegisterFsResourcePlugin(Object... props) {
            this.props = MapUtil.toMap(props); 
        }
        @Override
        public void beforeSetUp(SlingContextImpl context) throws Exception {
            Map<String,Object> config = new HashMap<>();
            config.put("provider.file", "src/test/resources/fs-test");
            config.put("provider.root", "/fs-test");
            config.put("provider.checkinterval", 0);
            config.put("provider.fs.mode", FsMode.FILES_FOLDERS.name());
            config.putAll(props);
            context.registerInjectActivateService(new FsResourceProvider(), config);
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
        
        assertNull(file.getResourceSuperType());
        assertEquals(file.getName(), file.adaptTo(File.class).getName());
        assertTrue(StringUtils.contains(file.adaptTo(URL.class).toString(), file.getName()));
        
        if (content != null) {
            try {
                try (InputStream is = file.adaptTo(InputStream.class)) {
                    String data = IOUtils.toString(is, CharEncoding.UTF_8);
                    assertEquals(content, data);
                }
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }    

    public static void assertChange(List<ResourceChange> changes, String path, ChangeType changeType) {
        boolean found = false;
        for (ResourceChange change : changes) {
            if (StringUtils.equals(change.getPath(), path) && change.getType() == changeType) {
                found = true;
                break;
            }
        }
        assertTrue("Change with path=" + path + ", changeType=" + changeType + " expected", found);
    }
    
    public static class ResourceListener implements ResourceChangeListener {
        private final List<ResourceChange> allChanges = new ArrayList<>();
        @Override
        public void onChange(List<ResourceChange> changes) {
            allChanges.addAll(changes);
        }
        public List<ResourceChange> getChanges() {
            return allChanges;
        }
    }

}
