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
package org.apache.sling.commons.compiler;

import java.util.List;

/**
 * The compilation result allows clients of the java compiler
 * to check for error messages, warnings (if not disabled by
 * the options) and allows to access the compiled classes.
 * @since 2.0
 */
public interface CompilationResult {

    /**
     * Return a list of error messages that occured during
     * compilation. If no errors occured <code>null</code>
     * is returned.
     * @return A list of error messages or <code>null</code>.
     */
    List<CompilerMessage> getErrors();

    /**
     * Return a list of warnings that occured during
     * compilation. If no warnings occured <code>null</code>
     * is returned.
     * @return A list of warnings or <code>null</code>.
     */
    List<CompilerMessage> getWarnings();

    /**
     * Was a compilation required or were all classes recent?
     * @return <code>true>/code> if classes were compiled.
     */
    boolean didCompile();

    /**
     * Try to load the compiled class.
     * The class loading might fail if the className is not
     * one of the compiled sources, if the compilation failed
     * or if a class loader writer has been used in combination
     * with a class loader that is not able to load the classes
     * written by the class loader writer.
     * @return The compiled class
     * @throws ClassNotFoundException If the class could not be found
     *         or compilation failed.
     */
    Class<?> loadCompiledClass(final String className)
    throws ClassNotFoundException;
}
