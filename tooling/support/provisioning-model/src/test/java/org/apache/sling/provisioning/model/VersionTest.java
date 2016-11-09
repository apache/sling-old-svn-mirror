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
package org.apache.sling.provisioning.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {

    @Test
    public void testSameVersion() {
        final String v1 = "1";
        final String v10 = "1.0";
        final String v100 = "1.0.0";

        final Version ve1 = new Version(v1);
        final Version ve10 = new Version(v10);
        final Version ve100 = new Version(v100);

        assertEquals(0, ve1.compareTo(ve10));
        assertEquals(0, ve10.compareTo(ve100));
        assertEquals(0, ve1.compareTo(ve100));
        assertEquals(0, ve10.compareTo(ve1));
        assertEquals(0, ve100.compareTo(ve10));
        assertEquals(0, ve100.compareTo(ve1));
    }

    @Test
    public void testVersions() {
        final String v1 = "1";
        final String v20 = "2.0";
        final String v150 = "1.5.0";

        final Version ve1 = new Version(v1);
        final Version ve20 = new Version(v20);
        final Version ve150 = new Version(v150);

        assertTrue(ve1.compareTo(ve20) < 0);
        assertTrue(ve20.compareTo(ve150) > 0);
        assertTrue(ve1.compareTo(ve150) < 0);
        assertTrue(ve20.compareTo(ve1) > 0);
        assertTrue(ve150.compareTo(ve20) < 0);
        assertTrue(ve150.compareTo(ve1) > 0);
    }

    @Test
    public void testSnapshotQualifier() {
        final Version v1 = new Version("1");
        final Version v1snapshot = new Version("1-SNAPSHOT");
        final Version v1a = new Version("1-A");

        // snapshot is lower than the corresponding version
        assertTrue(v1.compareTo(v1snapshot) > 0);
        assertTrue(v1snapshot.compareTo(v1) < 0);

        // qualifier is higher than the version
        assertTrue(v1a.compareTo(v1) > 0);
        assertTrue(v1.compareTo(v1a) < 0);

        // qualifier is higher than snapshot
        assertTrue(v1a.compareTo(v1snapshot) > 0);
        assertTrue(v1snapshot.compareTo(v1a) < 0);
    }

    @Test
    public void testQualifiers() {
        final Version va = new Version("1-A");
        final Version vb = new Version("1-B");

        assertTrue(va.compareTo(vb) < 0);
        assertTrue(vb.compareTo(va) > 0);
    }

    @Test
    public void testOSGiVersion() {
        final Version v = new Version("1.5.2.SNAPSHOT");
        assertEquals(1, v.getMajorVersion());
        assertEquals(5, v.getMinorVersion());
        assertEquals(2, v.getMicroVersion());
        assertEquals("SNAPSHOT", v.getQualifier());
    }
}
