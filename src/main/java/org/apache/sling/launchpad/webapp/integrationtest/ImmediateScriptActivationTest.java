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
import java.util.concurrent.atomic.AtomicInteger;

/** Upload many different scripts in sequence and verify that they are immediately
 *  available to render content.
 */
public class ImmediateScriptActivationTest extends AbstractSlingResourceTypeRenderingTest {

    public static final int LOOP_COUNT = 10;
    private final AtomicInteger counter = new AtomicInteger();
    
    @Override
    protected void setUp() throws Exception {
        slingResourceType = getClass().getSimpleName() + "." + counter.incrementAndGet();
        super.setUp();
    }
    
    private void assertScriptActivation(String testId, String testScript, String extension, String expectedContent) throws IOException {
        final String testUrl = displayUrl + "." + testId + ".html";
        {
            final String content = getContent(testUrl, CONTENT_TYPE_HTML);
            assertFalse("Expecting no rendering script initially", content.contains(expectedContent));
        }
        
        {
            final String toDelete = uploadTestScript(testScript,testId + ".html" + extension);
            try {
                final String content = getContent(testUrl, CONTENT_TYPE_HTML);
                assertContains(content, expectedContent);
            } finally {
                testClient.delete(toDelete);
            }
        }
    }
    
    public void testEspActivation() throws IOException {
        for(int i=0; i < LOOP_COUNT; i++) {
            assertScriptActivation("test_" + i, "rendering-test.esp", ".esp", "ESP template");
        }
    }
    
    public void testJspActivation() throws IOException {
        for(int i=0; i < LOOP_COUNT; i++) {
            assertScriptActivation("test_" + i, "rendering-test.jsp", ".jsp", "<h1>JSP rendering result</h1>");
        }
    }
}