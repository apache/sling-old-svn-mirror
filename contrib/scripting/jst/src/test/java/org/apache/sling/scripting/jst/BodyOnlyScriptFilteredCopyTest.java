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

/** Test the BodyOnlyScriptFilteredCopy */
public class BodyOnlyScriptFilteredCopyTest extends TestCase {
    
    private final BodyOnlyScriptFilteredCopy bfc = new BodyOnlyScriptFilteredCopy();
    
    private void runTest(String input, String expected) throws IOException {
        final StringWriter sw = new StringWriter();
        bfc.copy(new StringReader(input), sw);
        assertEquals(TestStringUtil.flatten(expected), TestStringUtil.flatten(sw.toString()));
    }
    
    public void testNoChanges() throws IOException {
        final String input = "No out.write statements on their own lines";
        final String expected = "";
        runTest(input,expected);
    }
    
    public void testBasicTemplate() throws IOException {
        final String input = 
            "out.write('Something before body');\n"
            + "out.write('<body class='foo'>');\n"
            + "out.write(\"some template code\");\n"
            + "out.write('</body>');\n"
            + "out.write('Something after body');\n"
        ;
            
        final String expected = 
            "out.write(\"some template code\");\n"
            ;
        
        runTest(input,expected);
    }
    
    public void testOpenClose() throws IOException {
        final String input = 
            "A\n"
            + "out.write('    <body class='foo'>');\n"
            + "out.write(\"some <script> here </script>\");\n"
            + "out.write('</body>');\n"
            + "B\n";
        
        final String expected = 
        "out.write(\"some <\");\n"
        + "out.write(\"script> here </\");\n"
        + "out.write(\"script>\");\n";
        
        runTest(input,expected);
    }
    
    public void test_SLING_615() throws IOException {
        final String input =
            "out.write(' <meta name='decorator' content='tssdotcom'></head><body>');\n"
            + "out.write('the body');\n"
            + "out.write('</body></html>');\n"
            ;
        
        final String expected =
            "out.write('the body');\n"
            ;
        
        runTest(input,expected);
    }
}
