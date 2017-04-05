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
package org.apache.sling.scripting.sightly.java.compiler;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.scripting.sightly.compiler.backend.BackendCompiler;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.compiler.commands.StatefulVisitor;
import org.apache.sling.scripting.sightly.java.compiler.impl.CodeGenVisitor;
import org.apache.sling.scripting.sightly.java.compiler.impl.CommandVisitorHandler;
import org.apache.sling.scripting.sightly.java.compiler.impl.JavaClassTemplate;
import org.apache.sling.scripting.sightly.java.compiler.impl.UnitBuilder;

/**
 * {@link BackendCompiler} that generates a Java class.
 */
public final class JavaClassBackendCompiler implements BackendCompiler {

    private static final String COMPILED_UNIT_TEMPLATE = "templates/compiled_unit_template.txt";
    private static final String SUBTEMPLATE = "templates/subtemplate.txt";
    private static final String mainTemplate;
    private static final String childTemplate;

    private UnitBuilder unitBuilder = new UnitBuilder();

    static {
        try {
            mainTemplate = IOUtils
                    .toString(JavaClassTemplate.class.getClassLoader().getResourceAsStream(COMPILED_UNIT_TEMPLATE), "UTF-8");
            childTemplate = IOUtils.toString(JavaClassTemplate.class.getClassLoader().getResourceAsStream(SUBTEMPLATE), "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public void handle(CommandStream stream) {
        StatefulVisitor statefulVisitor = new StatefulVisitor();
        final CodeGenVisitor visitor = new CodeGenVisitor(unitBuilder, statefulVisitor.getControl());
        statefulVisitor.initializeWith(visitor);
        stream.addHandler(new CommandVisitorHandler(statefulVisitor) {
            @Override
            public void onDone() {
                super.onDone();
                visitor.finish();
            }
        });
    }

    /**
     * Provided the class information, this method will build the source code for the generated Java class.
     *
     * @param classInfo the class information
     * @return the generated Java class' source code
     */
    public String build(ClassInfo classInfo) {
        CompilationOutput compilationOutput = unitBuilder.build();
        JavaClassTemplate mainTemplate = newMainTemplate();
        mainTemplate.setPackageName(classInfo.getPackageName());
        mainTemplate.setClassName(classInfo.getSimpleClassName());
        processCompilationResult(compilationOutput, mainTemplate);
        return mainTemplate.toString();
    }

    private void processCompilationResult(CompilationOutput result, JavaClassTemplate mainTemplate) {
        mainTemplate.writeMainBody(result.getMainBody());
        for (Map.Entry<String, CompilationOutput> entry : result.getSubTemplates().entrySet()) {
            JavaClassTemplate childTemplate = newChildTemplate();
            processCompilationResult(entry.getValue(), childTemplate);
            mainTemplate.writeSubTemplate(entry.getKey(), childTemplate.toString());
        }
    }

    private JavaClassTemplate newMainTemplate() {
        return new JavaClassTemplate(mainTemplate);
    }

    private JavaClassTemplate newChildTemplate() {
        return new JavaClassTemplate(childTemplate);
    }
}
