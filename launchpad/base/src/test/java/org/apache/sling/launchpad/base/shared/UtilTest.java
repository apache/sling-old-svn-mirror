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
package org.apache.sling.launchpad.base.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.TestCase;

public class UtilTest {

    @SuppressWarnings("serial")
    static Map<String, String> properties = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put("foo", "_v_foo_");
            put("bar", "_v_bar_");
            put("baz", "_v_baz_");
            put("foo._v_bar_", "_v_foo.bar_");
        }
    });

    @Test
    public void test_substVars_no_replacement() {
        TestCase.assertEquals("foo", Util.substVars("foo", "the_foo", null, properties));
        TestCase.assertEquals("%d{yyyy-MM-dd} %t{short} %m", Util.substVars("%d{yyyy-MM-dd} %t{short} %m", "the_foo", null, properties));
    }

    @Test
    public void test_substVars_single_replacement() {
        TestCase.assertEquals("_v_foo_", Util.substVars("${foo}", "the_foo", null, properties));
        TestCase.assertEquals("leading _v_foo_ trailing", Util.substVars("leading ${foo} trailing", "the_foo", null, properties));
        TestCase.assertEquals("leading _v_foo_ middle _v_baz_ trailing",
            Util.substVars("leading ${foo} middle ${baz} trailing", "the_foo", null, properties));
    }

    @Test
    public void test_substVars_nested_replacement() {
        TestCase.assertEquals("leading _v_foo.bar_ middle _v_baz_ trailing",
            Util.substVars("leading ${foo.${bar}} middle ${baz} trailing", "the_foo", null, properties));
    }

    @Test
    public void test_substVars_missing_replacement() {
        System.setProperty("foobar", "_v_foobar_");
        System.clearProperty("foobaz");
        TestCase.assertEquals("leading _v_foobar_ middle  trailing",
            Util.substVars("leading ${foobar} middle ${foobaz} trailing", "the_foo", null, properties));
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_substVars_recursive_failure() {
        Util.substVars("leading ${foo} middle ${baz} trailing", "foo", null, properties);
    }
}