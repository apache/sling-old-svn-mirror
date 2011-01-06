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
package org.apache.sling.installer.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.junit.Test;

public class RegisteredResourceComparatorTest {

    private void assertOrder(Set<RegisteredResource> toTest, RegisteredResource[] inOrder) {
        assertEquals("Expected sizes to match", toTest.size(), inOrder.length);
        int i = 0;
        for(RegisteredResource r : toTest) {
            final RegisteredResource ref = inOrder[i];
            assertSame("At index " + i + ", expected toTest and ref to match", ref, r);
            i++;
        }
    }

    private RegisteredResource getConfig(String url, Dictionary<String, Object> data, int priority) throws IOException {
        return getConfig(url, data, priority, null);
    }

    private RegisteredResource getConfig(String url, Dictionary<String, Object> data, int priority, String digest) throws IOException {
        if(data == null) {
            data = new Hashtable<String, Object>();
            data.put("foo", "bar");
        }
        final InstallableResource r = new InstallableResource(url, null, data, digest, null, priority);
        return RegisteredResourceImpl.create(null, r, "test", new FileUtil(new MockBundleContext()));
    }

    private void assertOrder(RegisteredResource[] inOrder) {
        final SortedSet<RegisteredResource> toTest = new TreeSet<RegisteredResource>();
        for(int i = inOrder.length - 1 ; i >= 0; i--) {
            toTest.add(inOrder[i]);
        }
        assertOrder(toTest, inOrder);
        toTest.clear();
        for(RegisteredResource r : inOrder) {
            toTest.add(r);
        }
        assertOrder(toTest, inOrder);
    }

    @Test
    public void testBundleName() {
        final RegisteredResource [] inOrder = {
                new MockBundleResource("a", "1.0", 10),
                new MockBundleResource("b", "1.0", 10),
                new MockBundleResource("c", "1.0", 10),
                new MockBundleResource("d", "1.0", 10),
        };
        assertOrder(inOrder);
    }

    @Test
    public void testBundleVersion() {
        final RegisteredResource [] inOrder = {
                new MockBundleResource("a", "1.2.51", 10),
                new MockBundleResource("a", "1.2.4", 10),
                new MockBundleResource("a", "1.1.0", 10),
                new MockBundleResource("a", "1.0.6", 10),
                new MockBundleResource("a", "1.0.0", 10),
        };
        assertOrder(inOrder);
    }

    @Test
    public void testBundlePriority() {
        final RegisteredResource [] inOrder = {
                new MockBundleResource("a", "1.0.0", 101),
                new MockBundleResource("a", "1.0.0", 10),
                new MockBundleResource("a", "1.0.0", 0),
                new MockBundleResource("a", "1.0.0", -5),
        };
        assertOrder(inOrder);
    }

    @Test
    public void testComposite() {
        final RegisteredResource [] inOrder = {
                new MockBundleResource("a", "1.2.0"),
                new MockBundleResource("a", "1.0.0"),
                new MockBundleResource("b", "1.0.0", 2),
                new MockBundleResource("b", "1.0.0", 0),
                new MockBundleResource("c", "1.5.0", -5),
                new MockBundleResource("c", "1.4.0", 50),
        };
        assertOrder(inOrder);
    }

    @Test
    public void testBundleDigests() {
        final RegisteredResource a = new MockBundleResource("a", "1.2.0", 0, "digestA");
        final RegisteredResource b = new MockBundleResource("a", "1.2.0", 0, "digestB");
        assertEquals("Digests must not be included in bundles comparison", 0, a.compareTo(b));
    }

    @Test
    public void testSnapshotSerialNumber() {
        // Verify that snapshots with a higher serial number come first
        final RegisteredResource [] inOrder = new RegisteredResource [3];
        inOrder[2] = new MockBundleResource("a", "1.2.0.SNAPSHOT", 0, "digestC");
        inOrder[1] = new MockBundleResource("a", "1.2.0.SNAPSHOT", 0, "digestB");
        inOrder[0] = new MockBundleResource("a", "1.2.0.SNAPSHOT", 0, "digestA");
        assertOrder(inOrder);
    }

    @Test
    public void testConfigPriority() throws IOException {
        final RegisteredResource [] inOrder = new RegisteredResource [3];
        inOrder[0] = getConfig("pid", null, 2);
        inOrder[1] = getConfig("pid", null, 1);
        inOrder[2] = getConfig("pid", null, 0);
        assertOrder(inOrder);
    }

    @Test
    public void testConfigDigests() throws IOException {
    	final Dictionary<String, Object> data = new Hashtable<String, Object>();
        data.put("foo", "bar");
        final RegisteredResource a = getConfig("pid", data, 0);
        data.put("foo", "changed");
        final RegisteredResource b = getConfig("pid", data, 0);
        assertEquals("Entity urls must be the same", a.getEntityId(), b.getEntityId());
        assertTrue("Digests must be included in configs comparison", a.compareTo(b) != 0);
        final RegisteredResource a2 = getConfig("pid", data, 0);
        final RegisteredResource b2 = getConfig("pid", data, 0);
        assertEquals("Digests must be included in configs comparison", 0, a2.compareTo(b2));
    }

    @Test
    public void testConfigPid() throws IOException {
        final RegisteredResource [] inOrder = new RegisteredResource [3];
        inOrder[0] = getConfig("pidA", null, 0);
        inOrder[1] = getConfig("pidB", null, 0);
        inOrder[2] = getConfig("pidC", null, 0);
        assertOrder(inOrder);
    }

    @Test
    public void testConfigComposite() throws IOException {
        final RegisteredResource [] inOrder = new RegisteredResource [4];
        inOrder[0] = getConfig("pidA", null, 10);
        inOrder[1] = getConfig("pidA", null, 0);
        inOrder[2] = getConfig("pidB", null, 1);
        inOrder[3] = getConfig("pidB", null, 0);
        assertOrder(inOrder);
    }

    @Test
    public void testConfigAndBundle() throws IOException {
    	final RegisteredResource cfg = getConfig("pid", null, InstallableResource.DEFAULT_PRIORITY);
    	final RegisteredResource b = new MockBundleResource("a", "1.0");
    	assertEquals("bundle is > config when compared", -1, b.compareTo(cfg));
    	assertEquals("config is < bundle when compared", 1, cfg.compareTo(b));
    }
}