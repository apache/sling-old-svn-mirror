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
package org.apache.sling.hc.sling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.hc.api.HealthCheckFacade;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.RulesEngine;
import org.apache.sling.hc.api.SystemAttribute;
import org.apache.sling.hc.sling.impl.RulesResourceParserImpl;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class RulesResourceParserTest {
    private final RulesResourceParserImpl parser = new RulesResourceParserImpl();
    private MockResolver resolver;
    private final List<RuleBuilder> builders = new ArrayList<RuleBuilder>();
    
    private final HealthCheckFacade facade = new HealthCheckFacade() {
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
                        public Object getValue(Logger logger) {
                            return toString();
                        }
                    };
                    return new Rule(a, expression);
                }
                return null;
            }
        });
        
        final Field f = parser.getClass().getDeclaredField("healthcheck");
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
    public void testResourceWithTags() {
        final MockResource r = new MockResource(resolver, "/foo", "test", "constant", "5", "> 3");
        r.setTags(new String [] { "foo", "bar" });
        final List<Rule> rules = parser.parseResource(r); 
        assertEquals(1, rules.size());
        final Rule rule = rules.get(0);
        assertEquals("Rule: test:constant:5 > 3", rule.toString());
        assertEquals(2, rule.getTags().size());
        assertTrue(rule.hasTag("foo"));
        assertTrue(rule.hasTag("bar"));
        assertNotNull(rule.getInfo());
        assertEquals("/foo", rule.getInfo().get("sling.resource.path"));
    }
    
    @Test
    public void testScriptResource() {
        final Resource root = new MockResource(resolver, "/foo", "test", "constant", "5", "> 3");
        new MockResource(resolver, "/foo/script1", "some script");
        final List<Rule> rules = parser.parseResource(root); 
        assertEquals(2, rules.size());
        final String [] expect = {
                "Rule: test:constant:5 > 3",
                "Rule: /foo/script1 TEST_PASSED"
            };
            final String allText = rules.toString();
            for(String resText : expect) {
                assertTrue("Expecting rules list (" + allText + ") to contain " + resText, allText.indexOf(resText) >= 0);
            }
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
    
    @Test(expected=SlingException.class)
    public void testUnauthorized() throws RepositoryException {
        resolver.setUnauthorized();
        final Resource root = new MockResource(resolver, "/foo", "test", "constant", "5", "> 3");
        parser.parseResource(root); 
    }
}
