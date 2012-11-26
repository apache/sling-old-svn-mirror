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
package org.apache.sling.jcr.compiler;

import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.Options;

/**
 * The <code>JcrJavaCompiler</code> compiles Java source code stored in the
 * repository and writes the generated class files by using the class loader writer.
 */
public interface JcrJavaCompiler {

    /**
     * Compile source from the repository.
     * @param srcFiles The array of path in the repository pointing to the source
     * @param outputDir - Not supported anymore - the classes are written using the class loader writer
     * @param options - Optional options
     * @since 2.0
     * @deprecated
     */
    @Deprecated
    CompilationResult compile(String[] srcFiles,
                              String   outputDir,
                              Options options)
    throws Exception;

    /**
     * Compile source from the repository.
     * @param srcFiles The array of path in the repository pointing to the source
     * @param options - Optional options
     * @since 2.1
     */
    CompilationResult compile(String[] srcFiles,
                              Options options)
    throws Exception;
}
