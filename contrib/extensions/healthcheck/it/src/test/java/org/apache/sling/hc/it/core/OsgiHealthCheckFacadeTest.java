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
package org.apache.sling.hc.it.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.api.HealthCheckFacade;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.SystemAttribute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;

@RunWith(PaxExam.class)
public class OsgiHealthCheckFacadeTest {
    
    @Inject
    private HealthCheckFacade facade;
    
    @Inject
    private BundleContext bundleContext;
    
    @Configuration
    public Option[] config() {
        return U.config(false);
    }
    
    @Test
    public void testFacadePresent() {
        assertNotNull("Expecting HealthCheckFacade service to be provided", facade);
    }
    
    @Test
    public void testDefaultRules() throws IOException {
        // There should be at least one rule builder, but not a lot
        final String [] rules = { 
            "healthcheck:RuleBuilderCount:> 0",
            "healthcheck:RuleBuilderCount:> 42"
        };
        final List<EvaluationResult> r = U.evaluateRules(facade, rules);
        
        assertEquals(2, r.size());
        int i=0;
        U.assertResult(r.get(i++), true, "Rule: RuleBuilderCount > 0");
        U.assertResult(r.get(i++), false, "Rule: RuleBuilderCount > 42");
    }
    
    @Test
    public void testAddingCustomRule() throws IOException {
        final String [] rules = { 
            "healthcheck:RuleBuilderCount:> 0",
            "healthcheck:RuleBuilderCount:> 42",
            "test:constant:5",
            "test:constant:12",
        };
        
        final SystemAttribute five = new SystemAttribute() {
            @Override
            public String toString() {
                return "five";
            }
            @Override
            public Object getValue(Logger logger) {
                return 5;
            }
        };
        
        // To add new rule types, just register RuleBuilder services
        final RuleBuilder rb = new RuleBuilder() {
            @Override
            public Rule buildRule(String namespace, String ruleName, String qualifier, String expression) {
                if("test".equals(namespace) && "constant".equals(ruleName)) {
                    return new Rule(five, expression);
                }
                return null;
            }
        };
        
        final ServiceRegistration<?> reg = bundleContext.registerService(RuleBuilder.class.getName(), rb, null);

        try {
            final List<EvaluationResult> r = U.evaluateRules(facade, rules);
            assertEquals(4, r.size());
            int i=0;
            U.assertResult(r.get(i++), true, "Rule: RuleBuilderCount > 0");
            U.assertResult(r.get(i++), false, "Rule: RuleBuilderCount > 42");
            U.assertResult(r.get(i++), true, "Rule: five 5");
            U.assertResult(r.get(i++), false, "Rule: five 12");
        } finally {
            reg.unregister();
        }
        
        final List<EvaluationResult> r = U.evaluateRules(facade, rules);
        assertEquals("Expecting custom RuleBuilder to be gone", 2, r.size());
    }
}
