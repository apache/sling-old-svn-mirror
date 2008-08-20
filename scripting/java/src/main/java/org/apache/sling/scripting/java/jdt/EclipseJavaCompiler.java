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
package org.apache.sling.scripting.java.jdt;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.sling.scripting.java.CompilationContext;
import org.apache.sling.scripting.java.CompilerError;
import org.apache.sling.scripting.java.Options;
import org.apache.sling.scripting.java.SlingIOProvider;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

/**
 * Eclipse Java Compiler
 *
 * @version $Id$
 */
public class EclipseJavaCompiler {

    /** The io provider. */
    private final SlingIOProvider ioProvider;

    /** The compiler options. */
    private final Options compilerOptions;

    /** The compilation context. */
    private final CompilationContext context;

    /**
     * Construct a new java compiler.
     * @param context
     */
    public EclipseJavaCompiler(final CompilationContext context) {
        this.ioProvider = context.getIOProvider();
        this.compilerOptions = context.getCompilerOptions();
        this.context = context;
    }

    private CompilerOptions getCompilerOptions() {
        CompilerOptions options = new CompilerOptions();
        final Map<String, String> settings = new HashMap<String, String>();
        settings.put(CompilerOptions.OPTION_LineNumberAttribute,
                CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_SourceFileAttribute,
                CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_ReportDeprecation,
                CompilerOptions.IGNORE);
        settings.put(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE);
        settings.put(CompilerOptions.OPTION_Encoding, this.compilerOptions.getJavaEncoding());
        if (this.compilerOptions.getClassDebugInfo()) {
            settings.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
        }
        if ( this.compilerOptions.getCompilerSourceVM().equals("1.6") ) {
            settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_6);
        } else {
            settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
        }
        if ( this.compilerOptions.getCompilerTargetVM().equals("1.6") ) {
            settings.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_6);
            settings.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
        } else {
            settings.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_5);
            settings.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
        }

        options.set(settings);
        return options;
    }

    /**
     * Compile the java class.
     * @return null if no error occured, a list of errors otherwise.
     * @throws IOException
     */
    public List<CompilerError> compile() throws IOException {
        final IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();
        final IProblemFactory problemFactory = new DefaultProblemFactory(Locale.getDefault());
        final CompilationUnit unit = new CompilationUnit(this.context.getSourcePath(),
                 this.context.getJavaClassName(),
                 this.context.getCompilerOptions(),
                 this.ioProvider);

        final Compiler compiler = new Compiler(unit,
                                         policy,
                                         getCompilerOptions(),
                                         unit,
                                         problemFactory);
        compiler.compile(new CompilationUnit[] {unit});
        if ( unit.getErrors().size() == 0 ) {
            return null;
        }
        return unit.getErrors();
    }

}
