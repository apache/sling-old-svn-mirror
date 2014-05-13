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

/**
 * The <code>JavaCompiler</code> provides platform independant Java
 * compilation support.
 */
public interface JavaCompiler {

    /**
     * Compile the compilation units.
     * This method checks if the compilation is necessary by using
     * last modified check of the source to compile and the class
     * file (if available).
     * The compiler compiles all sources if at least one of the
     * class files is out dated!
     *
     * @param units The compilation units.
     * @param options The compilation options - this object is optional
     * @return The compilation result with more information.
     * @since 2.0
     */
    CompilationResult compile(CompilationUnit[] units,
                              Options options);
}
