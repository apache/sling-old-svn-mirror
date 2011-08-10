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
package org.apache.sling.launchpad.base.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

/**
 * Testing the bootstrap installer methods
 */
public class BootstrapInstallerTest {

    /**
     * Test method for
     * {@link org.apache.sling.launchpad.base.impl.BootstrapInstaller#extractFileName(java.lang.String)}
     * .
     */
    @Test
    public void testExtractFileNameForwardSlash() {
        String filename = BootstrapInstaller.extractFileName("myfile.html");
        assertEquals("myfile.html", filename);

        filename = BootstrapInstaller.extractFileName("/things/myfile.html");
        assertEquals("myfile.html", filename);

        filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/myfile.html");
        assertEquals("myfile.html", filename);

        try {
            filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/");
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/");
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            filename = BootstrapInstaller.extractFileName(null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link org.apache.sling.launchpad.base.impl.BootstrapInstaller#extractFileName(java.lang.String)}
     * .
     */
    @Test
    public void testExtractFileNameSeparatorChar() {
        String filename = BootstrapInstaller.extractFileName("myfile.html".replace('/', File.separatorChar));
        assertEquals("myfile.html", filename);

        filename = BootstrapInstaller.extractFileName("/things/myfile.html".replace('/', File.separatorChar));
        assertEquals("myfile.html", filename);

        filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/myfile.html".replace('/', File.separatorChar));
        assertEquals("myfile.html", filename);

        try {
            filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/".replace('/', File.separatorChar));
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            filename = BootstrapInstaller.extractFileName("LOTS/of/random/things/".replace('/', File.separatorChar));
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            filename = BootstrapInstaller.extractFileName(null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link org.apache.sling.launchpad.base.impl.BootstrapInstaller#copyStreamToFile(java.io.InputStream, java.io.File)}
     * .
     */
    @Test
    public void testCopyStreamToFile() {
        InputStream stream = null;
        File to = null;
        File testDir = new File("testing");
        testDir.deleteOnExit(); // cleanup
        assertTrue(testDir.mkdir());

        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
            "holaworld.jar");
        assertNotNull(stream); // cleanup
        to = new File(testDir, "test.jar");
        to.deleteOnExit();
        try {
            BootstrapInstaller.copyStreamToFile(stream, to);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        File copy = new File(testDir, "test.jar");
        try {
            FileInputStream copyStream = new FileInputStream(copy);
            byte[] copyData = new byte[copyStream.available()];
            copyStream.read(copyData);
            copyStream.close();
            FileInputStream origStream = new FileInputStream(copy);
            byte[] origData = new byte[origStream.available()];
            origStream.read(origData);
            origStream.close();
            assertArrayEquals(copyData, origData);
        } catch (FileNotFoundException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        try {
            BootstrapInstaller.copyStreamToFile(null, to);
            fail("should have thrown exception");
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        try {
            BootstrapInstaller.copyStreamToFile(stream, null);
            fail("should have thrown exception");
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link org.apache.sling.launchpad.base.impl.BootstrapInstaller#isBlank(java.lang.String)}
     * .
     */
    @Test
    public void testIsBlank() {
        assertTrue(BootstrapInstaller.isBlank(null));
        assertTrue(BootstrapInstaller.isBlank(""));
        assertTrue(BootstrapInstaller.isBlank(" "));

        assertFalse(BootstrapInstaller.isBlank("Test"));
        assertFalse(BootstrapInstaller.isBlank(" asdf "));
    }

    // TODO eventually add in tests that create a context so we can test more
    // things in detail

}
