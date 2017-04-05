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
package org.apache.sling.scripting.sightly.compiler.commands;

/**
 * This class can be extended by {@link CommandVisitor} implementations that don't provide support for all the available {@link Command}s.
 */
public abstract class AbstractCommandVisitor implements CommandVisitor {

    @Override
    public void visit(Conditional.Start conditionalStart) {

    }

    @Override
    public void visit(Conditional.End conditionalEnd) {

    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {

    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {

    }

    @Override
    public void visit(VariableBinding.Global globalAssignment) {

    }

    @Override
    public void visit(OutputVariable outputVariable) {

    }

    @Override
    public void visit(OutText outText) {

    }

    @Override
    public void visit(Loop.Start loopStart) {

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

    }
}
