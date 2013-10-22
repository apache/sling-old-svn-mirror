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

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Verify the repository name, to make sure we're testing the right one */
public class RepositoryNameTest extends HttpTestBase {
    
    /** Use the same property that Sling uses to switch between repositories.
     *  Current run modes are "jackrabbit" and "oak".
     */
    public static final String RUN_MODE_PROP = "sling.run.modes";
    public static final String DEFAULT_RUN_MODE = "jackrabbit";
    
    public void testName() throws IOException {
        final String path = "/testing/RepositoryDescriptors.txt";
        final String runMode = System.getProperty(RUN_MODE_PROP, DEFAULT_RUN_MODE);
        final String content = getContent(HTTP_BASE_URL + path, CONTENT_TYPE_PLAIN).toLowerCase();
        final Pattern expectedPattern = Pattern.compile("(?s).*jcr.repository.name=[^$]*" + runMode + ".*");
        assertTrue(
                "Expecting lowercased content at " + path + " to match " + expectedPattern + ", content=" + content, 
                expectedPattern.matcher(content).matches());
    }
}
