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

import org.apache.sling.testing.junit.rules.FilterRule;
import org.apache.sling.testing.junit.rules.annotation.IgnoreIfProperty;
import org.apache.sling.testing.junit.rules.annotation.Issue;
import org.apache.sling.testing.junit.rules.category.FailingTest;
import org.apache.sling.testing.junit.rules.util.IgnoreTestsConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

public class FilterRuleExcludeCategoryTest {

    @Rule
    public FilterRule testFilterRule = new FilterRule();

    @Rule
    public TestName name = new TestName();

    static {
        System.setProperty(FilterRule.CATEGORY_PROPERTY, "Issue");
        IgnoreTestsConfig.reCreate();
    }

    /*
     * System prop is set for excluding a category only Setup: a test is annotated with just the @IgnoreIf Result: The
     * test is executed
     */
    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "a")
    public void testIgnoreIfOnly() {
        Assert.assertTrue("Test should be Run", true);
    }

    /*
     * System prop is set for excluding a category only Setup: a test is annotated with @IgnoreIf and @Category which
     * is excluded Result: The test is skipped
     */
    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "a")
    @Category(Issue.class)
    public void testIgnoreIfPropExistsandExcludedCategoryExists() {
        Assert.fail("Test should be Ignored");
    }

    /*
     * System prop is set for excluding a category only Setup: a test is annotated with @IgnoreIf and @Category which
     * is not excluded Result: The test is skipped
     */
    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "a")
    @Category(FailingTest.class)
    public void testIgnoreIfPropExixtsandExcludedCategoryNotExists() {
        Assert.assertTrue("Test should be Run", true);
    }

    /*
     * System prop is set for excluding a category only Setup: a test is annotated with @Category which is excluded
     * Result: The test is skipped
     */
    @Test
    @Category(Issue.class)
    public void testExcludedCategoryExists() {
        Assert.fail("Test should be Ignored");
    }

    /*
     * System prop is set for excluding a category only Setup: a test is annotated with @Category which is not
     * excluded Result: The test is executed
     */
    @Test
    @Category(FailingTest.class)
    public void testExcludedCategoryNotExists() {
        Assert.assertTrue("Test should be Run", true);
    }

    /*
     * System prop is set for ignoring tests and also for excluding a category Setup: a test is not annotated with
     * @Category & @IgnoreIf Result: The test is skipped
     */
    @Test
    public void testNoAnnotationsExists() {
        Assert.assertTrue("Test should be Run", true);
    }

}
