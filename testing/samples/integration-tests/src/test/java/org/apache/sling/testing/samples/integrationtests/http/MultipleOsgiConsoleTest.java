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

package org.apache.sling.testing.samples.integrationtests.http;

import static org.junit.Assert.assertEquals;
import org.apache.sling.testing.tools.sling.SlingInstance;
import org.apache.sling.testing.tools.sling.SlingInstancesRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Example testing several Sling instances */
public class MultipleOsgiConsoleTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Instance names must be consistent with the system properties that we get */
    final static String [] INSTANCE_NAMES = { "instance1", "instance2" };
    
    @Rule
    /** This causes our tests to be executed once for each specified instance */
    public final SlingInstancesRule rule = new SlingInstancesRule(INSTANCE_NAMES);
    
    // Verify that our SlingInstancesRule works properly
    // (not needed for regular tests - here we also test the testing framework)
    private static int numberOfTestsExecuted;
    
    @BeforeClass
    public static void beforeClass() {
        numberOfTestsExecuted = 0;
    }
    
    @AfterClass
    public static void afterClass() {
        assertEquals("Expecting all instances to be tested", 
                INSTANCE_NAMES.length * 2, numberOfTestsExecuted);
    }

    @Test
    public void testSomeConsolePaths() throws Exception {
        numberOfTestsExecuted++;
        final SlingInstance instance = rule.getSlingInstance();
        log.info("Running testSomeConsolePaths {} on {}", getClass().getSimpleName(), instance.getServerBaseUrl());

        final String [] subpaths = {
                "bundles",
                "components",
                "configMgr",
                "config",
                "licenses",
                "logs",
                "memoryusage",
                "services"
        };

        for(String subpath : subpaths) {
            final String path = "/system/console/" + subpath;
            instance.getRequestExecutor().execute(
                    instance.getRequestBuilder().buildGetRequest(path)
                            .withCredentials(instance.getServerUsername(), instance.getServerPassword())
            ).assertStatus(200);
        }
    }
    
    @Test
    public void checkRuleWithMoreThanOneTest() throws Exception {
        numberOfTestsExecuted++;
        final SlingInstance instance = rule.getSlingInstance();
        log.info("Running checkRuleWithMoreThanOneTest {} on {}", getClass().getSimpleName(), instance.getServerBaseUrl());

        final String path = "/system/console/bundles";
        instance.getRequestExecutor().execute(
                instance.getRequestBuilder().buildGetRequest(path)
                        .withCredentials(instance.getServerUsername(), instance.getServerPassword())
        ).assertStatus(200);
    }
}