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
import org.apache.sling.testing.junit.rules.util.IgnoreTestsConfig;
import org.junit.*;

public class FilterRuleIgnorelistTest {

    @ClassRule
    public static FilterRule classRule = new FilterRule();
    @Rule
    public FilterRule methodRule = new FilterRule();

    static {
        System.clearProperty(IgnoreTestsConfig.IGNORE_LIST_PROP);
        System.setProperty(IgnoreTestsConfig.IGNORE_LIST_PROP, "*.FilterRuleIgnorelistTest#shouldSkip:WTF-9999, *.notmypackage.*");
        IgnoreTestsConfig.reCreate();
    }

    @AfterClass
    public static void clearIgnoreList() {
        System.clearProperty(IgnoreTestsConfig.IGNORE_LIST_PROP);
    }

    @Test
    public void shouldSkip() {
        Assert.fail("Test should be skipped");
    }

    @Test
    public void shouldPass() {

    }

}
