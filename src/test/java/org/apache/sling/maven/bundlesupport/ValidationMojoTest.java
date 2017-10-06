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
package org.apache.sling.maven.bundlesupport;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ValidationMojoTest {
    
    private static String MESSAGE = "JSON validation failed: Unexpected character '\n' (Codepoint: 10) "
            + "on [lineNumber=47, columnNumber=1434, streamOffset=2866]. "
            + "Reason is [[End of file hit too early]]";

    @Test
    public void testParseLineNumber() {
        assertEquals(47, ValidationMojo.parseLineNumber(MESSAGE));
    }

    @Test
    public void testParseColumnNumber() throws Exception {
        assertEquals(1434, ValidationMojo.parseColumnNumber(MESSAGE));
    }

    @Test
    public void testCleanupMessage() throws Exception {
        assertEquals("JSON validation failed: Unexpected character '\\n' (Codepoint: 10). Reason is [[End of file hit too early]]",
                ValidationMojo.cleanupMessage(MESSAGE));
    }

}
