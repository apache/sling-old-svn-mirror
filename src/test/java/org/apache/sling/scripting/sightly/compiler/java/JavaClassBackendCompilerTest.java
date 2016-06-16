/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler.java;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.commons.io.IOUtils;
import org.apache.sling.scripting.sightly.compiler.CompilationUnit;
import org.apache.sling.scripting.sightly.compiler.SightlyCompiler;
import org.apache.sling.scripting.sightly.compiler.java.utils.CharSequenceJavaCompiler;
import org.apache.sling.scripting.sightly.compiler.java.utils.TestUtils;
import org.apache.sling.scripting.sightly.java.compiler.ClassInfo;
import org.apache.sling.scripting.sightly.java.compiler.JavaClassBackendCompiler;
import org.apache.sling.scripting.sightly.java.compiler.RenderUnit;
import org.apache.sling.scripting.sightly.render.AbstractRuntimeObjectModel;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class JavaClassBackendCompilerTest {

    @Test
    public void testScript() throws Exception {
        CompilationUnit compilationUnit = TestUtils.readScriptFromClasspath("/test.html");
        JavaClassBackendCompiler backendCompiler = new JavaClassBackendCompiler();
        SightlyCompiler sightlyCompiler = new SightlyCompiler();
        sightlyCompiler.compile(compilationUnit, backendCompiler);
        ClassInfo classInfo = new ClassInfo() {
            @Override
            public String getSimpleClassName() {
                return "Test";
            }

            @Override
            public String getPackageName() {
                return "org.example.test";
            }

            @Override
            public String getFullyQualifiedClassName() {
                return "org.example.test.Test";
            }
        };
        String source = backendCompiler.build(classInfo);
        ClassLoader classLoader = JavaClassBackendCompilerTest.class.getClassLoader();
        CharSequenceJavaCompiler<RenderUnit> compiler = new CharSequenceJavaCompiler<>(classLoader, null);
        Class<RenderUnit> newClass = compiler.compile(classInfo.getFullyQualifiedClassName(), source, new Class<?>[]{});
        RenderUnit renderUnit = newClass.newInstance();
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        RenderContext renderContext = new RenderContext() {
            @Override
            public AbstractRuntimeObjectModel getObjectModel() {
                return new AbstractRuntimeObjectModel() {};
            }

            @Override
            public Bindings getBindings() {
                return new SimpleBindings();
            }

            @Override
            public Object call(String functionName, Object... arguments) {
                assert arguments.length == 2;
                // for this test case only the xss runtime function will be called; return the unfiltered input
                return arguments[0];
            }
        };
        renderUnit.render(printWriter, renderContext, new SimpleBindings());
        String expectedOutput = IOUtils.toString(this.getClass().getResourceAsStream("/test-output.html"), "UTF-8");
        assertEquals(expectedOutput, writer.toString());

    }
}
