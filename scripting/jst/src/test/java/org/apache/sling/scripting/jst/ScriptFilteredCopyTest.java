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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.sling.commons.testing.util.TestStringUtil;

/** Test the ScriptFilteredCopy */
public class ScriptFilteredCopyTest extends TestCase {
    
    private final ScriptFilteredCopy sfc = new ScriptFilteredCopy();
    
    private void runTest(String input, String expected) throws IOException {
        final StringWriter sw = new StringWriter();
        sfc.copy(new StringReader(input), sw);
        assertEquals(TestStringUtil.flatten(expected), TestStringUtil.flatten(sw.toString()));
    }
    
    public void testNoChanges() throws IOException {
        final String input = "No out.write statements on their own lines";
        final String expected = input + "\n";
        runTest(input,expected);
    }
    
    public void testOpenClose() throws IOException {
        final String input = 
            "A\n"
            + "out.write(\"some <script> here </script>\");\n"
            + "B\n";
        
        final String expected = 
        "A\n"
        + "out.write(\"some <\");\n"
        + "out.write(\"script> here </\");\n"
        + "out.write(\"script>\");\n"
        + "B\n";
        
        runTest(input,expected);
    }
    
    public void testOpenAtStart() throws IOException {
        final String input = "out.write(\"<script> here\");\n";
        final String expected = 
            "out.write(\"<\");\n"
            + "out.write(\"script> here\");\n"
        ;
        
        runTest(input,expected);
    }

    public void testOpenAtEnd() throws IOException {
        final String input = "out.write(\"Here a <script>\");\n";
        final String expected = 
            "out.write(\"Here a <\");\n"
            + "out.write(\"script>\");\n"
        ;
        runTest(input,expected);
    }
}
