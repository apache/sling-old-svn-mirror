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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.fscontentparser.ParserOptions;
import org.apache.sling.fsprovider.internal.TestUtils.RegisterFsResourcePlugin;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Test access to files and folders and JSON content from filesystem.
 */
public class JsonContentTest {

    private Resource root;
    private Resource fsroot;

    @Rule
    public SlingContext context = new SlingContextBuilder(ResourceResolverType.JCR_MOCK)
        .plugin(new RegisterFsResourcePlugin(
                "provider.fs.mode", FsMode.INITIAL_CONTENT.name(),
                "provider.initial.content.import.options", "overwrite:=true;ignoreImportProviders:=jcr.xml"
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
    }

    @Test
    public void testFiles() {
        assertFile(fsroot, "folder1/file1a.txt", "file1a");
        assertFile(fsroot, "folder1/file1b.txt", "file1b");
        assertFile(fsroot, "folder1/folder11/file11a.txt", "file11a");
        assertNull(fsroot.getChild("folder2/content.json"));
        assertFile(fsroot, "folder2/content/file2content.txt", "file2content");
        assertFile(fsroot, "folder3/content.jcr.xml", null);
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
        Resource underTest = fsroot.getChild("folder2/content");
        assertNotNull(underTest);
        assertEquals("app:Page", underTest.getValueMap().get("jcr:primaryType", String.class));
        assertEquals("app:Page", underTest.getResourceType());
        assertThat(underTest, ResourceMatchers.hasChildren("jcr:content"));
    }

    @Test
    public void testContent_Level1() {
        Resource underTest = fsroot.getChild("folder2/content/jcr:content");
        assertNotNull(underTest);
        assertEquals("app:PageContent", underTest.getValueMap().get("jcr:primaryType", String.class));
        assertEquals("sample/components/homepage", underTest.getResourceType());
        assertEquals("sample/components/supertype", underTest.getResourceSuperType());
        assertThat(underTest, ResourceMatchers.hasChildren("par", "header", "newslist", "lead", "image", "carousel", "rightpar"));
    }

    @Test
    public void testContent_Level5() {
        Resource underTest = fsroot.getChild("folder2/content/jcr:content/par/image/file/jcr:content");
        assertNotNull(underTest);
        assertEquals("nt:resource", underTest.getValueMap().get("jcr:primaryType", String.class));
        assertFalse(underTest.listChildren().hasNext());
    }

    @Test
    public void testContent_Datatypes() {
        Resource underTest = fsroot.getChild("folder2/content/toolbar/profiles/jcr:content");
        ValueMap props = underTest.getValueMap();
        
        assertEquals("Profiles", props.get("jcr:title", String.class));
        assertEquals(true, props.get("booleanProp", false));
        assertEquals((Long)1234567890123L, props.get("longProp", Long.class));
        assertEquals((Double)1.2345d, props.get("decimalProp", Double.class), 0.00001d);
        
        assertArrayEquals(new String[] { "aa", "bb", "cc" }, props.get("stringPropMulti", String[].class));
        assertArrayEquals(new Long[] { 1234567890123L, 55L }, props.get("longPropMulti", Long[].class));
    }

    @Test
    public void testContent_Datatypes_JCR() throws RepositoryException {
        Resource underTest = fsroot.getChild("folder2/content/toolbar/profiles/jcr:content");
        ValueMap props = underTest.getValueMap();
        Node node = underTest.adaptTo(Node.class);
        
        assertEquals("/fs-test/folder2/content/toolbar/profiles/jcr:content", node.getPath());
        assertEquals(6, node.getDepth());
        
        assertTrue(node.hasProperty("jcr:title"));
        assertEquals(PropertyType.STRING, node.getProperty("jcr:title").getType());
        assertFalse(node.getProperty("jcr:title").isMultiple());
        assertEquals("jcr:title", node.getProperty("jcr:title").getDefinition().getName());
        assertEquals("/fs-test/folder2/content/toolbar/profiles/jcr:content/jcr:title", node.getProperty("jcr:title").getPath());
        assertEquals("Profiles", node.getProperty("jcr:title").getString());
        assertEquals(PropertyType.BOOLEAN, node.getProperty("booleanProp").getType());
        assertEquals(true, node.getProperty("booleanProp").getBoolean());
        assertEquals(PropertyType.LONG, node.getProperty("longProp").getType());
        assertEquals(1234567890123L, node.getProperty("longProp").getLong());
        assertEquals(PropertyType.DOUBLE, node.getProperty("decimalProp").getType());
        assertEquals(1.2345d, node.getProperty("decimalProp").getDouble(), 0.00001d);
        
        assertEquals(PropertyType.STRING, node.getProperty("stringPropMulti").getType());
        assertTrue(node.getProperty("stringPropMulti").isMultiple());
        Value[] stringPropMultiValues = node.getProperty("stringPropMulti").getValues();
        assertEquals(3, stringPropMultiValues.length);
        assertEquals("aa", stringPropMultiValues[0].getString());
        assertEquals("bb", stringPropMultiValues[1].getString());
        assertEquals("cc", stringPropMultiValues[2].getString());

        assertEquals(PropertyType.LONG, node.getProperty("longPropMulti").getType());
        assertTrue(node.getProperty("longPropMulti").isMultiple());
        Value[] longPropMultiValues = node.getProperty("longPropMulti").getValues();
        assertEquals(2, longPropMultiValues.length);
        assertEquals(1234567890123L, longPropMultiValues[0].getLong());
        assertEquals(55L, longPropMultiValues[1].getLong());
        
        // assert property iterator
        Set<String> propertyNames = new HashSet<>();
        PropertyIterator propertyIterator = node.getProperties();
        while (propertyIterator.hasNext()) {
            propertyNames.add(propertyIterator.nextProperty().getName());
        }
        assertTrue(props.keySet().containsAll(propertyNames));

        // assert node iterator
        Set<String> nodeNames = new HashSet<>();
        NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            nodeNames.add(nodeIterator.nextNode().getName());
        }
        assertEquals(ImmutableSet.of("par", "rightpar"), nodeNames);
        
        // node hierarchy
        assertTrue(node.hasNode("rightpar"));
        Node rightpar = node.getNode("rightpar");
        assertEquals(7, rightpar.getDepth());
        Node parent = rightpar.getParent();
        assertTrue(node.isSame(parent));
        Node ancestor = (Node)rightpar.getAncestor(5);
        assertEquals(underTest.getParent().getPath(), ancestor.getPath());
        Node root = (Node)rightpar.getAncestor(0);
        assertEquals("/", root.getPath());
        
        // node types
        assertTrue(node.isNodeType("app:PageContent"));
        assertEquals("app:PageContent", node.getPrimaryNodeType().getName());
        assertFalse(node.getPrimaryNodeType().isMixin());
        NodeType[] mixinTypes = node.getMixinNodeTypes();
        assertEquals(2, mixinTypes.length);
        assertEquals("type1", mixinTypes[0].getName());
        assertEquals("type2", mixinTypes[1].getName());
        assertTrue(mixinTypes[0].isMixin());
        assertTrue(mixinTypes[1].isMixin());
    }

    @Test
    public void testFallbackNodeType() throws RepositoryException {
        Resource underTest = fsroot.getChild("folder2/content/jcr:content/par/title_2");
        assertEquals(ParserOptions.DEFAULT_PRIMARY_TYPE, underTest.adaptTo(Node.class).getPrimaryNodeType().getName());
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
    public void testFolder2ChildNodes() throws RepositoryException {
        Resource folder2 = fsroot.getChild("folder2");
        List<Resource> children = ImmutableList.copyOf(folder2.listChildren());
        
        assertEquals(2, children.size());
        Resource child1 = children.get(0);
        assertEquals("content", child1.getName());
        assertEquals("app:Page", child1.getResourceType());
        assertEquals("app:Page", child1.getValueMap().get("jcr:primaryType", String.class));

        Resource child2 = children.get(1);
        assertEquals("folder21", child2.getName());
        assertEquals("nt:folder", child2.getValueMap().get("jcr:primaryType", String.class));
    }

}
