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
package org.apache.sling.muppet.sling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.muppet.api.MuppetFacade;
import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.RuleBuilder;
import org.apache.sling.muppet.api.RulesEngine;
import org.apache.sling.muppet.api.SystemAttribute;
import org.apache.sling.muppet.sling.impl.RulesResourceParserImpl;
import org.junit.Before;
import org.junit.Test;

public class RulesResourceParserTest {
    private final RulesResourceParserImpl parser = new RulesResourceParserImpl();
    private MockResolver resolver;
    private final List<RuleBuilder> builders = new ArrayList<RuleBuilder>();
    
    private final MuppetFacade facade = new MuppetFacade() {
        public RulesEngine getNewRulesEngine() { return null; }
        public List<Rule> parseSimpleTextRules(Reader textRules) throws IOException { return null; }
        public List<RuleBuilder> getRuleBuilders() { return builders; }
    };
    
    @Before
    public void setup() throws Exception {
        resolver = new MockResolver();
        
        builders.add(new RuleBuilder() {
            @Override
            public Rule buildRule(final String namespace, final String ruleName, final String qualifier, final String expression) {
                if("test".equals(namespace)) {
                    final SystemAttribute a = new SystemAttribute() {
                        @Override
                        public String toString() {
                            return namespace + ":" + ruleName + ":" + qualifier;
                        }
                        @Override
                        public Object getValue() {
                            return toString();
                        }
                    };
                    return new Rule(a, expression);
                }
                return null;
            }
        });
        
        final Field f = parser.getClass().getDeclaredField("muppet");
        f.setAccessible(true);
        f.set(parser, facade);
    }
    
    @Test
    public void testEmptyResource() {
        final Resource r = new MockResource(resolver, "/", null, null, null, null);
        assertEquals(0, parser.parseResource(r).size());
    }
    
    @Test
    public void testSingleResource() {
        final Resource r = new MockResource(resolver, "/", "test", "constant", "5", "> 3");
        final List<Rule> rules = parser.parseResource(r); 
        assertEquals(1, rules.size());
        assertEquals("Rule: test:constant:5 > 3", rules.get(0).toString());
    }
    
    @Test
    public void testResourceTree() {
        final Resource root = new MockResource(resolver, "/foo", "test", "constant", "5", "> 3");
        new MockResource(resolver, "/foo/1", "test", "constant", "12", "A");
        new MockResource(resolver, "/foo/2", "test", "constant", "12", "B");
        new MockResource(resolver, "/foo/3", "SHOULD_BE_IGNORED", "constant", "12", "A");
        new MockResource(resolver, "/foo/4", "null", "ignored as well", "12", "A");
        new MockResource(resolver, "/foo/some/path/to/me", "test", "deep", "43", "C");
        
        final List<Rule> rules = parser.parseResource(root); 
        assertEquals(4, rules.size());

        final String [] expect = {
            "Rule: test:constant:5 > 3",
            "Rule: test:constant:12 A",
            "Rule: test:constant:12 B",
            "Rule: test:deep:43 C"
        };
        final String allText = rules.toString();
        for(String resText : expect) {
            assertTrue("Expecting rules list (" + allText + ") to contain " + resText, allText.indexOf(resText) >= 0);
        }
    }
}
