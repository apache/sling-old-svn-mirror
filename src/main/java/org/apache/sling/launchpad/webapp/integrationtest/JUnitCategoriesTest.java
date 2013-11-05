/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.commons.testing.junit.categories.JackrabbitOnly;
import org.apache.sling.commons.testing.junit.categories.OakOnly;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/** Verify the our JUnit categories are set up correctly */
public class JUnitCategoriesTest {
    private static final Logger log = LoggerFactory.getLogger(JUnitCategoriesTest.class);
    private static final List<String> executedTests = new ArrayList<String>();
    
    @BeforeClass
    public static void clearExecutedTests() {
        executedTests.clear();
    }
    
    @AfterClass
    public static void checkExecutedTests() {
        if(executedTests.size() < 1) {
            fail("At least one of the Jackrabbit/Oak test categories should be active");
        } else if(executedTests.size() > 1) {
            fail("Only one of the Jackrabbit/Oak test category should be active:" + executedTests);
        }
        log.info("JUnit categories are set up correctly:" + executedTests);
    }
    
    @Category(JackrabbitOnly.class)
    @Test
    public void jackrabbitTest() {
        executedTests.add("Jackrabbit-only test was executed");
    }
    
    @Category(OakOnly.class)
    @Test
    public void oakTest() {
        executedTests.add("Oak-only test was executed");
    }
}