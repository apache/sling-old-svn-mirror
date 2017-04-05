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

import static org.apache.sling.fsprovider.internal.TestUtils.assertFile;
import static org.apache.sling.fsprovider.internal.TestUtils.assertFolder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.fsprovider.internal.TestUtils.RegisterFsResourcePlugin;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test access FileFault XML files, folders and content.
 */
public class FileVaultContentTest {

    private Resource damAsset;
    private Resource sampleContent;

    @Rule
    public SlingContext context = new SlingContextBuilder(ResourceResolverType.JCR_MOCK)
        .plugin(new RegisterFsResourcePlugin(
                "provider.fs.mode", FsMode.FILEVAULT_XML.name(),
                "provider.file", "src/test/resources/vaultfs-test/jcr_root",
                "provider.filevault.filterxml.path", "src/test/resources/vaultfs-test/META-INF/vault/filter.xml",
                "provider.root", "/content/dam/talk.png"
                ))
        .plugin(new RegisterFsResourcePlugin(
                "provider.fs.mode", FsMode.FILEVAULT_XML.name(),
                "provider.file", "src/test/resources/vaultfs-test/jcr_root",
                "provider.filevault.filterxml.path", "src/test/resources/vaultfs-test/META-INF/vault/filter.xml",
                "provider.root", "/content/samples"
                ))
        .build();

    @Before
    public void setUp() {
        damAsset = context.resourceResolver().getResource("/content/dam/talk.png");
        sampleContent = context.resourceResolver().getResource("/content/samples");
    }

    @Test
    public void testDamAsset() {
        assertNotNull(damAsset);
        assertEquals("app:Asset", damAsset.getResourceType());
        
        Resource content = damAsset.getChild("jcr:content");
        assertNotNull(content);
        assertEquals("app:AssetContent", content.getResourceType());
        
        Resource metadata = content.getChild("metadata");
        assertNotNull(metadata);
        ValueMap props = metadata.getValueMap();
        assertEquals((Integer)4, props.get("app:Bitsperpixel", Integer.class));
        
        assertFolder(content, "renditions");
        assertFile(content, "renditions/original", null);
        assertFile(content, "renditions/web.1280.1280.png", null);
    }

    @Test
    public void testSampleContent() {
        assertNotNull(sampleContent);
        assertEquals("sling:OrderedFolder", sampleContent.getResourceType());

        Resource enContent = sampleContent.getChild("en/jcr:content");
        assertArrayEquals(new String[] { "/etc/mobile/groups/responsive" }, enContent.getValueMap().get("app:deviceGroups", String[].class));
    }

    @Test
    public void testListChildren() {
        Resource en = sampleContent.getChild("en");
        List<Resource> children = ImmutableList.copyOf(en.listChildren());
        assertEquals(2, children.size());
        
        Resource child1 = children.get(0);
        assertEquals("jcr:content", child1.getName());
        assertEquals("samples/sample-app/components/content/page/homepage", child1.getResourceType());
 
        Resource child2 = children.get(1);
        assertEquals("tools", child2.getName());
        assertEquals("app:Page", child2.getResourceType());
        
        // child3 (conference) is hidden because of filter
    }

    @Test
    public void testJcrMixedContent() throws RepositoryException {
        // prepare mixed JCR content
        Node root = context.resourceResolver().adaptTo(Session.class).getNode("/");
        Node content = root.addNode("content", "nt:folder");
        Node samples = content.addNode("samples", "nt:folder");
        Node en = samples.addNode("en", "nt:folder");
        Node conference = en.addNode("conference", "nt:folder");
        conference.addNode("page2", "nt:folder");
        samples.addNode("it", "nt:folder");
        
        // pass-through because of filter
        assertNotNull(context.resourceResolver().getResource("/content/samples/en/conference"));
        assertNotNull(sampleContent.getChild("en/conference"));
        assertNotNull(context.resourceResolver().getResource("/content/samples/en/conference/page2"));
        assertNotNull(sampleContent.getChild("en/conference/page2"));
        
        // hidden because overlayed by resource provider
        assertNull(context.resourceResolver().getResource("/content/samples/it"));
        assertNull(sampleContent.getChild("it"));

        // list children with mixed content
        Resource enResource = sampleContent.getChild("en");
        assertThat(enResource, ResourceMatchers.containsChildren("jcr:content", "tools", "conference"));
    }

}
