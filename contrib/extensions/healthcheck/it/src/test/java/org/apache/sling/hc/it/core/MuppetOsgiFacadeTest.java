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
package org.apache.sling.muppet.it.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.muppet.api.EvaluationResult;
import org.apache.sling.muppet.api.MuppetFacade;
import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.RuleBuilder;
import org.apache.sling.muppet.api.SystemAttribute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
public class MuppetOsgiFacadeTest {
    
    @Inject
    private MuppetFacade facade;
    
    @Inject
    private BundleContext bundleContext;
    
    @Configuration
    public Option[] config() {
        return U.config(false);
    }
    
    @Test
    public void testFacadePresent() {
        assertNotNull("Expecting MuppetFacade service to be provided", facade);
    }
    
    @Test
    public void testDefaultRules() throws IOException {
        // There should be at least one rule builder, but not a lot
        final String [] rules = { 
            "muppet:RuleBuilderCount:> 0",
            "muppet:RuleBuilderCount:> 42"
        };
        final List<EvaluationResult> r = U.evaluateRules(facade, rules);
        
        assertEquals(2, r.size());
        int i=0;
        U.assertResult(r.get(i++), EvaluationResult.Status.OK, "Rule: RuleBuilderCount > 0");
        U.assertResult(r.get(i++), EvaluationResult.Status.ERROR, "Rule: RuleBuilderCount > 42");
    }
    
    @Test
    public void testAddingCustomRule() throws IOException {
        final String [] rules = { 
            "muppet:RuleBuilderCount:> 0",
            "muppet:RuleBuilderCount:> 42",
            "test:constant:5",
            "test:constant:12",
        };
        
        final SystemAttribute five = new SystemAttribute() {
            @Override
            public String toString() {
                return "five";
            }
            @Override
            public Object getValue() {
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
            U.assertResult(r.get(i++), EvaluationResult.Status.OK, "Rule: RuleBuilderCount > 0");
            U.assertResult(r.get(i++), EvaluationResult.Status.ERROR, "Rule: RuleBuilderCount > 42");
            U.assertResult(r.get(i++), EvaluationResult.Status.OK, "Rule: five 5");
            U.assertResult(r.get(i++), EvaluationResult.Status.ERROR, "Rule: five 12");
        } finally {
            reg.unregister();
        }
        
        final List<EvaluationResult> r = U.evaluateRules(facade, rules);
        assertEquals("Expecting custom RuleBuilder to be gone", 2, r.size());
    }
}
