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
package org.apache.sling.hc.impl;

import org.apache.sling.hc.impl.healthchecks.JmxScriptBinding;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxScriptBindingTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Test
    public void testJmxAttribute() throws Exception {
        final JmxScriptBinding b = new JmxScriptBinding(logger);
        final Object result = b.attribute("java.lang:type=ClassLoading", "LoadedClassCount");
        assertNotNull("Expecting non-null attribute value", result);
        assertTrue("Expecting non-empty value", result.toString().length() > 0);
    }
}