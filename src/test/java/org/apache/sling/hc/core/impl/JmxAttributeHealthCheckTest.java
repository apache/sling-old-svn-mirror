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
package org.apache.sling.hc.core.impl;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.hc.api.Result;
import org.junit.Test;

public class JmxAttributeHealthCheckTest {

    static void assertJmxValue(String objectName, String attributeName, String constraint, boolean expected) {
        final JmxAttributeHealthCheck hc = new JmxAttributeHealthCheck();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(JmxAttributeHealthCheck.PROP_OBJECT_NAME, objectName);
        props.put(JmxAttributeHealthCheck.PROP_ATTRIBUTE_NAME, attributeName);
        props.put(JmxAttributeHealthCheck.PROP_CONSTRAINT, constraint);
        hc.activate(props);

        final Result r = hc.execute();
        assertEquals("Expected result " + expected, expected, r.isOk());
    }

    @Test
    public void testJmxAttributeMatch() {
        assertJmxValue("java.lang:type=ClassLoading", "LoadedClassCount", "> 10", true);
    }

    @Test
    public void testJmxAttributeNoMatch() {
        assertJmxValue("java.lang:type=ClassLoading", "LoadedClassCount", "< 10", false);
    }
}
