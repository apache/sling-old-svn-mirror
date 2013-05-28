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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.RuleFilter;
import org.apache.sling.hc.api.SystemAttribute;
import org.apache.sling.hc.util.DefaultEvaluator;
import org.junit.Test;
import org.slf4j.Logger;

public class RulesEngineTest {
    
    static class TestBuilder implements RuleBuilder {
        @Override
        public Rule buildRule(final String namespace, final String ruleName,final String qualifier, final String expression) {
            if(!"test".equals(namespace)) {
                return null;
            }
            
            SystemAttribute attr = new SystemAttribute() {
                @Override
                public Object getValue(Logger logger) {
                    if("constant".equals(ruleName)) {
                        return Integer.valueOf(qualifier);
                    } else if("invert".equals(ruleName)) {
                            return Integer.valueOf("-" + qualifier);
                    } else {
                        return null;
                    }
                }
                
            };
            
            return new Rule(attr, expression, new DefaultEvaluator()) {
                @Override
                public String toString() {
                    return "" + namespace + "_" + ruleName + "_" + qualifier + "_" + expression;
                }
            };
        }
    }
    
    private void assertResult(EvaluationResult rr, boolean expectOk, String ruleString) {
        assertEquals("Rule " + rr.getRule() + " result matches", expectOk, !rr.anythingToReport());
        assertEquals("Rule " + rr.getRule() + " string matches", rr.getRule().toString(), ruleString);
    }
    
    @Test
    public void parseAndExecute() throws IOException {
        final String rules =
            "test:constant:5:5\n"
            + "test:constant:5: > 2\n"
            + "test:constant:5: < 12\n"
            + "test:constant:5: between 4 and 6\n"
            + "test:constant:5: between 12 and 21\n"
            + "test:constant:5:42\n"
            + "test:invert:12:-1\n"
            + "test:invert:12:-12\n"
        ;
        
        final TextRulesParser p = new TextRulesParser();
        p.addBuilder(new TestBuilder());
        
        final RulesEngineImpl e = new RulesEngineImpl();
        e.addRules(p.parse(new StringReader(rules)));
        
        final List<EvaluationResult> result = e.evaluateRules();
        assertEquals(8, result.size());
        
        int i=0;
        assertResult(result.get(i++), true, "test_constant_5_5");
        assertResult(result.get(i++), true, "test_constant_5_> 2");
        assertResult(result.get(i++), true, "test_constant_5_< 12");
        assertResult(result.get(i++), true, "test_constant_5_between 4 and 6");
        assertResult(result.get(i++), false, "test_constant_5_between 12 and 21");
        assertResult(result.get(i++), false, "test_constant_5_42");
        assertResult(result.get(i++), false, "test_invert_12_-1");
        assertResult(result.get(i++), true, "test_invert_12_-12");
    }
    
    @Test
    public void testRuleFilter() throws IOException {
        final String rules =
            "test:constant:5:5\n"
            + "test:constant:5: > 2\n"
            + "test:constant:5: < 12\n"
            + "test:constant:5: between 4 and 6\n"
            + "test:constant:5: between 12 and 21\n"
            + "test:constant:5:42\n"
            + "test:invert:12:-1\n"
            + "test:invert:12:-12\n"
        ;
        
        final TextRulesParser p = new TextRulesParser();
        p.addBuilder(new TestBuilder());
        
        final RulesEngineImpl e = new RulesEngineImpl();
        e.addRules(p.parse(new StringReader(rules)));
        
        final RuleFilter f = new RuleFilter() {
            public boolean accept(Rule r) {
                return r.toString().contains("12");
            }
        };
        final List<EvaluationResult> result = e.evaluateRules(f);
        assertEquals("Expecting only 4 rules according to RuleFilter", 4, result.size());
    }
}
