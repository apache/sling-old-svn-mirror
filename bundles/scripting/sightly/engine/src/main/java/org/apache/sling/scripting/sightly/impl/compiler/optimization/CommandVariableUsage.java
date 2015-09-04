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

package org.apache.sling.scripting.sightly.impl.compiler.optimization;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.ris.Command;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Loop;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutText;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Procedure;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;

/**
 * Extracts the variables used in a command
 */
public class CommandVariableUsage implements CommandVisitor {

    private ArrayList<String> variables = new ArrayList<String>();

    public static List<String> extractVariables(Command command) {
        CommandVariableUsage cvu = new CommandVariableUsage();
        command.accept(cvu);
        return cvu.variables;
    }

    @Override
    public void visit(Conditional.Start conditionalStart) {
        variables.add(conditionalStart.getVariable());
    }

    @Override
    public void visit(Conditional.End conditionalEnd) {
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        addFromExpression(variableBindingStart.getExpression());
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {
    }

    @Override
    public void visit(VariableBinding.Global globalAssignment) {
        addFromExpression(globalAssignment.getExpression());
    }

    private void addFromExpression(ExpressionNode node) {
        variables.addAll(VariableFinder.findVariables(node));
    }

    @Override
    public void visit(OutVariable outVariable) {
        variables.add(outVariable.getVariableName());
    }

    @Override
    public void visit(OutText outText) {
    }

    @Override
    public void visit(Loop.Start loopStart) {
        variables.add(loopStart.getListVariable());
    }

    @Override
    public void visit(Loop.End loopEnd) {
    }

    @Override
    public void visit(Procedure.Start startProcedure) {
    }

    @Override
    public void visit(Procedure.End endProcedure) {
    }

    @Override
    public void visit(Procedure.Call procedureCall) {
        variables.add(procedureCall.getTemplateVariable());
        variables.add(procedureCall.getArgumentsVariable());
    }
}
