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
package org.apache.sling.hc.rules.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.rules.jmx.JmxBeansRuleBuilder;
import org.junit.Test;

public class JmxBeansRuleBuilderTest {
    private final RuleBuilder jmxRuleBuilder = new JmxBeansRuleBuilder();
    
    @Test
    public void testBasicJvmBean() {
        // Assuming this attribute is present in all JVMs that we use to run tests...
        final Rule r = jmxRuleBuilder.buildRule("jmxbeans", "java.lang:type=ClassLoading", "LoadedClassCount", "> 100");
        assertNotNull("Expecting to get a jmxbean Rule", r);
        assertFalse(r.evaluate().anythingToReport());
    }
    
    @Test
    public void testHashSeparatorInBeanName() {
        final Rule r = jmxRuleBuilder.buildRule("jmxbeans", "java.lang#type=ClassLoading", "LoadedClassCount", "> 100");
        assertNotNull("Expecting to get a jmxbean Rule", r);
        assertFalse(r.evaluate().anythingToReport());
    }
    
    @Test
    public void testNonExistentBean() {
        final Rule r = jmxRuleBuilder.buildRule("jmxbeans", "java.lang:type=DoesNotExist", "LoadedClassCount", "5");
        assertNotNull("Expecting to get a jmxbean Rule", r);
        assertTrue(r.evaluate().anythingToReport());
    }
}
