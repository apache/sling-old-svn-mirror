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
package org.apache.sling.testing.rules.util.filterrule;

import org.apache.sling.testing.rules.FilterRule;
import org.apache.sling.testing.rules.annotation.IgnoreIfProperties;
import org.apache.sling.testing.rules.annotation.IgnoreIfProperty;
import org.junit.*;

@IgnoreIfProperty(name = "test.filterrule.a", value = "x")
public class FilterRuleTest {
    @ClassRule
    public static FilterRule testFilterRuleClass = new FilterRule();

    @Rule
    public FilterRule testFilterRule = new FilterRule();

    @BeforeClass
    public static void beforeClass() {
        System.out.println("BeforeClass");
    }

    @Before
    public void before() {
        System.out.println("Before");
    }

    @After
    public void after() {
        System.out.println("After");
    }

    static {
        System.clearProperty(FilterRule.CATEGORY_PROPERTY);
        System.clearProperty(FilterRule.INCLUDE_CATEGORY_PROPERTY);

        System.setProperty("test.filterrule.a", "a");
        System.setProperty("test.filterrule.b", "b");
        System.setProperty("test.filterrule.c", "");
    }

    @Test
    public void testWithoutShouldRun() {
        // Should pass
    }

    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "x")
    public void testSingleShouldRun() {
        // Should pass
    }

    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "a")
    public void testSingleShouldSkip() {
        Assert.fail("Test should be Ignored");
    }

    @Test
    @IgnoreIfProperty(name = "test.filterrule.c")
    public void testSingleDefaultShouldSkip() {
        Assert.fail("Test should be Ignored");
    }

    @Test
    @IgnoreIfProperties({@IgnoreIfProperty(name = "test.filterrule.a", value = "a"),
            @IgnoreIfProperty(name = "test.filterrule.noexist")})
    public void testMultipleShouldSkip() {
        Assert.fail("Test should be Ignored");
    }

    @Test
    @IgnoreIfProperties({@IgnoreIfProperty(name = "test.filterrule.noexist1", value = "a"),
            @IgnoreIfProperty(name = "test.filterrule.noexist2")})
    public void testMultipleShouldPass() {
        // Should pass
    }

    @Test
    @IgnoreIfProperties({@IgnoreIfProperty(name = "test.filterrule.noexist1", value = "a"),
            @IgnoreIfProperty(name = "test.filterrule.noexist2")})
    @IgnoreIfProperty(name = "test.filterrule.noexist3")
    public void testBothShouldPass() {
        // Should pass
    }

    @Test
    @IgnoreIfProperties({@IgnoreIfProperty(name = "test.filterrule.noexist1", value = "a"),
            @IgnoreIfProperty(name = "test.filterrule.c")})
    @IgnoreIfProperty(name = "test.filterrule.noexist3")
    public void testBothShouldSkip1() {
        Assert.fail("Test should be Ignored");
    }

    @Test
    @IgnoreIfProperties({@IgnoreIfProperty(name = "test.filterrule.noexist1", value = "a"),
            @IgnoreIfProperty(name = "test.filterrule.noexist2")})
    @IgnoreIfProperty(name = "test.filterrule.b", value = "b")
    public void testBothShouldSkip2() {
        Assert.fail("Test should be Ignored");
    }
}
