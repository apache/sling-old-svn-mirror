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
package org.apache.sling.commons.osgi;

import junit.framework.TestCase;

/**
 * Tests for the manifest header parsing.
 */
public class ManifestHeaderTest extends TestCase {

    public void testNonExisting() {
        String header = null;
        final ManifestHeader entry = ManifestHeader.parse(header);
        assertNull(entry);
    }

    public void testSinglePath() {
        String header = "something";
        final ManifestHeader entry = ManifestHeader.parse(header);
        assertEquals(1, entry.getEntries().length);
        assertEquals(header, entry.getEntries()[0].getValue());
        assertEquals(0, entry.getEntries()[0].getAttributes().length);
        assertEquals(0, entry.getEntries()[0].getDirectives().length);
    }

    public void testSeveralPaths() {
        String header = "one,two,   three    ,\n   four, \n   five";
        final ManifestHeader entry = ManifestHeader.parse(header);
        assertEquals(5, entry.getEntries().length);
        assertEquals("one", entry.getEntries()[0].getValue());
        assertEquals(0, entry.getEntries()[0].getAttributes().length);
        assertEquals(0, entry.getEntries()[0].getDirectives().length);
        assertEquals("two", entry.getEntries()[1].getValue());
        assertEquals(0, entry.getEntries()[1].getAttributes().length);
        assertEquals(0, entry.getEntries()[1].getDirectives().length);
        assertEquals("three", entry.getEntries()[2].getValue());
        assertEquals(0, entry.getEntries()[2].getAttributes().length);
        assertEquals(0, entry.getEntries()[2].getDirectives().length);
        assertEquals("four", entry.getEntries()[3].getValue());
        assertEquals(0, entry.getEntries()[3].getAttributes().length);
        assertEquals(0, entry.getEntries()[3].getDirectives().length);
        assertEquals("five", entry.getEntries()[4].getValue());
        assertEquals(0, entry.getEntries()[4].getAttributes().length);
        assertEquals(0, entry.getEntries()[4].getDirectives().length);
    }

    public void testAttributes() {
        String header = "one;a=1;b=2";
        final ManifestHeader entry = ManifestHeader.parse(header);
        assertEquals(1, entry.getEntries().length);
        assertEquals("one", entry.getEntries()[0].getValue());
        assertEquals(2, entry.getEntries()[0].getAttributes().length);
        assertEquals(0, entry.getEntries()[0].getDirectives().length);
        assertEquals("a", entry.getEntries()[0].getAttributes()[0].getName());
        assertEquals("b", entry.getEntries()[0].getAttributes()[1].getName());
        assertEquals("1", entry.getEntries()[0].getAttributes()[0].getValue());
        assertEquals("2", entry.getEntries()[0].getAttributes()[1].getValue());
    }

    public void testDirectives() {
        String header = "one;a:=1;b:=2";
        final ManifestHeader entry = ManifestHeader.parse(header);
        assertEquals(1, entry.getEntries().length);
        assertEquals("one", entry.getEntries()[0].getValue());
        assertEquals(2, entry.getEntries()[0].getDirectives().length);
        assertEquals(0, entry.getEntries()[0].getAttributes().length);
        assertEquals("a", entry.getEntries()[0].getDirectives()[0].getName());
        assertEquals("b", entry.getEntries()[0].getDirectives()[1].getName());
        assertEquals("1", entry.getEntries()[0].getDirectives()[0].getValue());
        assertEquals("2", entry.getEntries()[0].getDirectives()[1].getValue());
    }

    public void testQuoting() {
        String header = "one;a:=\"1,2\"";
        final ManifestHeader entry = ManifestHeader.parse(header);
        assertEquals(1, entry.getEntries().length);
        assertEquals("one", entry.getEntries()[0].getValue());
        assertEquals(1, entry.getEntries()[0].getDirectives().length);
        assertEquals(0, entry.getEntries()[0].getAttributes().length);
        assertEquals("a", entry.getEntries()[0].getDirectives()[0].getName());
        assertEquals("1,2", entry.getEntries()[0].getDirectives()[0].getValue());
    }

    public void testQuoting2() {
        String header = "CQ-INF/content/apps/xyz/docroot;overwrite:=true;path:=/apps/xyz/docroot;ignoreImportProviders:=\"json,xml\"";
        final ManifestHeader entry = ManifestHeader.parse(header);
        assertEquals(1, entry.getEntries().length);
        assertEquals("CQ-INF/content/apps/xyz/docroot", entry.getEntries()[0].getValue());
        assertEquals(3, entry.getEntries()[0].getDirectives().length);
        assertEquals(0, entry.getEntries()[0].getAttributes().length);
        assertEquals("overwrite", entry.getEntries()[0].getDirectives()[0].getName());
        assertEquals("true", entry.getEntries()[0].getDirectives()[0].getValue());
        assertEquals("path", entry.getEntries()[0].getDirectives()[1].getName());
        assertEquals("/apps/xyz/docroot", entry.getEntries()[0].getDirectives()[1].getValue());
        assertEquals("ignoreImportProviders", entry.getEntries()[0].getDirectives()[2].getName());
        assertEquals("json,xml", entry.getEntries()[0].getDirectives()[2].getValue());
    }


    public void testMultipleEntries() {
        String header = "SLING-INF/content/etc;checkin:=true;path:=/etc,\nSLING-INF/content/libs;overwrite:=true;path:=/libs";
        final ManifestHeader entry = ManifestHeader.parse(header);
        assertEquals(2, entry.getEntries().length);
        assertEquals("SLING-INF/content/etc", entry.getEntries()[0].getValue());
        assertEquals(2, entry.getEntries()[0].getDirectives().length);
        assertEquals(0, entry.getEntries()[0].getAttributes().length);
        assertEquals("checkin", entry.getEntries()[0].getDirectives()[0].getName());
        assertEquals("path", entry.getEntries()[0].getDirectives()[1].getName());
        assertEquals("true", entry.getEntries()[0].getDirectives()[0].getValue());
        assertEquals("/etc", entry.getEntries()[0].getDirectives()[1].getValue());
        assertEquals("SLING-INF/content/libs", entry.getEntries()[1].getValue());
        assertEquals(2, entry.getEntries()[1].getDirectives().length);
        assertEquals(0, entry.getEntries()[1].getAttributes().length);
        assertEquals("overwrite", entry.getEntries()[1].getDirectives()[0].getName());
        assertEquals("path", entry.getEntries()[1].getDirectives()[1].getName());
        assertEquals("true", entry.getEntries()[1].getDirectives()[0].getValue());
        assertEquals("/libs", entry.getEntries()[1].getDirectives()[1].getValue());
    }
}
