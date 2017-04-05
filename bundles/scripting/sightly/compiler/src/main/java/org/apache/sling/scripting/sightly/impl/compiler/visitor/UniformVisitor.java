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
package org.apache.sling.scripting.sightly.impl.compiler.visitor;

import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandVisitor;
import org.apache.sling.scripting.sightly.compiler.commands.Conditional;
import org.apache.sling.scripting.sightly.compiler.commands.Loop;
import org.apache.sling.scripting.sightly.compiler.commands.OutText;
import org.apache.sling.scripting.sightly.compiler.commands.OutputVariable;
import org.apache.sling.scripting.sightly.compiler.commands.Procedure;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;

/**
 * Abstract visitor that allows to skip processing some commands.
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
    public void visit(OutputVariable outputVariable) {
        onCommand(outputVariable);
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
