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
package org.apache.sling.testing.rules.util;

import org.apache.sling.testing.rules.RunOnceRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunOnceRuleTest {
    private static final Logger LOG = LoggerFactory.getLogger(RunOnceRule.class);
    public static int count = 0;
    
    @Rule
    public RunOnceRule roc = new RunOnceRule(new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            count ++;
            LOG.debug("Run {} times", count);
            return base;
        }
    });
    
    @Test
    public void test1() {
        Assert.assertEquals("Should have run only once", 1, count);
    }

    @Test
    public void test2() {
        Assert.assertEquals("Should have run only once", 1, count);
    }

    @Test
    public void test3() {
        Assert.assertEquals("Should have run only once", 1, count);
    }

    @Test
    public void test4() {
        Assert.assertEquals("Should have run only once", 1, count);
    }
    
}
