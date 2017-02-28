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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

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
 * Test access to files and folders from filesystem.
 */
public class FilesFolderTest {

    private Resource root;
    private Resource fsroot;

    @Rule
    public SlingContext context = new SlingContextBuilder(ResourceResolverType.JCR_MOCK)
        .plugin(new RegisterFsResourcePlugin())
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
        assertFile(fsroot, "folder2/content/file2content.txt", "file2content");
        assertFile(fsroot, "folder3/content.jcr.xml", null);
    }

    @Test
    public void testListChildren() {
        assertThat(root, ResourceMatchers.containsChildren("fs-test"));
        assertThat(fsroot, ResourceMatchers.hasChildren("folder1", "folder2", "folder3"));
        assertThat(fsroot.getChild("folder1"), ResourceMatchers.hasChildren("folder11", "file1a.txt", "file1b.txt"));
        assertThat(fsroot.getChild("folder2"), ResourceMatchers.hasChildren("folder21", "content.json"));
        assertFalse(fsroot.getChild("folder1/file1a.txt").listChildren().hasNext());
    }

}
