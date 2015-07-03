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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.junit.Test;

public class RegisteredResourceComparatorTest {

    private void assertOrder(final Set<RegisteredResource> toTest, final RegisteredResource[] inOrder) {
        assertEquals("Expected sizes to match", toTest.size(), inOrder.length);
        int i = 0;
        for(final RegisteredResource r : toTest) {
            final RegisteredResource ref = inOrder[i];
            assertSame("At index " + i + ", expected toTest and ref to match.", ref, r);
            i++;
        }
    }

    private RegisteredResourceImpl getConfig(String url, Dictionary<String, Object> data, int priority) throws IOException {
        return getConfig(url, data, priority, null);
    }

    private RegisteredResourceImpl getConfig(String url, Dictionary<String, Object> data, int priority, String digest) throws IOException {
        return getConfig(url, data, priority, digest, null);
    }

    private RegisteredResourceImpl getConfig(String url, Dictionary<String, Object> data, int priority, String digest, ResourceState state) throws IOException {
        if (data == null) {
            data = new Hashtable<String, Object>();
            data.put("foo", "bar");
        }
        new FileDataStore(new MockBundleContext());
        final InstallableResource r = new InstallableResource(url, null, data, digest, null, priority);
        final InternalResource internal = InternalResource.create("test", r);
        final RegisteredResourceImpl rr = RegisteredResourceImpl.create(internal);
        TransformationResult[] tr = new DefaultTransformer().transform(rr);
        if ( tr == null ) {
            final TransformationResult result = new TransformationResult();
            result.setId(url);
            result.setResourceType(InstallableResource.TYPE_CONFIG);
            tr = new TransformationResult[] {
                      result
            };
        }
        final RegisteredResourceImpl result = (RegisteredResourceImpl)rr.clone(tr[0]);
        if ( state != null ) {
            result.setState(state);
        }
        return result;
    }
    
    private RegisteredResource untransformedResource(String id, int prio) throws IOException {
        final ByteArrayInputStream is = new ByteArrayInputStream(id.getBytes("UTF-8")); 
        final InstallableResource r = new InstallableResource(id, is, null, id, id, prio);
        final InternalResource internal = InternalResource.create("test", r);
        return RegisteredResourceImpl.create(internal);
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
        final MockBundleResource a = new MockBundleResource("a", "1.2.0", 0, "digestA");
        final MockBundleResource b = new MockBundleResource("a", "1.2.0", 0, "digestB");
        assertEquals("Digests must not be included in bundles comparison", 0, a.compareTo(b));
        final MockBundleResource c = new MockBundleResource("a", "1.2.0.SNAPSHOT", 0, "1000");
        final MockBundleResource d = new MockBundleResource("a", "1.2.0.SNAPSHOT", 0, "2000");
        assertEquals("Digests should be compared with highest first", 1, c.compareTo(d));
    }

    @Test
    public void testSnapshotSerialNumber() {
        // Verify that snapshots with a higher serial number come first
        final RegisteredResource [] inOrder = new RegisteredResource [3];
        inOrder[0] = new MockBundleResource("a", "1.2.0.SNAPSHOT", 0, "digestC");
        inOrder[1] = new MockBundleResource("a", "1.2.0.SNAPSHOT", 0, "digestB");
        inOrder[2] = new MockBundleResource("a", "1.2.0.SNAPSHOT", 0, "digestA");
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
        final RegisteredResourceImpl a = getConfig("pid", data, 0);
        data.put("foo", "changed");
        final RegisteredResourceImpl b = getConfig("pid", data, 0);
        assertEquals("Entity urls must be the same", a.getEntityId(), b.getEntityId());
        assertTrue("Digests must be included in configs comparison", a.compareTo(b) != 0);
        final RegisteredResourceImpl a2 = getConfig("pid", data, 0);
        final RegisteredResourceImpl b2 = getConfig("pid", data, 0);
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
    public void testConfigState() throws IOException {
        final RegisteredResource [] inOrder = new RegisteredResource [2];
        inOrder[0] = getConfig("pidA", null, 100, "a", ResourceState.INSTALLED);
        inOrder[1] = getConfig("pidA", null, 100, "b", ResourceState.INSTALL);
        assertOrder(inOrder);
    }
    
    @Test
    public void testNullEntityId() throws IOException {
        final SortedSet<RegisteredResource> set = new TreeSet<RegisteredResource>();
        final RegisteredResource a = untransformedResource("a", 1);
        final RegisteredResource b = untransformedResource("b", 1);
        set.add(a);
        set.add(b);
        assertEquals("Expecting a to be first", a, set.first());
    }
}