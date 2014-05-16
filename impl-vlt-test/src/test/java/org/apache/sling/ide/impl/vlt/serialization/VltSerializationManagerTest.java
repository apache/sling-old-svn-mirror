/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.impl.vlt.serialization;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class VltSerializationManagerTest {

    private VltSerializationManager serializationManager;

    @Rule
    public TemporaryFolder trash = new TemporaryFolder();

    @Before
    public void init() {
        serializationManager = new VltSerializationManager();
    }

    @Test
    public void getSerializationFilePath_Root() {

        File root = findFilesystemRoot();

        assertThat(serializationManager.getBaseResourcePath(new File(root, ".content.xml").getAbsolutePath()),
                is(root.getAbsolutePath()));
    }

    private File findFilesystemRoot() {
        File[] roots = File.listRoots();
        Assume.assumeTrue("No filesystem roots found", roots != null && roots.length > 0);
        return roots[0];
    }

    @Test
    public void getSerializationFilePath_NestedPath() {

        File f = newFile(findFilesystemRoot(), "apps", "sling", "servlet", "default", ".content.xml");

        assertThat(serializationManager.getBaseResourcePath(f.getAbsolutePath()), is(f.getParentFile()
                .getAbsolutePath()));
    }

    private File newFile(File parent, String... segments) {

        File current = parent;
        for (String segment : segments) {
            current = new File(parent, segment);
        }
        return current;
    }

    @Test
    public void getSerializationFilePath_FullCoverageAggerate() throws IOException {

        File contentFile = trash.newFile("default.xml");
        InputStream in = getClass().getResourceAsStream("simple-content.xml");
        FileOutputStream out = new FileOutputStream(contentFile);
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }

        assertThat(serializationManager.getBaseResourcePath(contentFile.getAbsolutePath()),
                is(new File(contentFile.getParent(), "default").getAbsolutePath()));
    }

    @Test
    public void getSerializationFilePath_XmlFile() throws IOException {

        File contentFile = trash.newFile("file.xml");
        InputStream in = getClass().getResourceAsStream("file.xml");
        FileOutputStream out = new FileOutputStream(contentFile);
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }

        assertThat(serializationManager.getBaseResourcePath(contentFile.getAbsolutePath()),
                is(contentFile.getAbsolutePath()));
    }
}
