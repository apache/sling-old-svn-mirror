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
import java.util.HashMap;

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
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class JavaClassBackendCompilerTest {

    @Test
    public void testScript() throws Exception {
        CompilationUnit compilationUnit = TestUtils.readScriptFromClasspath("/test.html");
        JavaClassBackendCompiler backendCompiler = new JavaClassBackendCompiler();
        SightlyCompiler sightlyCompiler = new SightlyCompiler();
        sightlyCompiler.compile(compilationUnit, backendCompiler);
        ClassInfo classInfo = buildClassInfo("testScript");
        String source = backendCompiler.build(classInfo);
        StringWriter writer = new StringWriter();
        Bindings bindings = new SimpleBindings();
        RenderContext renderContext = buildRenderContext(bindings);
        render(writer, classInfo, source, renderContext, new SimpleBindings());
        String expectedOutput = IOUtils.toString(this.getClass().getResourceAsStream("/test-output.html"), "UTF-8");
        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    public void sling_6094_1() throws Exception {
        CompilationUnit compilationUnit = TestUtils.readScriptFromClasspath("/SLING-6094.1.html");
        JavaClassBackendCompiler backendCompiler = new JavaClassBackendCompiler();
        SightlyCompiler sightlyCompiler = new SightlyCompiler();
        sightlyCompiler.compile(compilationUnit, backendCompiler);
        ClassInfo classInfo = buildClassInfo("sling_6094_1");
        String source = backendCompiler.build(classInfo);
        StringWriter writer = new StringWriter();
        Bindings bindings = new SimpleBindings();
        bindings.put("img", new HashMap<String, Object>(){{
            put("attributes", new HashMap<String, String>() {{
                put("v-bind:src", "replaced");
            }});
        }});
        RenderContext renderContext = buildRenderContext(bindings);
        render(writer, classInfo, source, renderContext, new SimpleBindings());
        String expectedOutput = IOUtils.toString(this.getClass().getResourceAsStream("/SLING-6094.1.output.html"), "UTF-8");
        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    public void sling_6094_2() throws Exception {
        CompilationUnit compilationUnit = TestUtils.readScriptFromClasspath("/SLING-6094.2.html");
        JavaClassBackendCompiler backendCompiler = new JavaClassBackendCompiler();
        SightlyCompiler sightlyCompiler = new SightlyCompiler();
        sightlyCompiler.compile(compilationUnit, backendCompiler);
        ClassInfo classInfo = buildClassInfo("sling_6094_2");
        String source = backendCompiler.build(classInfo);
        StringWriter writer = new StringWriter();
        Bindings bindings = new SimpleBindings();
        RenderContext renderContext = buildRenderContext(bindings);
        render(writer, classInfo, source, renderContext, new SimpleBindings());
        String expectedOutput = IOUtils.toString(this.getClass().getResourceAsStream("/SLING-6094.2.output.html"), "UTF-8");
        assertEquals(expectedOutput, writer.toString());
    }

    private ClassInfo buildClassInfo(final String info) {
        return new ClassInfo() {
            @Override
            public String getSimpleClassName() {
                return "Test_" + info;
            }

            @Override
            public String getPackageName() {
                return "org.apache.sling.scripting.sightly.compiler.java";
            }

            @Override
            public String getFullyQualifiedClassName() {
                return "org.apache.sling.scripting.sightly.compiler.java.Test_" + info;
            }
        };
    }

    private RenderContext buildRenderContext(final Bindings bindings) {
        return new RenderContext() {
            @Override
            public RuntimeObjectModel getObjectModel() {
                return new AbstractRuntimeObjectModel() {};
            }

            @Override
            public Bindings getBindings() {
                return bindings;
            }

            @Override
            public Object call(String functionName, Object... arguments) {
                return arguments[0];
            }
        };
    }

    private void render(StringWriter writer, ClassInfo classInfo, String source, RenderContext renderContext, Bindings arguments) throws
            Exception {
        ClassLoader classLoader = JavaClassBackendCompilerTest.class.getClassLoader();
        CharSequenceJavaCompiler<RenderUnit> compiler = new CharSequenceJavaCompiler<>(classLoader, null);
        Class<RenderUnit> newClass = compiler.compile(classInfo.getFullyQualifiedClassName(), source);
        RenderUnit renderUnit = newClass.newInstance();
        PrintWriter printWriter = new PrintWriter(writer);
        renderUnit.render(printWriter, renderContext, arguments);
    }
}
