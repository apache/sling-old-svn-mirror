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

import static org.junit.Assert.assertTrue;

import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.api.Evaluator;
import org.apache.sling.hc.api.SystemAttribute;
import org.apache.sling.hc.util.DefaultEvaluator;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEvaluatorTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private RuleLoggerImpl wrapper; 

    private final Evaluator evaluator = new DefaultEvaluator();
    
    static class ConstantAttribute implements SystemAttribute {
        
        private final Object value;
        
        ConstantAttribute(Object value) {
            this.value = value;
        }
        
        @Override
        public Object getValue(Logger logger) {
            return value;
        }
    };
    
    private final SystemAttribute five = new ConstantAttribute("5");
    private final SystemAttribute active = new ConstantAttribute("active");
    private final SystemAttribute intTwelve = new ConstantAttribute(12);
    private final SystemAttribute nullAttr = new ConstantAttribute(null);
    
    private void assertWrapperResult(RuleLoggerImpl wrapper, boolean expectOk) {
        if(!expectOk) {
            assertTrue("Expecting max log level >= INFO, got " + wrapper.getMaxLevel(), 
                    wrapper.getMaxLevel().ordinal() >= EvaluationResult.LogLevel.INFO.ordinal());
        } else {
            assertTrue("Expecting max log level < INFO, got " + wrapper.getMaxLevel(), 
                    wrapper.getMaxLevel().ordinal() < EvaluationResult.LogLevel.INFO.ordinal());
        }
    }
    
    @Before
    public void setup() {
        wrapper = new RuleLoggerImpl(logger);
    }
    
    @Test
    public void testActiveEquals() {
        evaluator.evaluate(active, "active", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testActiveNotEquals() {
        evaluator.evaluate(active, "foo", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testFiveEquals() {
        evaluator.evaluate(five, "5", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testIntTwelveEquals() {
        evaluator.evaluate(intTwelve, "12", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testIntTwelveGreaterThan() {
        evaluator.evaluate(intTwelve, "> 11", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testFiveNotEquals() {
        evaluator.evaluate(five, "foo", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testNullNotEquals() {
        evaluator.evaluate(nullAttr, "foo", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testNullNotGreater() {
        evaluator.evaluate(nullAttr, "> 2", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testGreaterThanTrue() {
        evaluator.evaluate(five, "> 2", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testGreaterThanFalse() {
        evaluator.evaluate(five, "> 12", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testLessThanTrue() {
        evaluator.evaluate(five, "< 12", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testLessThanFalse() {
        evaluator.evaluate(five, "< 2", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testBetweenA() {
        evaluator.evaluate(five, "between 2 and 6", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testBetweenB() {
        evaluator.evaluate(five, "between 2 and 5", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testBetweenC() {
        evaluator.evaluate(five, "between 5 and 7", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testBetweenD() {
        evaluator.evaluate(five, "between 4 and 7", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testBetweenE() {
        evaluator.evaluate(five, "BETWEEN 2 AND 6", wrapper);
        assertWrapperResult(wrapper, true);
    }
    
    @Test
    public void testBetweenF() {
        evaluator.evaluate(five, "between 12 and 61", wrapper);
        assertWrapperResult(wrapper, false);
    }
    
    @Test
    public void testBetweenG() {
        evaluator.evaluate(five, "BETWEEN 12 AND 61", wrapper);
        assertWrapperResult(wrapper, false);
    }
}
