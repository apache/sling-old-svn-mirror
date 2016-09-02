/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler;

import java.io.Reader;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class describes a compilation unit that the {@link SightlyCompiler} will process during the call of the {@code compile} methods.
 */
@ProviderType
public interface CompilationUnit {

    /**
     * Returns the name of the script that will be compiled.
     *
     * @return the script name
     */
    String getScriptName();

    /**
     * Provides the {@link Reader} from which the compiler will read the script to compile.
     *
     * @return the reader
     */
    Reader getScriptReader();

}
