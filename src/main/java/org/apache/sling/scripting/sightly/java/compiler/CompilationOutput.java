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

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The {@code CompilationOutput} encapsulates the result of a compile operation as processed by the {@link JavaClassBackendCompiler}.
 */
@ProviderType
public interface CompilationOutput {

    /**
     * Provides the generated class' main body section.
     *
     * @return the generated class' main body section
     */
    String getMainBody();

    /**
     * Provides the sub-templates ({@code data-sly-template}) code sections.
     *
     * @return the sub-templates ({@code data-sly-template}) code sections
     */
    Map<String, CompilationOutput> getSubTemplates();
}
