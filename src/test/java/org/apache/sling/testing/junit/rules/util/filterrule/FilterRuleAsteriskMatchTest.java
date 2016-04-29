/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.junit.rules.util.filterrule;

import org.apache.sling.testing.junit.rules.util.IgnoreTestsConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class FilterRuleAsteriskMatchTest {



    private final String pattern;
    private final String text;
    private final boolean match;

    public FilterRuleAsteriskMatchTest(String pattern, String text, boolean match) {
        this.pattern = pattern;
        this.text = text;
        this.match = match;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"a.b.c", "a.b.c", true},
                {"a.b.c.*", "a.b.c.d", true},
                {"*.c", "a.b.c", true},
                {"*", "a.b.c.MyTest", true},
                {"*.MyTest", "a.b.c.MyTest", true},
                {"a.b.*.c", "a.b.x.y.c", true},
                {"a.b.*.c.*", "a.b.x.y.c.MyTest", true},

                {"a.b.*.c", "a.b.x.y.c.NotMyTest", false},
                {"*.MyTest", "a.b.c.NotMyTest", false},
                {"*.c", "a.b.c.d", false},
                {"a.b.c", "x", false},
                {"", "x", false},
        });
    }

    @Test
    public void testAsteriskMatch() {
        Assert.assertEquals(this.match, IgnoreTestsConfig.asteriskMatch(this.pattern, this.text));
    }
}
