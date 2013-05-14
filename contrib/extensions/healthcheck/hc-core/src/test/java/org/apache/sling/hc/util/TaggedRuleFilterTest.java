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
package org.apache.sling.hc.util;

import org.apache.sling.hc.api.Rule;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class TaggedRuleFilterTest {
    private Rule notags = new Rule(null, null);
    private Rule foobar = new Rule(null, null).setTags("foo", "bar");
    private Rule foo = new Rule(null, null).setTags("foo");

    @Test
    public void testNoTagsFilter() {
        final TaggedRuleFilter ntf = new TaggedRuleFilter();
        assertTrue(ntf.accept(notags));
        assertTrue(ntf.accept(foobar));
        assertTrue(ntf.accept(foo));
    }
    
    @Test
    public void testFooTagsFilter() {
        final TaggedRuleFilter ftf = new TaggedRuleFilter("foo");
        assertFalse(ftf.accept(notags));
        assertTrue(ftf.accept(foobar));
        assertTrue(ftf.accept(foo));
    }
    
    @Test
    public void testFoobarTagsFilter() {
        final TaggedRuleFilter f = new TaggedRuleFilter("foo", "bar");
        assertFalse(f.accept(notags));
        assertTrue(f.accept(foobar));
        assertFalse(f.accept(foo));
    }
    
    @Test
    public void testFoobarLowercaseTagsFilter() {
        final TaggedRuleFilter f = new TaggedRuleFilter("Foo", "BAR");
        assertFalse(f.accept(notags));
        assertTrue(f.accept(foobar));
        assertFalse(f.accept(foo));
    }
    
}
