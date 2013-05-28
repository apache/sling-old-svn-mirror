/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.api;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class RuleTagsTest {
    private Rule r;
    
    @Before
    public void setup() {
        r = new Rule(null, null);
    }
    
    @Test
    public void testNoTags() {
        assertTrue(r.getTags().isEmpty());
    }
    
    @Test
    public void testEmptyTagsList() {
        r.setTags();
        assertTrue(r.getTags().isEmpty());
    }
    
    @Test
    public void testSingleTag() {
        r.setTags("foo");
        assertEquals(1, r.getTags().size());
        assertTrue(r.hasTag("foo"));
        assertFalse(r.hasTag("absent"));
    }
    
    @Test
    public void testSeveralTags() {
        r.setTags("foo", "bar", "BAR");
        assertEquals(2, r.getTags().size());
        assertTrue(r.hasTag("foo"));
        assertTrue(r.hasTag("bar"));
        assertFalse(r.hasTag("BAR"));
    }
    
    @Test
    public void testReplaceTags() {
        r.setTags("foo", "bar", "BAR");
        assertEquals(2, r.getTags().size());
        assertTrue(r.hasTag("foo"));
        assertTrue(r.hasTag("bar"));
        assertFalse(r.hasTag("BAR"));
        
        r.setTags("one", "two", "three four");
        assertEquals(3, r.getTags().size());
        assertTrue(r.hasTag("one"));
        assertTrue(r.hasTag("two"));
        assertTrue(r.hasTag("three four"));
        assertFalse(r.hasTag("foo"));
        assertFalse(r.hasTag("bar"));
    }
}
