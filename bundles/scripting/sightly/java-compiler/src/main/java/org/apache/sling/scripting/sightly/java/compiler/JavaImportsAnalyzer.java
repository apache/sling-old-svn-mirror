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
package org.apache.sling.scripting.sightly.java.compiler;

/**
 * The {@code JavaImportsAnalyzer} allows checking imports in generated HTL Java classes, in order to optimise dependencies.
 */
public interface JavaImportsAnalyzer {

    /**
     * Analyses the provided {@code importedClass} and decides if this class should be an explicit import or not in the generated HTL
     * Java class.
     *
     * @param importedClass the import to analyse
     * @return {@code true} if the import should be declared, {@code false} otherwise
     */
    default boolean allowImport(String importedClass) {
        return true;
    }

}
