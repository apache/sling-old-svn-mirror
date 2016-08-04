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
package org.apache.sling.scripting.sightly.impl.compiler.debug;

import java.util.Stack;

import org.apache.sling.scripting.sightly.compiler.commands.AbstractCommandVisitor;
import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandHandler;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.compiler.commands.Conditional;
import org.apache.sling.scripting.sightly.compiler.commands.Loop;
import org.apache.sling.scripting.sightly.compiler.commands.Procedure;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;

/**
 * A visitor which checks a stream for correct closing of things
 */
public final class SanityChecker extends AbstractCommandVisitor implements CommandHandler {

    private enum NestedType {
        CONDITIONAL, VARIABLE_BIND, LOOP
    }

    private final Stack<NestedType> stack = new Stack<>();
    private boolean inProcedure;

    private SanityChecker() {
    }

    public static void attachChecker(CommandStream commandStream) {
        commandStream.addHandler(new SanityChecker());
    }

    @Override
    public void onEmit(Command command) {
        command.accept(this);
    }

    @Override
    public void onError(String errorMessage) {
        throw new RuntimeException(errorMessage);
    }

    @Override
    public void onDone() {
        if (!stack.isEmpty()) {
            throw new IllegalStateException("Unclosed commands left");
        }
    }

    @Override
    public void visit(Conditional.Start conditionalStart) {
        stack.push(NestedType.CONDITIONAL);
    }

    @Override
    public void visit(Conditional.End conditionalEnd) {
        popCheck(NestedType.CONDITIONAL);
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        stack.push(NestedType.VARIABLE_BIND);
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {
        popCheck(NestedType.VARIABLE_BIND);
    }

    @Override
    public void visit(Loop.Start loopStart) {
        stack.push(NestedType.LOOP);
    }

    @Override
    public void visit(Loop.End loopEnd) {
        popCheck(NestedType.LOOP);
    }

    @Override
    public void visit(Procedure.Start startProcedure) {
        if (inProcedure) {
            throw new IllegalStateException("Cannot have nested procedures: " + startProcedure.getName());
        }
        inProcedure = true;
    }

    @Override
    public void visit(Procedure.End endProcedure) {
        if (!inProcedure) {
            throw new IllegalStateException("Procedure closing is unmatched");
        }
        inProcedure = false;
    }

    private void popCheck(NestedType nestedType) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Unbalanced command: " + nestedType);
        }
        NestedType top = stack.pop();
        if (top != nestedType) {
            throw new IllegalStateException("Command closing is unmatched. Expected " + top + ", actual: " + nestedType);
        }
    }

}
