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
package org.apache.sling.scripting.sightly.compiler.java.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;


public class CharSequenceJavaCompilerException extends Exception {

    private Set<String> classNames;
    transient private DiagnosticCollector<JavaFileObject> diagnostics;

    public CharSequenceJavaCompilerException(String message,
                                         Set<String> qualifiedClassNames,
                                         DiagnosticCollector<JavaFileObject> diagnostics) {
        super(message);
        setClassNames(qualifiedClassNames);
        setDiagnostics(diagnostics);
    }

    public CharSequenceJavaCompilerException(Set<String> qualifiedClassNames,
                                         Throwable cause, DiagnosticCollector<JavaFileObject> diagnostics) {
        super(cause);
        setClassNames(qualifiedClassNames);
        setDiagnostics(diagnostics);
    }

    private void setClassNames(Set<String> qualifiedClassNames) {
        classNames = new HashSet<>(qualifiedClassNames);
    }

    private void setDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        this.diagnostics = diagnostics;
    }

    /**
     * Gets the diagnostics collected by this exception.
     *
     * @return this exception's diagnostics
     */
    public DiagnosticCollector<JavaFileObject> getDiagnostics() {
        return diagnostics;
    }

    /**
     * @return The name of the classes whose compilation caused the compile
     *         exception
     */
    public Collection<String> getClassNames() {
        return Collections.unmodifiableSet(classNames);
    }
}

