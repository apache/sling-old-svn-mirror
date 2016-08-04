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

import org.apache.sling.testing.junit.rules.annotation.IgnoreIfProperty;
import org.apache.sling.testing.junit.rules.annotation.Issue;
import org.apache.sling.testing.junit.rules.FilterRule;
import org.apache.sling.testing.junit.rules.category.FailingTest;
import org.apache.sling.testing.junit.rules.category.SlowRunningTest;
import org.apache.sling.testing.junit.rules.util.IgnoreTestsConfig;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

//TODO use a better way to test this
public class FilterRuleExcludeCategoryIgnoreIfTest {

    @Rule
    public FilterRule testFilterRule = new FilterRule();

    @Rule
    public TestName name = new TestName();

    static {
        System.setProperty(FilterRule.CATEGORY_PROPERTY, "Issue,SlowRunningTest");
        System.setProperty("test.filterrule.a", "a");
        IgnoreTestsConfig.reCreate();
    }

    /*
     * System prop is set for ignoring tests and also for excluding a category Setup: a test is annotated with just
     * the @IgnoreIf Result: The test is skipped
     */
    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "a")
    public void testIgnoreIfOnly() {
        Assert.fail("Test should be Ignored");
    }

    /*
     * System prop is set for ignoring tests and also for excluding a category Setup: a test is annotated with
     * @IgnoreIf and @Category which is excluded Result: The test is skipped
     */
    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "a")
    @Category(Issue.class)
    public void testIgnoreIfPropExistsandExcludedCategoryExists() {
        Assert.fail("Test should be Ignored");
    }

    /*
     * System prop is set for ignoring tests and also for excluding a category Setup: a test is annotated with
     * @IgnoreIf and @Category which is excluded Result: The test is skipped
     */
    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "a")
    @Category(SlowRunningTest.class)
    public void testIgnoreIfPropExistsandExcludedCategoryExists_2() {
        Assert.fail("Test should be Ignored");
    }

    /*
     * System prop is set for ignoring tests and also for excluding a category Setup: a test is annotated with
     * @IgnoreIf and @Category which is not excluded Result: The test is skipped
     */
    @Test
    @IgnoreIfProperty(name = "test.filterrule.a", value = "a")
    @Category(FailingTest.class)
    public void testIgnoreIfPropExixtsandExcludedCategoryNotExists() {
        Assert.fail("Test should be Ignored");
    }

    /*
     * System prop is set for ignoring tests and also for excluding a category Setup: a test is annotated with
     * @Category which is excluded Result: The test is skipped
     */
    @Test
    @Category(Issue.class)
    @Ignore("SLING-5803")
    public void testExcludedCategoryExists() {
        Assume.assumeTrue(System.getProperty(FilterRule.CATEGORY_PROPERTY).equals("Issue,SlowRunningTest"));
        Assert.fail("Test should be Ignored");
    }

    /*
     * System prop is set for ignoring tests and also for excluding a category Setup: a test is annotated with
     * @Category which is not excluded Result: The test is not skipped
     */
    @Test
    @Category(FailingTest.class)
    public void testExcludedCategoryNotExists() {
        Assert.assertTrue("Test should be Run", true);
    }

    /*
     * System prop is set for ignoring tests and also for excluding a category Setup: a test is not annotated with
     * @Category & @IgnoreIf Result: The test is not skipped
     */
    @Test
    public void testNoAnnotationsExists() {
        Assert.assertTrue("Test should be Run", true);
    }

}
