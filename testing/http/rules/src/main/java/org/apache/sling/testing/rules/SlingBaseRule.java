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
package org.apache.sling.testing.rules;

import org.apache.sling.testing.rules.category.FailingTest;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Sling Rule that wraps all the Rules (at method level) useful when running a Sling Integration Test.
 * Can be used in any junit test using the @Rule annotation
 */
public class SlingBaseRule implements TestRule {
    /** Rule to define the max timeout for all the tests */
    public final TestTimeout testTimeoutRule = new TestTimeout();

    /** Rule to add a Sticky Session Cookie for requests */
    public final TestStickyCookieRule testStickySessionRule = new TestStickyCookieRule();

    /** Rule to filter tests per method name */
    public final FilterRule filterRule = new FilterRule().addDefaultIgnoreCategories(FailingTest.class);

    /** Rule to send in every request a header with the test name */
    public final TestDescriptionRule testDescriptionRule = new TestDescriptionRule();

    /** Main RuleChain describing the order of execution of all the rules */
    protected TestRule ruleChain = RuleChain.outerRule(testTimeoutRule)
            .around(testStickySessionRule)
            .around(filterRule)
            .around(testDescriptionRule);

    @Override
    public Statement apply(Statement base, Description description) {
        return ruleChain.apply(base, description);
    }
}
