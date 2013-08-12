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
package org.apache.sling.hc.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.SimpleConstraintChecker;
import org.junit.Before;
import org.junit.Test;

public class SimpleConstraintCheckerTest {
    private Result result; 

    private final SimpleConstraintChecker checker = new SimpleConstraintChecker();

    @Before
    public void setup() {
        result = new Result();
    }
    
    @Test
    public void testStringEquals() {
        final String s = "test_" + System.currentTimeMillis();
        checker.check(s, s, result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testStringNotEquals() {
        final String s = "test_" + System.currentTimeMillis();
        checker.check(s, "something else", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testFiveEquals() {
        final String s = "5";
        checker.check(s, s, result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testIntTwelveEquals() {
        checker.check(12, "12", result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testIntTwelveGreaterThan() {
        checker.check(12, "> 11", result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testFiveNotEquals() {
        checker.check("5", "foo", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testNullNotEquals() {
        checker.check(null, "foo", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testNullNotGreater() {
        checker.check(null, "> 2", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testGreaterThanTrue() {
        checker.check("5", "> 2", result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testGreaterThanFalse() {
        checker.check("5", "> 12", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testLessThanTrue() {
        checker.check("5", "< 12", result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testLessThanFalse() {
        checker.check("5", "< 2", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testBetweenA() {
        checker.check("5", "between 2 and 6", result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testBetweenB() {
        checker.check("5", "between 12 and 16", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testBetweenC() {
        checker.check(5L, "between 12 and 16", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testBetweenD() {
        checker.check(5L, "between 4 and 16", result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testBetweenE() {
        checker.check(5L, "betWEEN 4 aND 16", result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testNotAnInteger() {
        checker.check("foo", "between 12 and 16", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testContainsA() {
        checker.check("This is a NICE STRING ok?", "contains NICE STRING", result);
        assertTrue(result.isOk());
    }
    
    @Test
    public void testContainsB() {
        checker.check("This is a NICE TOUCH ok?", "contains NICE STRING", result);
        assertFalse(result.isOk());
    }
    
    @Test
    public void testContainsC() {
        checker.check("This is a NICE TOUCH ok?", "contains NICE", result);
        assertTrue(result.isOk());
    }
}
