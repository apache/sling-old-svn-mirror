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
package org.apache.sling.commons.osgi.bundleversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class BundleVersionComparatorTest {
    
    @Test
    public void testSortBundles() {
        final MockBundleVersionInfo [] sorted = {
                new MockBundleVersionInfo("a.name", "1.1", 1),
                new MockBundleVersionInfo("a.name", "1.0", 1),
                new MockBundleVersionInfo("b", "1.2.0.SNAPSHOT", 2),
                new MockBundleVersionInfo("b", "1.2.0.SNAPSHOT", 1),
                new MockBundleVersionInfo("b", "1.1", 1),
                new MockBundleVersionInfo("b", "1.0.1.SNAPSHOT", 2),
                new MockBundleVersionInfo("b", "1.0.1.SNAPSHOT", 1),
                new MockBundleVersionInfo("b", "1.0", 1),
                new MockBundleVersionInfo("b", "0.9", 1),
                new MockBundleVersionInfo("b", "0.8.1", 1),
                new MockBundleVersionInfo("b", "0.8.0", 1)
        };
        
        final List<BundleVersionInfo<?>> list = new ArrayList<BundleVersionInfo<?>>();
        for(int i = sorted.length - 1 ; i >= 0; i--) {
            list.add(sorted[i]);
        }
        
        final String firstBeforeSort = list.get(0).toString();
        Collections.sort(list, new BundleVersionComparator());
        final String newFirstItem = list.get(0).toString();
        assertFalse("First item (" + newFirstItem + ") must have changed during sort", firstBeforeSort.equals(newFirstItem));
        
        int i = 0;
        for(BundleVersionInfo<?> vi : list) {
            assertEquals("Item sorted as expected at index " + i, sorted[i].toString(), vi.toString());
            i++;
        }
    }
    
    @Test
    public void testEqual() {
        final MockBundleVersionInfo a = new MockBundleVersionInfo("a", "1.0", 2);
        final MockBundleVersionInfo b = new MockBundleVersionInfo("a", "1.0", 1);
        final BundleVersionComparator c = new BundleVersionComparator();
        assertEquals("Last-modified must not be relevant for non-snapshot bundles", 0, c.compare(a, b));
    }
    
    public void testExceptionsOnNull() {
        final MockBundleVersionInfo a = new MockBundleVersionInfo("a", "1.0", 2);
        final BundleVersionComparator c = new BundleVersionComparator();
        
        try {
            c.compare(a, null);
            fail("Expected an IllegalArgumentException");
        } catch(IllegalArgumentException asExpected) {
        }

        try {
            c.compare(null, a);
            fail("Expected an IllegalArgumentException");
        } catch(IllegalArgumentException asExpected) {
        }

        try {
            c.compare(null, null);
            fail("Expected an IllegalArgumentException");
        } catch(IllegalArgumentException asExpected) {
        }
    }
    
    public void testExceptionOnNonBundle() {
        final MockBundleVersionInfo a = new MockBundleVersionInfo("a", "1.0", 2);
        final MockBundleVersionInfo nonBundle = new MockBundleVersionInfo();
        final BundleVersionComparator c = new BundleVersionComparator();
        
        try {
            c.compare(a, nonBundle);
            fail("Expected an IllegalArgumentException");
        } catch(IllegalArgumentException asExpected) {
        }
        
        try {
            c.compare(nonBundle, a);
            fail("Expected an IllegalArgumentException");
        } catch(IllegalArgumentException asExpected) {
        }
    }
}
