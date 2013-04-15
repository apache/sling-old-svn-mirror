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

import static org.apache.sling.muppet.api.EvaluationResult.Status.ERROR;
import static org.apache.sling.muppet.api.EvaluationResult.Status.OK;
import static org.junit.Assert.assertEquals;

import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.SystemAttribute;
import org.junit.Test;

public class RuleTest {
    final SystemAttribute five = new SystemAttribute() {
        @Override
        public Object getValue() {
            return 5;
        }
    };
    
    @Test
    public void testWithDefaultEvaluator() {
        assertEquals("== 5", OK, new Rule(five,"5").execute());
        assertEquals("> 2", OK, new Rule(five,"> 2").execute());
        assertEquals("> 12 is false", ERROR, new Rule(five,"> 12").execute());
    }
}
