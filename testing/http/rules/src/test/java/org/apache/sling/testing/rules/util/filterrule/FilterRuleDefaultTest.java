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
import org.apache.sling.testing.rules.annotation.IgnoreIfProperty;
import org.apache.sling.testing.rules.category.FailingTest;
import org.apache.sling.testing.rules.category.SlowRunningTest;
import org.junit.*;
import org.junit.experimental.categories.Category;

@IgnoreIfProperty(name = "test.filterrule.a", value = "x")
public class FilterRuleDefaultTest {
    @ClassRule
    public static FilterRule testFilterRuleClass = new FilterRule().addDefaultIgnoreCategories(FailingTest.class);

    @Rule
    public FilterRule testFilterRule = new FilterRule().addDefaultIgnoreCategories(FailingTest.class);

    @BeforeClass
    public static void beforeClass() {
        System.out.println("BeforeClass");
        System.clearProperty(FilterRule.CATEGORY_PROPERTY);
        System.clearProperty(FilterRule.INCLUDE_CATEGORY_PROPERTY);
    }

    @Before
    public void before() {
        System.out.println("Before");
    }

    @After
    public void after() {
        System.out.println("After");
    }

    @Test
    public void testWithoutShouldRun() {
        // Should pass
    }

    @Test
    @Category(SlowRunningTest.class)
    public void testSingleShouldRun() {
        // Should pass
    }

    @Test
    @Category(FailingTest.class)
    public void testSingleShouldSkip() {
        Assert.fail("Test should be Ignored");
    }
}
