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
package org.apache.sling.scripting.sightly.compiler.commands;

import java.util.Stack;

import org.apache.sling.scripting.sightly.impl.compiler.visitor.UniformVisitor;

/**
 * This {@link CommandVisitor} allows storing states for {@link Command} evaluation.
 */
public final class StatefulVisitor implements CommandVisitor {

    private final Stack<CommandVisitor> stack = new Stack<CommandVisitor>();
    private CommandVisitor visitor;
    private StateControl control = new StateControl();

    public StatefulVisitor() {
        this.visitor = InvalidState.INSTANCE;
    }

    public StateControl getControl() {
        return control;
    }

    public void initializeWith(CommandVisitor initialState) {
        if (this.visitor != InvalidState.INSTANCE) {
            throw new IllegalStateException("Initial state is already set");
        }
        this.visitor = initialState;
    }

    @Override
    public void visit(Conditional.Start conditionalStart) {
        visitor.visit(conditionalStart);
    }

    @Override
    public void visit(Conditional.End conditionalEnd) {
        visitor.visit(conditionalEnd);
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        visitor.visit(variableBindingStart);
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {
        visitor.visit(variableBindingEnd);
    }

    @Override
    public void visit(VariableBinding.Global globalAssignment) {
        visitor.visit(globalAssignment);
    }

    @Override
    public void visit(OutputVariable outputVariable) {
        visitor.visit(outputVariable);
    }

    @Override
    public void visit(OutText outText) {
        visitor.visit(outText);
    }

    @Override
    public void visit(Loop.Start loopStart) {
        visitor.visit(loopStart);
    }

    @Override
    public void visit(Loop.End loopEnd) {
        visitor.visit(loopEnd);
    }

    @Override
    public void visit(Procedure.Start startProcedure) {
        visitor.visit(startProcedure);
    }

    @Override
    public void visit(Procedure.End endProcedure) {
        visitor.visit(endProcedure);
    }

    @Override
    public void visit(Procedure.Call procedureCall) {
        visitor.visit(procedureCall);
    }

    private void pushVisitor(CommandVisitor visitor) {
        stack.push(this.visitor);
        this.visitor = visitor;
    }

    private CommandVisitor popVisitor() {
        CommandVisitor top = this.visitor;
        this.visitor = this.stack.pop();
        return top;
    }

    public final class StateControl {

        public void push(CommandVisitor visitor) {
            pushVisitor(visitor);
        }

        public CommandVisitor pop() {
            return popVisitor();
        }

        public CommandVisitor replace(CommandVisitor visitor) {
            CommandVisitor current = pop();
            push(visitor);
            return current;
        }
    }

    private static final class InvalidState extends UniformVisitor {

        public static final InvalidState INSTANCE = new InvalidState();

        private InvalidState() {
        }

        @Override
        public void onCommand(Command command) {
            throw new IllegalStateException("StatefulVisitor has not been initialized");
        }
    }
}
