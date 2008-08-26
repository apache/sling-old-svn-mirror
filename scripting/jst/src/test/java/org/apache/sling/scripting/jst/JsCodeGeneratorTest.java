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
package org.apache.sling.scripting.jst;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.sling.commons.testing.util.TestStringUtil;

/** Test the JsCodeGenerator */
public class JsCodeGeneratorTest extends TestCase {

    public static final String ENCODING = "UTF-8";
    public static final String TEST_RESOURCES_PATH = "/jscode";
    private final JsCodeGenerator generator = new JsCodeGenerator();
    
    /** Pass a .input.jst test file through the JsCodeGenerator,
     *  and verify that the result matches the corresponding
     *  .expected.js file. 
     */
    protected boolean runTest(int index) throws Exception {
        final String input = TEST_RESOURCES_PATH + "/jscode" + index + ".input.jst";
        final InputStream scriptStream = getClass().getResourceAsStream(input);
        if(scriptStream == null) {
            // no more test files
            return false;
        }
        assertNotNull(input + " must be found", scriptStream);
        final Reader r = new InputStreamReader(scriptStream, ENCODING);
        
        final String expectedCode = StringUtil.readClassResource(
                getClass(), TEST_RESOURCES_PATH + "/jscode" + index + ".expected.js", ENCODING);
        final StringWriter result = new StringWriter();
        generator.generateCode(r, new PrintWriter(result));
        
        assertEquals("Generated code must match expected code for " + input, 
                TestStringUtil.flatten(expectedCode.toString().trim()), 
                TestStringUtil.flatten(result.toString().trim())
                );
        
        return true;
    }
    
    public void testJscodeTestFiles() throws Exception {
        int maxTestFiles = 999;
        int expectedTests = 2;
        int counter = 0;
        
        for(int i=1 ; i <= maxTestFiles; i++) {
            if(!runTest(i)) {
                break;
            }
            counter++;
        }
        
        assertTrue("At least " + expectedTests + " have been run", counter >= expectedTests);
    }
}
