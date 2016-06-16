/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.compiler;

import java.util.List;

import org.apache.sling.scripting.sightly.compiler.CompilationResult;
import org.apache.sling.scripting.sightly.compiler.CompilationUnit;
import org.apache.sling.scripting.sightly.compiler.CompilerMessage;
import org.apache.sling.scripting.sightly.compiler.SightlyCompiler;
import org.apache.sling.scripting.sightly.impl.TestUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SightlyCompilerTest {

    private SightlyCompiler compiler = new SightlyCompiler();

    @Test
    public void testEmptyExpression() {
        CompilationResult result = compile("/empty-expression.html");
        assertTrue("Didn't expect any warnings or errors.", result.getErrors().size() == 0 && result.getWarnings().size() == 0);
    }

    @Test
    public void testMissingExplicitContext() {
        String script = "/missing-explicit-context.html";
        CompilationResult result = compile(script);
        List<CompilerMessage> warnings = result.getWarnings();
        assertTrue("Expected compilation warnings.", warnings.size() == 1);
        CompilerMessage warningMessage = warnings.get(0);
        assertTrue(script.equals(warningMessage.getScriptName()));
        assertEquals("${some.value}: Element script requires that all expressions have an explicit context specified. The expression will" +
                " be replaced with an empty string.", warningMessage.getMessage());
    }

    @Test
    public void testSensitiveAttributes() {
        String script = "/sensitive-attributes.html";
        CompilationResult result = compile(script);
        List<CompilerMessage> warnings = result.getWarnings();
        assertTrue("Expected compilation warnings.", warnings.size() == 2);
        CompilerMessage _1stWarning = warnings.get(0);
        CompilerMessage _2ndWarning = warnings.get(1);
        assertEquals(script, _1stWarning.getScriptName());
        assertEquals(script, _2ndWarning.getScriptName());
        assertEquals("${style.string}: Expressions within the value of attribute style need to have an explicit context option. The " +
                "expression will be replaced with an empty string.", _1stWarning.getMessage());
        assertEquals("${onclick.action}: Expressions within the value of attribute onclick need to have an explicit context option. The " +
                "expression will be replaced with an empty string.", _2ndWarning.getMessage());

    }

    private CompilationResult compile(String file) {
        CompilationUnit compilationUnit = TestUtils.readScriptFromClasspath(file);
        return compiler.compile(compilationUnit);
    }


}
