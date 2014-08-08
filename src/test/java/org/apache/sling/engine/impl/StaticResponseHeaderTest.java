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
package org.apache.sling.engine.impl;

import junit.framework.TestCase;
import org.junit.Test;

public class StaticResponseHeaderTest {

    @Test
    public void test_constructor_null() {
        try {
            new StaticResponseHeader(null);
            TestCase.fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test
    public void test_constructor_empty() {
        try {
            new StaticResponseHeader("");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_constructor_missing_responseHeaderValue() {
        try {
            new StaticResponseHeader("responseHeader");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new StaticResponseHeader("responseHeaderName=");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_constructor_missing_responseHeaderName() {
        try {
            new StaticResponseHeader("=user");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_constructor_and_map() {
        assertMapping("responseHeaderName", "responseHeaderValue");
    }

    private void assertMapping(final String responseHeaderName, final String responseHeaderValue) {
        StringBuilder spec = new StringBuilder();
        spec.append(responseHeaderName);
        spec.append('=').append(responseHeaderValue);

        // spec analysis
        final StaticResponseHeader mapping = new StaticResponseHeader(spec.toString());
        TestCase.assertEquals(responseHeaderName, mapping.getResponseHeaderName());
        TestCase.assertEquals(responseHeaderValue, mapping.getResponseHeaderValue());
    }
}
