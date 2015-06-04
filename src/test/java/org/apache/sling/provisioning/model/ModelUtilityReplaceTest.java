/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.provisioning.model;

import static org.junit.Assert.assertEquals;

import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;
import org.junit.Test;

public class ModelUtilityReplaceTest {
    
    private static final Feature TEST_FEATURE = new Feature("testFeature");
    static {
        TEST_FEATURE.getVariables().put("var1", "value1");
        TEST_FEATURE.getVariables().put("var2", "value2");
    }

    @Test
    public void testNoReplace() {
        assertEquals("nothing to replace", ModelUtility.replace(TEST_FEATURE, "nothing to replace", null));
    }

    @Test
    public void testOneReplace() {
        assertEquals("one value1 variable", ModelUtility.replace(TEST_FEATURE, "one ${var1} variable", null));
        assertEquals("value1 one variable", ModelUtility.replace(TEST_FEATURE, "${var1} one variable", null));
        assertEquals("value1 one variable value1", ModelUtility.replace(TEST_FEATURE, "${var1} one variable ${var1}", null));
    }

    @Test
    public void testTwoReplaces() {
        assertEquals("two value1 variables value2", ModelUtility.replace(TEST_FEATURE, "two ${var1} variables ${var2}", null));
        assertEquals("value1value2 two variables", ModelUtility.replace(TEST_FEATURE, "${var1}${var2} two variables", null));
        assertEquals("value2 two variables value1", ModelUtility.replace(TEST_FEATURE, "${var2} two variables ${var1}", null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidVariable() {
        assertEquals("one value1 variable", ModelUtility.replace(TEST_FEATURE, "one ${var99} variable", null));
    }

    @Test
    public void testEscapedVariable() {
        assertEquals("escaped \\${var99} variable", ModelUtility.replace(TEST_FEATURE, "escaped \\${var99} variable", null));
        assertEquals("\\${var99} escaped variable", ModelUtility.replace(TEST_FEATURE, "\\${var99} escaped variable", null));
        assertEquals("escaped variable \\${var99}", ModelUtility.replace(TEST_FEATURE, "escaped variable \\${var99}", null));
        assertEquals("escaped \\${var1} variable value2", ModelUtility.replace(TEST_FEATURE, "escaped \\${var1} variable ${var2}", null));
    }

    @Test
    public void testOneResolver() {
        VariableResolver resolver = new VariableResolver() {
            @Override
            public String resolve(Feature feature, String name) {
                return name.toUpperCase();
            }
        };
        assertEquals("one VAR1 variable", ModelUtility.replace(TEST_FEATURE, "one ${var1} variable", resolver));
        assertEquals("VAR1 one variable", ModelUtility.replace(TEST_FEATURE, "${var1} one variable", resolver));
        assertEquals("VAR1 one variable VAR2", ModelUtility.replace(TEST_FEATURE, "${var1} one variable ${var2}", resolver));
    }

}
