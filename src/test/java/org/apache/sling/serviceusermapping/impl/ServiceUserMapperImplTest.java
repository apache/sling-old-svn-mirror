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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.sling.commons.testing.osgi.MockBundle;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class ServiceUserMapperImplTest {
    private static final String BUNDLE_SYMBOLIC1 = "bundle1";

    private static final String BUNDLE_SYMBOLIC2 = "bundle2";

    private static final String SUB = "sub";

    private static final String NONE = "none";

    private static final String SAMPLE = "sample";

    private static final String ANOTHER = "another";

    private static final String SAMPLE_SUB = "sample_sub";

    private static final String ANOTHER_SUB = "another_sub";

    private static final Bundle BUNDLE1 = new MockBundle(10) {
        @Override
        public String getSymbolicName() {
            return BUNDLE_SYMBOLIC1;
        };

        @Override
        public java.util.Dictionary<?, ?> getHeaders() {
            return new Hashtable<String, Object>();
        };

        @Override
        public java.util.Dictionary<?, ?> getHeaders(String locale) {
            return getHeaders();
        };
    };

    private static final Bundle BUNDLE2 = new MockBundle(10) {
        @Override
        public String getSymbolicName() {
            return BUNDLE_SYMBOLIC2;
        };

        @Override
        public java.util.Dictionary<?, ?> getHeaders(String locale) {
            return getHeaders();
        };
    };

    @Test
    public void test_getServiceUserID() {
        @SuppressWarnings("serial")
        Map<String, Object> config = new HashMap<String, Object>() {
            {
                put("user.mapping", new String[] {
                    BUNDLE_SYMBOLIC1 + "=" + SAMPLE, //
                    BUNDLE_SYMBOLIC2 + "=" + ANOTHER, //
                    BUNDLE_SYMBOLIC1 + ":" + SUB + "=" + SAMPLE_SUB, //
                    BUNDLE_SYMBOLIC2 + ":" + SUB + "=" + ANOTHER_SUB //
                });
                put("user.default", NONE);
            }
        };

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(config);

        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, null));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, null));
        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, ""));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, ""));
        TestCase.assertEquals(SAMPLE_SUB, sum.getServiceUserID(BUNDLE1, SUB));
        TestCase.assertEquals(ANOTHER_SUB, sum.getServiceUserID(BUNDLE2, SUB));
    }
}
