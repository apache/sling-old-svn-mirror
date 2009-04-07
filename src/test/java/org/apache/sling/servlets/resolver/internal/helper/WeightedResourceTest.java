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
package org.apache.sling.servlets.resolver.internal.helper;

import junit.framework.TestCase;

public class WeightedResourceTest extends TestCase {

    public void testEquality() {
        WeightedResource lr1 = new WeightedResource(0, null, 0, WeightedResource.WEIGHT_NONE);
        WeightedResource lr2 = new WeightedResource(0, null, 0, WeightedResource.WEIGHT_NONE);
        
        // expect same objects to be equal
        assertTrue(lr1.equals(lr1));
        
        // expect different instances to not be equal
        assertFalse(lr1.equals(lr2));
        assertFalse(lr2.equals(lr1));
        assertFalse(lr1.equals(null));
        assertFalse(lr2.equals(null));
    }
    
    public void testCompareToSelectors() {
        WeightedResource lr1 = new WeightedResource(0, null, 1, WeightedResource.WEIGHT_NONE);
        WeightedResource lr2 = new WeightedResource(1, null, 0, WeightedResource.WEIGHT_NONE);
        
        // expect the same objects to compare equal
        assertEquals(0, lr1.compareTo(lr1));
        assertEquals(0, lr2.compareTo(lr2));
        
        assertTrue(lr1.compareTo(lr2) < 0);
        assertTrue(lr2.compareTo(lr1) > 0);
    }
    
    public void testCompareToPrefix() {
        WeightedResource lr1 = new WeightedResource(0, null, 2, WeightedResource.WEIGHT_PREFIX);
        WeightedResource lr2 = new WeightedResource(1, null, 2, WeightedResource.WEIGHT_NONE);
        
        // expect the same objects to compare equal
        assertEquals(0, lr1.compareTo(lr1));
        assertEquals(0, lr2.compareTo(lr2));
        
        assertTrue(lr1.compareTo(lr2) < 0);
        assertTrue(lr2.compareTo(lr1) > 0);
    }
    
    public void testCompareToExtension() {
        WeightedResource lr1 = new WeightedResource(0, null, 2, WeightedResource.WEIGHT_EXTENSION);
        WeightedResource lr2 = new WeightedResource(1, null, 2, WeightedResource.WEIGHT_NONE);
        
        // expect the same objects to compare equal
        assertEquals(0, lr1.compareTo(lr1));
        assertEquals(0, lr2.compareTo(lr2));
        
        assertTrue(lr1.compareTo(lr2) < 0);
        assertTrue(lr2.compareTo(lr1) > 0);
    }
 
    public void testCompareToOrdinal() {
        WeightedResource lr1 = new WeightedResource(0, null, 0, WeightedResource.WEIGHT_NONE);
        WeightedResource lr2 = new WeightedResource(1, null, 0, WeightedResource.WEIGHT_NONE);
        
        // expect the same objects to compare equal
        assertEquals(0, lr1.compareTo(lr1));
        assertEquals(0, lr2.compareTo(lr2));
        
        assertTrue(lr1.compareTo(lr2) < 0);
        assertTrue(lr2.compareTo(lr1) > 0);
    }
}
