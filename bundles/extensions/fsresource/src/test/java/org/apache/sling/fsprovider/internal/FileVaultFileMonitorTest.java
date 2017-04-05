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

import static org.apache.sling.fsprovider.internal.TestUtils.assertChange;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.fsprovider.internal.TestUtils.ResourceListener;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.apache.sling.testing.mock.sling.junit.SlingContextCallback;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test events when changing file system content (FileVault XML).
 */
public class FileVaultFileMonitorTest {
    
    private static final int CHECK_INTERVAL = 120;
    private static final int WAIT_INTERVAL = 250;

    private final File tempDir;
    private final ResourceListener resourceListener = new ResourceListener();
    
    public FileVaultFileMonitorTest() throws Exception {
        tempDir = Files.createTempDirectory(getClass().getName()).toFile();
    }

    @Rule
    public SlingContext context = new SlingContextBuilder(ResourceResolverType.JCR_MOCK)
        .beforeSetUp(new SlingContextCallback() {
            @Override
            public void execute(SlingContext context) throws Exception {
                // copy test content to temp. directory
                tempDir.mkdirs();
                File sourceDir = new File("src/test/resources/vaultfs-test");
                FileUtils.copyDirectory(sourceDir, tempDir);
                
                // mount temp. directory
                context.registerInjectActivateService(new FsResourceProvider(),
                        "provider.file", tempDir.getPath() + "/jcr_root",
                        "provider.filevault.filterxml.path", tempDir.getPath() + "/META-INF/vault/filter.xml",
                        "provider.root", "/content/dam/talk.png",
                        "provider.checkinterval", CHECK_INTERVAL,
                        "provider.fs.mode", FsMode.FILEVAULT_XML.name());
                context.registerInjectActivateService(new FsResourceProvider(),
                        "provider.file", tempDir.getPath() + "/jcr_root",
                        "provider.filevault.filterxml.path", tempDir.getPath() + "/META-INF/vault/filter.xml",
                        "provider.root", "/content/samples",
                        "provider.checkinterval", CHECK_INTERVAL,
                        "provider.fs.mode", FsMode.FILEVAULT_XML.name());
                
                // register resource change listener
                context.registerService(ResourceChangeListener.class, resourceListener,
                        ResourceChangeListener.PATHS, new String[] { "/content/dam/talk.png", "/content/samples" });
            }
        })
        .afterTearDown(new SlingContextCallback() {
            @Override
            public void execute(SlingContext context) throws Exception {
                // remove temp directory
                tempDir.delete();
            }
        })
        .build();

    @Test
    public void testUpdateFile() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File file = new File(tempDir, "jcr_root/content/dam/talk.png/_jcr_content/renditions/web.1280.1280.png");
        FileUtils.touch(file);
        
        Thread.sleep(WAIT_INTERVAL);

        assertEquals(1, changes.size());
        assertChange(changes, "/content/dam/talk.png/jcr:content/renditions/web.1280.1280.png", ChangeType.CHANGED);
    }
    
    @Test
    public void testAddFile() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File file = new File(tempDir, "jcr_root/content/dam/talk.png/_jcr_content/renditions/text.txt");
        FileUtils.write(file, "newcontent");
        
        Thread.sleep(WAIT_INTERVAL);

        assertEquals(2, changes.size());
        assertChange(changes, "/content/dam/talk.png/jcr:content/renditions", ChangeType.CHANGED);
        assertChange(changes, "/content/dam/talk.png/jcr:content/renditions/text.txt", ChangeType.ADDED);
    }
    
    @Test
    public void testRemoveFile() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File file = new File(tempDir, "jcr_root/content/dam/talk.png/_jcr_content/renditions/web.1280.1280.png");
        file.delete();
        
        Thread.sleep(WAIT_INTERVAL);

        assertEquals(2, changes.size());
        assertChange(changes, "/content/dam/talk.png/jcr:content/renditions", ChangeType.CHANGED);
        assertChange(changes, "/content/dam/talk.png/jcr:content/renditions/web.1280.1280.png", ChangeType.REMOVED);
    }
    
    @Test
    public void testAddFolder() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File folder = new File(tempDir, "jcr_root/content/dam/talk.png/_jcr_content/newfolder");
        folder.mkdir();
        
        Thread.sleep(WAIT_INTERVAL);

        assertEquals(2, changes.size());
        assertChange(changes, "/content/dam/talk.png/jcr:content", ChangeType.CHANGED);
        assertChange(changes, "/content/dam/talk.png/jcr:content/newfolder", ChangeType.ADDED);
    }
    
    @Test
    public void testRemoveFolder() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File folder = new File(tempDir, "jcr_root/content/dam/talk.png/_jcr_content/renditions");
        FileUtils.deleteDirectory(folder);
        
        Thread.sleep(WAIT_INTERVAL);

        assertEquals(2, changes.size());
        assertChange(changes, "/content/dam/talk.png/jcr:content", ChangeType.CHANGED);
        assertChange(changes, "/content/dam/talk.png/jcr:content/renditions", ChangeType.REMOVED);
    }

    @Test
    public void testUpdateContent() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File file = new File(tempDir, "jcr_root/content/samples/en/.content.xml");
        FileUtils.touch(file);
        
        Thread.sleep(WAIT_INTERVAL);

        assertChange(changes, "/content/samples/en", ChangeType.REMOVED);
        assertChange(changes, "/content/samples/en", ChangeType.ADDED);
        assertChange(changes, "/content/samples/en/jcr:content", ChangeType.ADDED);
    }
    
    @Test
    public void testAddContent() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File file = new File(tempDir, "jcr_root/content/samples/fr/.content.xml");
        file.getParentFile().mkdir();
        FileUtils.write(file, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<jcr:root xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:app=\"http://sample.com/jcr/app/1.0\" "
                + "xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" jcr:primaryType=\"app:Page\">\n"
                + "<jcr:content jcr:primaryType=\"app:PageContent\"/>\n"
                + "</jcr:root>");
        
        Thread.sleep(WAIT_INTERVAL);

        assertChange(changes, "/content/samples", ChangeType.CHANGED);
        assertChange(changes, "/content/samples/fr", ChangeType.ADDED);
        assertChange(changes, "/content/samples/fr/jcr:content", ChangeType.ADDED);
    }
    
    @Test
    public void testRemoveContent() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File file = new File(tempDir, "jcr_root/content/samples/en");
        FileUtils.deleteDirectory(file);
        
        Thread.sleep(WAIT_INTERVAL);

        assertEquals(2, changes.size());
        assertChange(changes, "/content/samples", ChangeType.CHANGED);
        assertChange(changes, "/content/samples/en", ChangeType.REMOVED);
    }
    
    @Test
    public void testRemoveContentDotXmlOnly() throws Exception {
        List<ResourceChange> changes = resourceListener.getChanges();
        assertTrue(changes.isEmpty());
        
        File file = new File(tempDir, "jcr_root/content/samples/en/.content.xml");
        file.delete();
        
        Thread.sleep(WAIT_INTERVAL);

        assertEquals(2, changes.size());
        assertChange(changes, "/content/samples/en", ChangeType.CHANGED);
        // this second event is not fully correct, but this is a quite special case, accept it for now 
        assertChange(changes, "/content/samples/en", ChangeType.REMOVED);
    }
    
}
