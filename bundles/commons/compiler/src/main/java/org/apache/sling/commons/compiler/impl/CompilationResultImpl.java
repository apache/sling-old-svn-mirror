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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilerMessage;

/**
 * Implementation of the compilation result
 */
public class CompilationResultImpl implements CompilationResult {

    private List<CompilerMessage> errors;

    private List<CompilerMessage> warnings;

    private final boolean ignoreWarnings;

    private final boolean compilationRequired;

    private final ClassLoaderWriter classLoaderWriter;

    public CompilationResultImpl(final String errorMessage) {
        this.ignoreWarnings = true;
        this.classLoaderWriter = null;
        this.compilationRequired = false;
        this.onError(errorMessage, "<General>", 0, 0);
    }

    public CompilationResultImpl(final ClassLoaderWriter writer) {
        this.ignoreWarnings = true;
        this.classLoaderWriter = writer;
        this.compilationRequired = false;
    }

    public CompilationResultImpl(final boolean ignoreWarnings,
                                 final ClassLoaderWriter writer) {
        this.ignoreWarnings = ignoreWarnings;
        this.classLoaderWriter = writer;
        this.compilationRequired = true;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilationResult#getErrors()
     */
    public List<CompilerMessage> getErrors() {
        return this.errors;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilationResult#getWarnings()
     */
    public List<CompilerMessage> getWarnings() {
        return this.warnings;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilationResult#loadCompiledClass(java.lang.String)
     */
    public Class<?> loadCompiledClass(final String className)
    throws ClassNotFoundException {
        if ( errors != null ) {
            throw new ClassNotFoundException(className);
        }
        return this.classLoaderWriter.getClassLoader().loadClass(className);
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilationResult#didCompile()
     */
    public boolean didCompile() {
        return this.compilationRequired;
    }

    /**
     * Notification of an error
     */
    public void onError(String msg, String sourceFile, int line, int position) {
        if ( errors == null ) {
            errors = new ArrayList<CompilerMessage>();
        }
        errors.add(new CompilerMessage(sourceFile, line, position, msg));
    }

    /**
     * Notification of a warning
     */
    public void onWarning(String msg, String sourceFile, int line, int position) {
        if ( !this.ignoreWarnings ) {
            if ( warnings == null ) {
                warnings = new ArrayList<CompilerMessage>();
            }
            warnings.add(new CompilerMessage(sourceFile, line, position, msg));
        }
    }
}
