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
package org.apache.sling.serviceusermapping.impl;

import java.lang.reflect.Field;

import junit.framework.TestCase;

import org.apache.sling.serviceusermapping.impl.Mapping;
import org.junit.Test;

public class MappingTest {

    @Test
    public void test_constructor_null() {
        try {
            new Mapping(null);
            TestCase.fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test
    public void test_constructor_empty() {
        try {
            new Mapping("");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_constructor_missing_user_name() {
        try {
            new Mapping("serviceName");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new Mapping("serviceName=");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_constructor_missing_service_name() {
        try {
            new Mapping("=user");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_constructor_and_map() {
        assertMapping("service", null, "user");
        assertMapping("service", "serviceInfo", "user");
        assertMapping("service", "", "user");
    }

    private void assertMapping(final String serviceName, final String serviceInfo, final String userName) {
        StringBuilder spec = new StringBuilder();
        spec.append(serviceName);
        if (serviceInfo != null) {
            spec.append(':').append(serviceInfo);
        }
        spec.append('=').append(userName);

        // spec analysis
        final Mapping mapping = new Mapping(spec.toString());
        TestCase.assertEquals(getField(mapping, "serviceName"), serviceName);
        TestCase.assertEquals(getField(mapping, "serviceInfo"), serviceInfo);
        TestCase.assertEquals(getField(mapping, "userName"), userName);

        // mapping
        TestCase.assertEquals(userName, mapping.map(serviceName, serviceInfo));
        if (serviceInfo == null) {
            // Mapping without serviceInfo must not match request with any
            // serviceInfo
            TestCase.assertNull(mapping.map(serviceName, serviceInfo + "-garbage"));
        } else {
            // Mapping with serviceInfo must not match request without
            // serviceInfo
            TestCase.assertNull(mapping.map(serviceName, null));
        }

        // no match for different service name
        TestCase.assertNull(mapping.map(serviceName + "-garbage", serviceInfo));

        // no match for null service name
        TestCase.assertNull(mapping.map(null, serviceInfo));
    }

    private String getField(final Object object, final String fieldName) {
        try {
            Field f = object.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (String) f.get(object);
        } catch (Exception e) {
            TestCase.fail("Cannot get field " + fieldName + ": " + e.toString());
            return null; // will not get here, quiesce compiler
        }
    }
}
