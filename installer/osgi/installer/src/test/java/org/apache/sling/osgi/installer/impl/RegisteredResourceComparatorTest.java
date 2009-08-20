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
package org.apache.sling.osgi.installer.impl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class RegisteredResourceComparatorTest {
    
    private void assertOrder(List<RegisteredResource> toTest, RegisteredResource[] inOrder) {
        assertEquals("Expected sizes to match", toTest.size(), inOrder.length);
        Collections.sort(toTest, new RegisteredResourceComparator());
        int i = 0;
        for(RegisteredResource r : toTest) {
            final RegisteredResource ref = inOrder[i];
            assertSame("At index " + i + ", expected toTest and ref to match", ref, r);
            i++;
        }
    }
    
    private void assertOrder(RegisteredResource[] inOrder) {
        final List<RegisteredResource> toTest = new ArrayList<RegisteredResource>();
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
    public void testDigest() {
        final RegisteredResource [] inOrder = {
                new MockBundleResource("a", "1.2.0", 0, "digestA"),
                new MockBundleResource("a", "1.2.0", 0, "digestB"),
                new MockBundleResource("a", "1.2.0", 0, "digestC"),
        };
        assertOrder(inOrder);
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
}