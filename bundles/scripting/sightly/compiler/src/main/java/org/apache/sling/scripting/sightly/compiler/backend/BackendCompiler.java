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
package org.apache.sling.scripting.sightly.compiler.backend;

import org.apache.sling.scripting.sightly.compiler.CompilationUnit;
import org.apache.sling.scripting.sightly.compiler.SightlyCompiler;
import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.compiler.commands.CommandVisitor;

import aQute.bnd.annotation.ConsumerType;

/**
 * <p>
 *     A {@link BackendCompiler} can be hooked in into the {@link SightlyCompiler} in order to transpile Sightly {@link Command}s into other
 *     JVM supported languages. The transpilation can be performed with the help of specific {@link CommandVisitor} implementations that are
 *     attached to the {@link Command}s from the {@link CommandStream}.
 * </p>
 * <p>
 *     For more details see {@link SightlyCompiler#compile(CompilationUnit, BackendCompiler)}.
 * </p>
 */
@ConsumerType
public interface BackendCompiler {

    /**
     * Process a stream of commands
     *
     * @param stream the stream of commands
     */
    void handle(CommandStream stream);

}
