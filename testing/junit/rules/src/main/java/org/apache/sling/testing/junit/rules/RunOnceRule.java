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
package org.apache.sling.testing.junit.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allows wrapping a junit {@link TestRule} that will be run only once.
 * Once means once per class RunOnceRule being loaded 
 */
public class RunOnceRule implements TestRule {
    private static final Logger LOG = LoggerFactory.getLogger(RunOnceRule.class);
    private static AtomicBoolean run = new AtomicBoolean(false);
    private final TestRule rule;

    /**
     * Constructor*
     * @param rule The rule to wrap and run once per classloader load
     * @param <T> The class of the TestRule
     */
    public <T extends TestRule> RunOnceRule(T rule) {
        this.rule = rule;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        // if not done yet
        if (!RunOnceRule.run.getAndSet(true)) {
            LOG.debug("Applying {} once", rule.getClass());
            return rule.apply(base, description);
        } else {
            return base;
        }
    }
}
