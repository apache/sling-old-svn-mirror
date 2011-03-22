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
package org.apache.sling.junit.remote.exported;

import static org.junit.Assert.fail;
import org.apache.sling.junit.remote.ide.SlingRemoteExecutionRule;
import org.apache.sling.testing.tools.http.Request;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test that can be run remotely on a Sling instance from an IDE, by
 *  setting the {@link SlingRemoteExecutionRule.SLING_REMOTE_TEST_URL}
 *  system property in the IDE setup, to the URL of 
 *  the Sling JUnit servlet (like http://localhost:8080/system/sling/junit)
 */
public class ExampleRemoteTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Execute this test remotely and customize the request (could be
     *  used to set credentials for example)
     */
    @Rule
    public SlingRemoteExecutionRule execRule = new SlingRemoteExecutionRule() {
        @Override
        public void customizeRequest(Request r) {
            log.info("Customizing request {}", r);
        }
    };
    
    @Test
    public void testAlwaysPasses() {
    }
    
    @Test
    public void testAlwaysFails() {
        fail("This test always fails");
    }
    
    @Test
    public void testFailsSometimes() {
        if(Math.random() < 0.5) {
            fail("This test fails sometimes");
        }
    }
}