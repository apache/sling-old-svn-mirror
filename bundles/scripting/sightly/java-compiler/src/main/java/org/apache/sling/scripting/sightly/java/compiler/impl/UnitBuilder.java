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

package org.apache.sling.scripting.sightly.java.compiler.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.scripting.sightly.java.compiler.CompilationOutput;

/**
 * Builder for compiled sources
 */
public class UnitBuilder {

    private final JavaSource source = new JavaSource();
    private final Set<String> parameters;
    private final Map<String, UnitBuilder> subTemplates = new HashMap<String, UnitBuilder>();

    public UnitBuilder() {
        this(Collections.<String>emptySet());
    }

    public UnitBuilder(Set<String> parameters) {
        this.parameters = parameters;
    }

    public UnitBuilder newSubBuilder(String name, Set<String> parameters) {
        UnitBuilder unitBuilder = new UnitBuilder(parameters);
        subTemplates.put(name, unitBuilder);
        return unitBuilder;
    }

    public JavaSource getSource() {
        return source;
    }

    public Set<String> getParameters() {
        return parameters;
    }

    public CompilationOutput build() {
        Map<String, CompilationOutput> map = new HashMap<>();
        for (Map.Entry<String, UnitBuilder> entry : subTemplates.entrySet()) {
            map.put(entry.getKey(), entry.getValue().build());
        }
        return new CompilationOutputImpl(source.toString(), map);
    }
}
