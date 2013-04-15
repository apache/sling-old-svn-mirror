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
package org.apache.sling.muppet.impl;

import org.apache.sling.muppet.api.Evaluator;
import org.apache.sling.muppet.api.SystemAttribute;
import org.apache.sling.muppet.util.DefaultEvaluator;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.apache.sling.muppet.api.EvaluationResult.Status.OK;
import static org.apache.sling.muppet.api.EvaluationResult.Status.ERROR;

public class DefaultEvaluatorTest {
    private final Evaluator evaluator = new DefaultEvaluator();
    
    private final SystemAttribute five = new SystemAttribute() {
        @Override
        public Object getValue() {
            return "5";
        }
    };
    
    @Test
    public void testEquals() {
        assertEquals(OK, evaluator.evaluate(five, "5"));
        assertEquals(ERROR, evaluator.evaluate(five, "foo"));
    }
    
    @Test
    public void testGreaterThan() {
        assertEquals(OK, evaluator.evaluate(five, "> 2"));
        assertEquals(ERROR, evaluator.evaluate(five, "> 12"));
    }
    
    @Test
    public void testLessThan() {
        assertEquals(OK, evaluator.evaluate(five, "< 12"));
        assertEquals(ERROR, evaluator.evaluate(five, "< 2"));
    }
    
    @Test
    public void testBetween() {
        assertEquals(OK, evaluator.evaluate(five, "between 2 and 6"));
        assertEquals(ERROR, evaluator.evaluate(five, "between 2 and 5"));
        assertEquals(ERROR, evaluator.evaluate(five, "between 5 and 7"));
        assertEquals(OK, evaluator.evaluate(five, "between 4 and 7"));
        assertEquals(OK, evaluator.evaluate(five, "BETWEEN 2 AND 6"));
        assertEquals(ERROR, evaluator.evaluate(five, "between 12 and 61"));
        assertEquals(ERROR, evaluator.evaluate(five, "BETWEEN 12 AND 61"));
    }
}
