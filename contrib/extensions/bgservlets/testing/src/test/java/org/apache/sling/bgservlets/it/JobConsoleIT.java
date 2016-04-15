/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.bgservlets.it;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.sling.commons.testing.integration.HttpTest;
import org.junit.Before;
import org.junit.Test;

/** Verify that our job console plugin is active and can read jobs */
public class JobConsoleIT {
    private final HttpTest T = new HttpTest();
    
    @Before
    public void setup() throws Exception {
        T.setUp();
    }
    
    @Test
    public void testJobConsoleOutput() throws IOException,InterruptedException {
        // Create a job
        T.assertPostStatus(HttpTest.HTTP_BASE_URL + "/tmp.json?sling:bg=true", 302, null, null);
        
        // Request must have created a job
        final long timeout = System.currentTimeMillis() + 10000L;
        final String path = "/system/console/bgservlets";
        final Pattern instanceIdPattern = Pattern.compile("(?s).+([0-9a-fA-F\\\\-]){20,}.+");
        while(true) {
            final String jobConsoleContent = T.getContent(HttpTest.HTTP_BASE_URL + path, HttpTest.CONTENT_TYPE_HTML);
            if(instanceIdPattern.matcher(jobConsoleContent).matches()) {
                break;
            }
            if(System.currentTimeMillis()  > timeout) {
                fail("Timeout waiting for instance ID at " + path);
            }
            Thread.sleep(50L);
        }
    }
}
