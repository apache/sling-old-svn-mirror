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

package org.apache.sling.scripting.sightly.impl.compiler.util;

import java.util.Set;

import org.apache.sling.scripting.sightly.impl.compiler.CompilerBackend;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandStream;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.VisitorHandler;

/**
 * Wrapping backend that checks for global bindings shadowing
 */
public class GlobalShadowCheckBackend implements CompilerBackend {

    private final CompilerBackend baseBackend;
    private final Set<String> globals;

    public GlobalShadowCheckBackend(CompilerBackend baseBackend, Set<String> globals) {
        this.baseBackend = baseBackend;
        this.globals = globals;
    }

    @Override
    public void handle(CommandStream stream) {
        stream.addHandler(new VisitorHandler(new GlobalShadowChecker(globals)));
        baseBackend.handle(stream);
    }
}
