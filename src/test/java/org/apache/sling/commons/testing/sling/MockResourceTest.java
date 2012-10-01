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
package org.apache.sling.commons.testing.sling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Test;

public class MockResourceTest {
    public static final String PATH = "/foo/bar";
    public static final String RT = "someResourceType";
    
    @Test 
    public void basicTest() throws Exception {
        final MockResource r = new MockResource(null, PATH, RT);
        r.addProperty("1", Integer.MAX_VALUE);
        r.addProperty("2", "two");
        
        assertTrue(r instanceof Resource);
        assertEquals(PATH, r.getPath());
        assertEquals(RT, r.getResourceType());
        assertNull(r.getResourceSuperType());
        
        final ValueMap m = r.adaptTo(ValueMap.class);
        assertEquals(Integer.MAX_VALUE, m.get("1"));
        assertEquals("two", m.get("2"));
        
        // changes to r do not affect m
        assertNull(m.get("third"));
        r.getProperties().put("third", "trois");
        assertNull(m.get("third"));
        
        // but we get the new value after adapting again
        assertEquals("trois", r.adaptTo(ValueMap.class).get("third"));
    }
    
    @Test
    public void testNoProperties() throws Exception {
        final MockResource r = new MockResource(null, PATH, RT);
        assertNotNull(r.adaptTo(ValueMap.class));
    }
}