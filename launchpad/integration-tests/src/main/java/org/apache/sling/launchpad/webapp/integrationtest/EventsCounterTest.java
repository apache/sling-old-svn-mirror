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
package org.apache.sling.launchpad.webapp.integrationtest;

import static org.junit.Assert.assertTrue;

import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.launchpad.webapp.integrationtest.util.EventsCounterUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/** Test the EventsCounter servlet and underlying events subsystems */
public class EventsCounterTest {
    
    private static int initialResourceEventsCount;
    private static String toDelete;
    public static final String TOPIC = "org/apache/sling/api/resource/Resource/ADDED";
    
    @Rule
    public RetryRule retryRule = new RetryRule();
    
    /** HTTP tests helper */
    private static final HttpTest H = new HttpTest();
    
    @BeforeClass
    public static void setupClass() throws Exception {
        H.setUp();
        initialResourceEventsCount = EventsCounterUtil.getEventsCount(H, TOPIC);
        final String testResourcePath = HttpTest.HTTP_BASE_URL + "/testing/" + EventsCounterTest.class.getName() + "." + System.currentTimeMillis();
        toDelete = H.getTestClient().createNode(testResourcePath, null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        H.getTestClient().delete(toDelete);
        H.tearDown();
    }
    
    @Retry
    @Test
    public void testResourceEvents() throws Exception {
        assertTrue("Expecting events counter to have changed", EventsCounterUtil.getEventsCount(H, TOPIC) > initialResourceEventsCount);
    }
}
