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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.fsprovider.internal.TestUtils.RegisterFsResourcePlugin;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test access mixed with JCR content on same path.
 */
public class JcrMixedTest {

    private Resource root;
    private Resource fsroot;

    @Rule
    public SlingContext context = new SlingContextBuilder(ResourceResolverType.JCR_MOCK)
        .plugin(new RegisterFsResourcePlugin())
        .build();

    @Before
    public void setUp() throws RepositoryException {
        root = context.resourceResolver().getResource("/");
        fsroot = context.resourceResolver().getResource("/fs-test");
        
        // prepare mixed JCR content
        Node node = root.adaptTo(Node.class);
        Node fstest = node.addNode("fs-test", "nt:folder");
        // folder1
        Node folder1 = fstest.addNode("folder1", "nt:folder");
        folder1.setProperty("prop1", "value1");
        folder1.setProperty("prop2", 123L);
        // folder1/file1a.txt
        Node file1a = folder1.addNode("file1a.txt", "nt:file");
        file1a.setProperty("prop1", "value2");
        file1a.setProperty("prop2", 234L);
        // folder1/file1c.txt
        folder1.addNode("file1c.txt", "nt:file");
        // folder99
        fstest.addNode("folder99", "nt:folder");
    }

    @Test
    public void testFolders() {
        // expected properties from JCR for folders
        Resource folder1 = fsroot.getChild("folder1");
        assertThat(folder1, ResourceMatchers.props("jcr:primaryType", "nt:folder",
                "prop1", "value1",
                "prop2", 123L));
    }

    @Test
    public void testFiles() {
        assertFile(fsroot, "folder1/file1a.txt", "file1a");
        assertFile(fsroot, "folder1/file1b.txt", "file1b");
        assertFile(fsroot, "folder1/folder11/file11a.txt", "file11a");
        assertFile(fsroot, "folder2/content.json", null);

        // do not expected properties from JCR for files
        Resource file1a = fsroot.getChild("folder1/file1a.txt");
        assertThat(file1a, not(ResourceMatchers.props(
                "prop1", "value2",
                "prop2", 234L)));
    }

    @Test
    public void testListChildren() {
        assertThat(root, ResourceMatchers.containsChildren("fs-test"));
        assertThat(fsroot, ResourceMatchers.hasChildren("folder1", "folder2", "folder99"));
        assertThat(fsroot.getChild("folder1"), ResourceMatchers.hasChildren("file1a.txt", "file1b.txt", "file1c.txt"));
    }

}
