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
package org.apache.sling.scripting.sightly.compiler.visitor;

import org.apache.sling.scripting.sightly.compiler.api.ris.Command;
import org.apache.sling.scripting.sightly.compiler.api.ris.CommandVisitor;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.BufferControl;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.Conditional;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.Loop;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.OutText;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.Procedure;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.compiler.api.ris.Command;
import org.apache.sling.scripting.sightly.compiler.api.ris.CommandVisitor;

/**
 * Do not dispatch for different command types
 */
public abstract class UniformVisitor implements CommandVisitor {

    protected abstract void onCommand(Command command);

    @Override
    public void visit(Conditional.Start conditionalStart) {
        onCommand(conditionalStart);
    }

    @Override
    public void visit(Conditional.End conditionalEnd) {
        onCommand(conditionalEnd);
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        onCommand(variableBindingStart);
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {
        onCommand(variableBindingEnd);
    }

    @Override
    public void visit(VariableBinding.Global globalAssignment) {
        onCommand(globalAssignment);
    }

    @Override
    public void visit(OutVariable outVariable) {
        onCommand(outVariable);
    }

    @Override
    public void visit(OutText outText) {
        onCommand(outText);
    }

    @Override
    public void visit(Loop.Start loopStart) {
        onCommand(loopStart);
    }

    @Override
    public void visit(Loop.End loopEnd) {
        onCommand(loopEnd);
    }

    @Override
    public void visit(BufferControl.Push bufferPush) {
        onCommand(bufferPush);
    }

    @Override
    public void visit(BufferControl.Pop bufferPop) {
        onCommand(bufferPop);
    }

    @Override
    public void visit(Procedure.Start startProcedure) {
        onCommand(startProcedure);
    }

    @Override
    public void visit(Procedure.End endProcedure) {
        onCommand(endProcedure);
    }

    @Override
    public void visit(Procedure.Call procedureCall) {
        onCommand(procedureCall);
    }
}
