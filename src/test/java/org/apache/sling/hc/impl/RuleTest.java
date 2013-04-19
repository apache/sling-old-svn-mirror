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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.SystemAttribute;
import org.junit.Test;
import org.slf4j.Logger;

public class RuleTest {
    
    final SystemAttribute five = new SystemAttribute() {
        @Override
        public String toString() {
            return "5";
        }
        
        @Override
        public Object getValue(Logger logger) {
            return 5;
        }
    };
    
    final SystemAttribute logErrorAndNull = new SystemAttribute() {
        @Override
        public Object getValue(Logger logger) {
            logger.warn("Something went wrong");
            return null;
        }
    };
    
    final SystemAttribute nullAttr = new SystemAttribute() {
        @Override
        public Object getValue(Logger logger) {
            return null;
        }
    };
    
    @Test
    public void testToStringWithExpr() {
        assertEquals("Rule: 5 hello", new Rule(five, "hello").toString());
    }
    
    @Test
    public void testToStringNoExpr() {
        assertEquals("Rule: 5", new Rule(five, null).toString());
    }
    
    @Test
    public void testEqualsFive() {
        assertFalse(new Rule(five,"5").evaluate().anythingToReport());
    }
    
    @Test
    public void testGreaterThanTwo() {
        assertFalse(new Rule(five,"> 2").evaluate().anythingToReport());
    }
    
    @Test
    public void testGreaterThanTwelve() {
        assertTrue(new Rule(five,"> 12").evaluate().anythingToReport());
    }
    
    @Test
    public void testLogAndNullNoExpr() {
        assertTrue(new Rule(logErrorAndNull,null).evaluate().anythingToReport());
    }
    
    @Test
    public void testLogAndNullEmptyExpr() {
        assertTrue(new Rule(logErrorAndNull,"\t\n").evaluate().anythingToReport());
    }
    
    @Test
    public void testNullNoLog() {
        assertFalse(new Rule(nullAttr,null).evaluate().anythingToReport());
    }
    
    @Test
    public void testNullEmptyExpr() {
        assertFalse(new Rule(nullAttr,"").evaluate().anythingToReport());
    }
}
