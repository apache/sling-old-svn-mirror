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

import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Base class for rendering tests
 */
public abstract class RenderingTestBase extends HttpTestBase {
    protected String scriptPath;
    protected String testText;
    protected String displayUrl;
    
    protected String uploadTestScript(String localFilename,String filenameOnServer) throws IOException {
        return uploadTestScript(scriptPath, localFilename, filenameOnServer);
    }
    
    protected void assertContains(String content, String expected) {
        HttpTest.assertContains(content, expected);
    }
    
    protected void assertNotContains(String content, String notExpected) {
        HttpTest.assertNotContains(content, notExpected);
    }
}
