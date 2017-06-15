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
import java.util.Arrays;
import java.util.Set;

import junit.framework.TestCase;
import org.junit.Test;

public class MappingTest {

    @Test(expected = NullPointerException.class)
    public void test_constructor_null() {
        new Mapping(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_empty() {
        new Mapping("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_missing_user_name() {
        new Mapping("serviceName");

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_missing_user_name2() {
        new Mapping("serviceName=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_missing_service_name() {
        new Mapping("=user");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_empty_service_info() {
        new Mapping("srv:=user");
    }

    @Test
    public void test_constructor_user_with_colon() {
        new Mapping("srv=jcr:user");
    }

    @Test
    public void test_constructor_and_map() {
        assertMapping("service", null, "user", (String[]) null);
        assertMapping("service", "subServiceName", "user", (String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_and_null_principals() {
        assertMapping("service", "subServiceName", null, (String[]) null);
    }

    @Test
    public void test_constructor_and_map_empty_principals() {
        assertMapping("service", "subServiceName", null);
    }

    @Test
    public void test_constructor_and_map_with_empty_principal() {
        assertMapping("service", "subServiceName", null, "principal", "", "principal1");
    }

    @Test
    public void test_constructor_and_map_with_null_principal() {
        assertMapping("service", "subServiceName", null, "principal", null, "principal1");
    }

    @Test
    public void test_constructor_and_map_single_principal() {
        assertMapping("service", "subServiceName", null, "principal");
    }

    @Test
    public void test_constructor_and_map_duplicate_principals() {
        assertMapping("service", "subServiceName", null, "principal", "principal");
    }

    @Test
    public void test_constructor_and_map_principals() {
        assertMapping("service", "subServiceName", null, "principal1", "principal2", "principal3");
    }

    @Test
    public void test_constructor_and_map_user_and_principals() {
        assertMapping("service", "subServiceName", "user", "principal1", "principal2", "principal3");
    }

    private void assertMapping(final String serviceName, final String subServiceName, final String userName, final String... principalNames) {
        StringBuilder spec = new StringBuilder();
        spec.append(serviceName);
        if (subServiceName != null) {
            spec.append(':').append(subServiceName);
        }
        spec.append('=');

        String expectedUserName = null;
        Set<String> expectedPrincipalsNames = null;
        if (principalNames != null) {
            spec.append(Arrays.toString(principalNames));
            expectedPrincipalsNames = Mapping.extractPrincipalNames(Arrays.toString(principalNames));
        } else if (userName != null) {
            spec.append(userName);
            expectedUserName = userName;
        }

        // spec analysis
        final Mapping mapping = new Mapping(spec.toString());
        TestCase.assertEquals(serviceName, getField(mapping, "serviceName"));
        TestCase.assertEquals(subServiceName, getField(mapping, "subServiceName"));
        if (expectedUserName == null) {
            TestCase.assertNull(getField(mapping, "userName"));
        } else {
            TestCase.assertEquals(expectedUserName, getField(mapping, "userName"));
        }
        if (principalNames == null) {
            TestCase.assertNull(getSetField(mapping, "principalNames"));
        } else {
            TestCase.assertEquals(expectedPrincipalsNames, getSetField(mapping, "principalNames"));
        }

        // mapping
        if (expectedUserName == null) {
            TestCase.assertNull(mapping.map(serviceName, subServiceName));
        } else {
            TestCase.assertEquals(userName, mapping.map(serviceName, subServiceName));
        }

        if (expectedPrincipalsNames == null) {
            TestCase.assertNull(mapping.mapPrincipals(serviceName, subServiceName));
        } else {
            TestCase.assertEquals(expectedPrincipalsNames, mapping.mapPrincipals(serviceName, subServiceName));
        }

        if (subServiceName == null) {
            // Mapping without subServiceName must not match request with any
            // subServiceName
            TestCase.assertNull(mapping.map(serviceName, subServiceName + "-garbage"));
            TestCase.assertNull(mapping.mapPrincipals(serviceName, subServiceName + "-garbage"));
        } else {
            // Mapping with subServiceName must not match request without
            // subServiceName
            TestCase.assertNull(mapping.map(serviceName, null));
            TestCase.assertNull(mapping.mapPrincipals(serviceName, null));
        }

        // no match for different service name
        TestCase.assertNull(mapping.map(serviceName + "-garbage", subServiceName));
        TestCase.assertNull(mapping.mapPrincipals(serviceName + "-garbage", subServiceName));

        // no match for null service name
        TestCase.assertNull(mapping.map(null, subServiceName));
        TestCase.assertNull(mapping.mapPrincipals(null, subServiceName));
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

    private Set<String> getSetField(final Object object, final String fieldName) {
        try {
            Field f = object.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (Set<String>) f.get(object);
        } catch (Exception e) {
            TestCase.fail("Cannot get field " + fieldName + ": " + e.toString());
            return null; // will not get here, quiesce compiler
        }
    }
}
