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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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
 * Test access to files and folders from file system.
 */
public class JcrXmlContentTest {

    private Resource root;
    private Resource fsroot;

    @Rule
    public SlingContext context = new SlingContextBuilder(ResourceResolverType.JCR_MOCK)
            .plugin(new RegisterFsResourcePlugin(
                    "provider.fs.mode", FsMode.INITIAL_CONTENT.name(),
                    "provider.initial.content.import.options", "overwrite:=true;ignoreImportProviders:=json"
                    ))
        .build();

    @Before
    public void setUp() {
        root = context.resourceResolver().getResource("/");
        fsroot = context.resourceResolver().getResource("/fs-test");
    }

    @Test
    public void testFolders() {
        assertFolder(fsroot, "folder1");
        assertFolder(fsroot, "folder1/folder11");
        assertFolder(fsroot, "folder2");
        assertFolder(fsroot, "folder3");
    }

    @Test
    public void testFiles() {
        assertFile(fsroot, "folder1/file1a.txt", "file1a");
        assertFile(fsroot, "folder1/file1b.txt", "file1b");
        assertFile(fsroot, "folder1/folder11/file11a.txt", "file11a");
        assertFile(fsroot, "folder2/content.json", null);
        assertNull(fsroot.getChild("folder3/content.jcr.xml"));
    }

    @Test
    public void testListChildren() {
        assertThat(root, ResourceMatchers.containsChildren("fs-test"));
        assertThat(fsroot, ResourceMatchers.hasChildren("folder1", "folder2"));
        assertThat(fsroot.getChild("folder1"), ResourceMatchers.hasChildren("folder11", "file1a.txt", "file1b.txt"));
        assertThat(fsroot.getChild("folder2"), ResourceMatchers.hasChildren("folder21", "content"));
    }

    @Test
    public void testContent_Root() {
        Resource underTest = fsroot.getChild("folder3/content");
        assertNotNull(underTest);
        assertEquals("app:Page", underTest.getValueMap().get("jcr:primaryType", String.class));
        assertEquals("app:Page", underTest.getResourceType());
        assertThat(underTest, ResourceMatchers.hasChildren("jcr:content"));
    }

    @Test
    public void testContent_Level1() {
        Resource underTest = fsroot.getChild("folder3/content/jcr:content");
        assertNotNull(underTest);
        assertEquals("app:PageContent", underTest.getValueMap().get("jcr:primaryType", String.class));
        assertEquals("samples/sample-app/components/content/page/homepage", underTest.getResourceType());
        assertThat(underTest, ResourceMatchers.hasChildren("teaserbar", "aside", "content"));
    }

    @Test
    public void testContent_Level3() {
        Resource underTest = fsroot.getChild("folder3/content/jcr:content/content/contentheadline");
        assertNotNull(underTest);
        assertEquals("nt:unstructured", underTest.getValueMap().get("jcr:primaryType", String.class));
        assertEquals("samples/sample-app/components/content/common/contentHeadline", underTest.getResourceType());
        assertFalse(underTest.listChildren().hasNext());
    }

    @Test
    public void testContent_Datatypes() {
        Resource underTest = fsroot.getChild("folder3/content/jcr:content");
        ValueMap props = underTest.getValueMap();
        
        assertEquals("en", props.get("jcr:title", String.class));
        assertEquals(true, props.get("includeAside", false));
        assertEquals((Long)1234567890123L, props.get("longProp", Long.class));
        assertEquals((Double)1.2345d, props.get("decimalProp", Double.class), 0.00001d);
        
        assertArrayEquals(new String[] { "aa", "bb", "cc" }, props.get("stringPropMulti", String[].class));
        assertArrayEquals(new Long[] { 1234567890123L, 55L }, props.get("longPropMulti", Long[].class));
    }

    @Test
    public void testContent_InvalidPath() {
        Resource underTest = fsroot.getChild("folder2/content/jcr:content/xyz");
        assertNull(underTest);
    }

    @Test
    public void testJcrMixedContent() throws RepositoryException {
        // prepare mixed JCR content
        Node node = root.adaptTo(Node.class);
        Node fstest = node.addNode("fs-test", "nt:folder");
        fstest.addNode("folder99", "nt:folder");

        assertNull(fsroot.getChild("folder99"));
    }

    @Test
    public void testFolder3ChildNodes() throws RepositoryException {
        Resource folder3 = fsroot.getChild("folder3");
        List<Resource> children = ImmutableList.copyOf(folder3.listChildren());
        
        assertEquals(2, children.size());
        Resource child1 = children.get(0);
        assertEquals("content", child1.getName());
        assertEquals("app:Page", child1.getResourceType());
        assertEquals("app:Page", child1.getValueMap().get("jcr:primaryType", String.class));

        Resource child2 = children.get(1);
        assertEquals("folder31", child2.getName());
        assertEquals("nt:folder", child2.getValueMap().get("jcr:primaryType", String.class));
    }

}
