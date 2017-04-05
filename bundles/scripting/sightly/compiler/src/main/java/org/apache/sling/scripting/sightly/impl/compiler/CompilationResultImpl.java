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
package org.apache.sling.scripting.sightly.impl.compiler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.scripting.sightly.compiler.CompilationResult;
import org.apache.sling.scripting.sightly.compiler.CompilerMessage;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;

public class CompilationResultImpl implements CompilationResult {

    private CommandStream commandStream;
    private List<CompilerMessage> warnings = new LinkedList<>();
    private List<CompilerMessage> errors = new LinkedList<>();

    public CompilationResultImpl(CommandStream commandStream) {
        this.commandStream = commandStream;
    }

    @Override
    public CommandStream getCommandStream() {
        return commandStream;
    }

    @Override
    public List<CompilerMessage> getWarnings() {
        return warnings;
    }

    @Override
    public List<CompilerMessage> getErrors() {
        return errors;
    }

    public void seal() {
        warnings = Collections.unmodifiableList(warnings);
        errors = Collections.unmodifiableList(errors);
    }
}
