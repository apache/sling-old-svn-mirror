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
package org.apache.sling.commons.compiler.impl;

import org.apache.sling.commons.compiler.ClassWriter;
import org.apache.sling.commons.compiler.CompileUnit;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.CompilerEnvironment;
import org.apache.sling.commons.compiler.ErrorHandler;
import org.apache.sling.commons.compiler.Options;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.CharArrayReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * The <code>EclipseJavaCompiler</code> provides platform independant Java Compilation 
 * support using the Eclipse Java Compiler (org.eclipse.jdt).
 * 
 * @scr.component metatype="no"
 * @scr.service interface="org.apache.sling.commons.compiler.JavaCompiler"
 */
public class EclipseJavaCompiler implements JavaCompiler {

    /** Logger instance */
    private static final Logger log = LoggerFactory.getLogger(JavaCompiler.class);

    // the static problem factory
    private static IProblemFactory PROBLEM_FACTORY =
        new DefaultProblemFactory(Locale.getDefault());

    public EclipseJavaCompiler() {
    }

    /**
     *
     * @param units
     * @param env
     * @param classWriter
     * @param errorHandler
     * @param options
     * @return
     */
    public boolean compile(CompileUnit[] units, CompilerEnvironment env,
            ClassWriter classWriter, ErrorHandler errorHandler,
            Options options) {

        IErrorHandlingPolicy policy =
            DefaultErrorHandlingPolicies.proceedWithAllProblems();

        // output for non-error log messages
        PrintWriter logWriter = null;

        if (options == null) {
            options = new Options();
        }
        
        HashMap props = new HashMap();
        if (options.isGenerateDebugInfo()) {
            props.put("org.eclipse.jdt.core.compiler.debug.localVariable", "generate");
            props.put("org.eclipse.jdt.core.compiler.debug.lineNumber", "generate");
            props.put("org.eclipse.jdt.core.compiler.debug.sourceFile", "generate");
        }
        String sourceVersion = options.getSourceVersion();
        if (sourceVersion != null) {
            props.put("org.eclipse.jdt.core.compiler.source", sourceVersion);
            //options.put("org.eclipse.jdt.core.compiler.compliance", sourceVersion);
            //options.put("org.eclipse.jdt.core.compiler.codegen.targetPlatform", sourceVersion);
        }
        //options.put("org.eclipse.jdt.core.encoding", "UTF8");
        CompilerOptions settings = new CompilerOptions(props);
        
        CompileContext context = new CompileContext(units, env, errorHandler, classWriter);

        if (log.isDebugEnabled()) {
            log.debug(settings.toString());
        }
        
        org.eclipse.jdt.internal.compiler.Compiler compiler =
                new org.eclipse.jdt.internal.compiler.Compiler(
                        context,
                        policy,
                        settings,
                        context,
                        PROBLEM_FACTORY,
                        logWriter);

        compiler.compile(context.sourceUnits());

        context.cleanup();
        
        return !context.hadErrors;
    }

    //--------------------------------------------------------< inner classes >

    private class CompileContext implements ICompilerRequestor, INameEnvironment {

        boolean hadErrors;
        HashMap<String,ICompilationUnit> compUnits;

        ErrorHandler errorHandler;
        ClassWriter classWriter;

        CompilerEnvironment compEnv;

        CompileContext(CompileUnit[] units,
        		CompilerEnvironment compEnv,
        		ErrorHandler errorHandler, 
        		ClassWriter classWriter) {
            
        	compUnits = new HashMap<String,ICompilationUnit>(units.length);
            for (int i = 0; i < units.length; i++) {
                CompilationUnitAdapter cua = new CompilationUnitAdapter(units[i]);
                char[][] compoundName = CharOperation.arrayConcat(cua.getPackageName(), cua.getMainTypeName());
                compUnits.put(CharOperation.toString(compoundName), new CompilationUnitAdapter(units[i]));
            }
        	
        	this.compEnv = compEnv;
        	this.errorHandler = errorHandler;
            this.classWriter = classWriter;
            hadErrors = false;
        }

        ICompilationUnit[] sourceUnits() {
        	return (ICompilationUnit[]) compUnits.values().toArray(
        			new ICompilationUnit[compUnits.size()]);
        }
        
        //---------------------------------------------------< ICompilerRequestor >
        /**
         * {@inheritDoc}
         */
        public void acceptResult(CompilationResult result) {
            if (result.hasErrors()) {
                hadErrors = true;
            }

            if (result.hasProblems()) {
                CategorizedProblem[] problems = result.getProblems();
                for (int i = 0; i < problems.length; i++) {
                    CategorizedProblem problem = problems[i];
                    String msg = problem.getMessage();
                    String fileName = CharOperation.charToString(problem.getOriginatingFileName());
                    int line = problem.getSourceLineNumber();
                    int pos = problem.getSourceStart();

                    if (problem.isError()) {
                        errorHandler.onError(msg, fileName, line, pos);
                    } else if (problem.isWarning()) {
                        errorHandler.onWarning(msg, fileName, line, pos);
                    } else {
                        log.debug("unknown problem category: " + problem.toString());
                    }
                }
            }
            ClassFile[] classFiles = result.getClassFiles();
            for (int i = 0; i < classFiles.length; i++) {
                ClassFile classFile = classFiles[i];
                String className = CharOperation.toString(classFile.getCompoundName());
                try {
                    classWriter.write(className, classFile.getBytes());
                } catch (Exception e) {
                    log.error("failed to persist class " + className, e);
                }
            }
        }

        //-------------------------------------------------< INameEnvironment >
        /**
         * {@inheritDoc}
         */
        public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
            // check 1st if type corresponds with any of current compilation units
            String fqn = CharOperation.toString(compoundTypeName);
            ICompilationUnit cu = (ICompilationUnit) compUnits.get(fqn);
            if (cu != null) {
                return new NameEnvironmentAnswer(cu, null);
            }

            // locate the class through the class loader
            try {
                byte[] bytes = compEnv.findClass(CharOperation.toString(compoundTypeName));
                if (bytes == null) {
                    return null;
                }
                ClassFileReader classFileReader =
                        new ClassFileReader(bytes, fqn.toCharArray(), true);
                return new NameEnvironmentAnswer(classFileReader, null);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
            return findType(CharOperation.arrayConcat(packageName, typeName));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isPackage(char[][] parentPackageName, char[] packageName) {
            String fqn = CharOperation.toString(
                    CharOperation.arrayConcat(parentPackageName, packageName));
            return compUnits.get(fqn) == null && compEnv.isPackage(fqn);
        }

        /**
         * {@inheritDoc}
         */
        public void cleanup() {
            compEnv.cleanup();
        }
    }
    
    private class CompilationUnitAdapter implements ICompilationUnit {

        CompileUnit compUnit;
        char[][] packageName;

        CompilationUnitAdapter(CompileUnit compUnit) {
            this.compUnit = compUnit;
        }

        String extractPackageName(char[] contents) {
            BufferedReader reader = new BufferedReader(new CharArrayReader(contents));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("package")) {
                        line = line.substring("package".length());
                        line = line.substring(0, line.lastIndexOf(';'));
                        return line.trim();
                    }
                }
            } catch (IOException e) {
                // should never get here...
            }

            // no package declaration found
            return "";
        }

        //-------------------------------------------------< ICompilationUnit >

        public char[] getContents() {
            return compUnit.getSourceFileContents();
        }

        public char[] getMainTypeName() {
            return compUnit.getMainTypeName().toCharArray();
        }

        public char[][] getPackageName() {
            if (packageName == null) {
                String s = extractPackageName(compUnit.getSourceFileContents());
                packageName = CharOperation.splitOn('.', s.toCharArray());
            }
            return packageName;
        }

        public char[] getFileName() {
            return compUnit.getSourceFileName().toCharArray();
        }
    }
}
