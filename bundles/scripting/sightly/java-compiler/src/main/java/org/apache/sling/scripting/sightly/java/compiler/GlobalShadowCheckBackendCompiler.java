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

import java.util.Set;

import org.apache.sling.scripting.sightly.compiler.backend.BackendCompiler;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.java.compiler.impl.CommandVisitorHandler;
import org.apache.sling.scripting.sightly.java.compiler.impl.GlobalShadowChecker;

/**
 * Wrapping {@link BackendCompiler} that checks for global bindings shadowing.
 */
public final class GlobalShadowCheckBackendCompiler implements BackendCompiler {

    private final BackendCompiler baseBackend;
    private final Set<String> globals;

    public GlobalShadowCheckBackendCompiler(BackendCompiler baseBackend, Set<String> globals) {
        this.baseBackend = baseBackend;
        this.globals = globals;
    }

    @Override
    public void handle(CommandStream stream) {
        stream.addHandler(new CommandVisitorHandler(new GlobalShadowChecker(globals)));
        baseBackend.handle(stream);
    }

    public CompilationOutput build(ClassInfo classInfo) {
        return null;
    }
}
