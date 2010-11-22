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
package org.apache.sling.event.impl.jobs;

import junit.framework.TestCase;

public class UtilityTest extends TestCase {

    public void test_filter_allowed() {
        final String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz0123456789_,.-+#!?$%&()=";
        assertEquals("Allowed Characters must not be filtered", allowed,
            Utility.filter(allowed));
    }

    public void test_filter_illegal_jcr() {
        assertEquals("_", Utility.filter("["));
        assertEquals("_", Utility.filter("]"));
        assertEquals("_", Utility.filter("*"));
        assertEquals("_", Utility.filter("/"));
        assertEquals("_", Utility.filter(":"));
        assertEquals("_", Utility.filter("'"));
        assertEquals("_", Utility.filter("\""));

        assertEquals("a_b", Utility.filter("a[b"));
        assertEquals("a_b", Utility.filter("a]b"));
        assertEquals("a_b", Utility.filter("a*b"));
        assertEquals("a_b", Utility.filter("a/b"));
        assertEquals("a_b", Utility.filter("a:b"));
        assertEquals("a_b", Utility.filter("a'b"));
        assertEquals("a_b", Utility.filter("a\"b"));

        assertEquals("_b", Utility.filter("[b"));
        assertEquals("_b", Utility.filter("]b"));
        assertEquals("_b", Utility.filter("*b"));
        assertEquals("_b", Utility.filter("/b"));
        assertEquals("_b", Utility.filter(":b"));
        assertEquals("_b", Utility.filter("'b"));
        assertEquals("_b", Utility.filter("\"b"));

        assertEquals("a_", Utility.filter("a["));
        assertEquals("a_", Utility.filter("a]"));
        assertEquals("a_", Utility.filter("a*"));
        assertEquals("a_", Utility.filter("a/"));
        assertEquals("a_", Utility.filter("a:"));
        assertEquals("a_", Utility.filter("a'"));
        assertEquals("a_", Utility.filter("a\""));
    }

    public void test_filter_consecutive_replace() {
        assertEquals("a_b_", Utility.filter("a/[b]"));
    }
}
