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
package org.apache.sling.discovery.impl.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.apache.commons.net.util.SubnetUtils;
import org.junit.Test;

public class WildcardHelperTest {

    @Test
    public void testNullValues() {
        
        SubnetUtils s = new SubnetUtils("1.2.3.4/10");
        s = new SubnetUtils("1.2.3.4", "255.255.0.0");
        
        try{
            WildcardHelper.wildcardAsRegex(null);
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
        try{
            WildcardHelper.matchesWildcard(null, "foo");
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
        try{
            WildcardHelper.matchesWildcard("foo", null);
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
    }
    
    @Test
    public void testEmptyValue() {
        final String expected = "\\Q\\E";
        assertEquals(expected, WildcardHelper.wildcardAsRegex(""));
    }
    
    @Test
    public void testWithoutWildcards() {
        for(int i=0; i<1000; i++) {
            String randomString = UUID.randomUUID().toString();
            assertTrue(WildcardHelper.matchesWildcard(randomString, randomString));
        }
    }
    
    @Test
    public void testWildcards() {
        assertTrue(WildcardHelper.matchesWildcard("", "*"));
        assertTrue(WildcardHelper.matchesWildcard("fooo", "*"));
        assertTrue(WildcardHelper.matchesWildcard("fooo", "**"));
        assertTrue(WildcardHelper.matchesWildcard("fooo", "**?"));
        assertTrue(WildcardHelper.matchesWildcard("fooo", "????"));
        assertFalse(WildcardHelper.matchesWildcard("fooo", "???"));
        assertFalse(WildcardHelper.matchesWildcard("fooo", "?????"));

        for(int i=0; i<100; i++) {
            String randomString = UUID.randomUUID().toString();
            assertTrue(WildcardHelper.matchesWildcard(randomString, "*"));
            assertTrue(WildcardHelper.matchesWildcard(randomString, randomString.substring(0, randomString.length()-1)+"?"));
            assertTrue(WildcardHelper.matchesWildcard(randomString, randomString.substring(0, randomString.length()-1)+"*"));
            assertTrue(WildcardHelper.matchesWildcard(randomString, randomString.substring(0, randomString.length()-2)+"??"));
            assertTrue(WildcardHelper.matchesWildcard(randomString, randomString.substring(0, randomString.length()-2)+"*"));
            assertTrue(WildcardHelper.matchesWildcard(randomString, "?" + randomString.substring(1, randomString.length())));
            assertTrue(WildcardHelper.matchesWildcard(randomString, "*" + randomString.substring(1, randomString.length())));
            assertTrue(WildcardHelper.matchesWildcard(randomString, "??" + randomString.substring(2, randomString.length())));
            assertTrue(WildcardHelper.matchesWildcard(randomString, "*" + randomString.substring(2, randomString.length())));
        }
        
        assertTrue(WildcardHelper.matchesWildcard("fooo", "f*"));
        assertTrue(WildcardHelper.matchesWildcard("fooo", "fo*"));
        assertTrue(WildcardHelper.matchesWildcard("fooo", "foo*"));
        assertTrue(WildcardHelper.matchesWildcard("fooo", "fooo*"));
        assertFalse(WildcardHelper.matchesWildcard("fooo", "fooo?"));
        assertFalse(WildcardHelper.matchesWildcard("fooo", "foooo*"));
        
        assertTrue(WildcardHelper.matchesWildcard("fooo", "*"));
        assertFalse(WildcardHelper.matchesWildcard("fooo", "?"));
        assertFalse(WildcardHelper.matchesWildcard("fooo", "f?"));

        assertTrue(WildcardHelper.matchesWildcard("fo", "f?"));

        assertTrue(WildcardHelper.matchesWildcard("foooba", "f*b?"));
        assertFalse(WildcardHelper.matchesWildcard("fooobar", "f*b?"));
        assertFalse(WildcardHelper.matchesWildcard("foooba", "f*b?r"));
        assertTrue(WildcardHelper.matchesWildcard("fooobar", "f*b?r"));
        assertTrue(WildcardHelper.matchesWildcard("foooba", "f???b?"));
        assertFalse(WildcardHelper.matchesWildcard("foooba", "f??b?"));
        assertFalse(WildcardHelper.matchesWildcard("foooba", "f??b?"));
    }
}
