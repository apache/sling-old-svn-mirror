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
 * Sling Rule that wraps all the ClassRules, useful when running a Sling Integration Test.
 * Can be used in any junit test class using @ClassRule annotation
 */
public class SlingBaseClassRule implements TestRule {
    /** Rule to filter tests at class level */
    public final FilterRule filterRule = new FilterRule().addDefaultIgnoreCategories(FailingTest.class);

    /** Main RuleChain describing the order of execution of all the rules */
    protected TestRule ruleChain = RuleChain.outerRule(filterRule);

    @Override
    public Statement apply(Statement base, Description description) {
        return ruleChain.apply(base, description);
    }
}
