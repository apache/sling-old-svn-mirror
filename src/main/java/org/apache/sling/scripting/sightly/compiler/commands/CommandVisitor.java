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

import aQute.bnd.annotation.ConsumerType;

/**
 * The {@code CommandVisitor} is the mechanism through which a {@link Command} can be processed.
 */
@ConsumerType
public interface CommandVisitor {

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.Conditional.Start} command.
     *
     * @param conditionalStart the command
     */
    void visit(Conditional.Start conditionalStart);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.Conditional.End} command.
     *
     * @param conditionalEnd the command
     */
    void visit(Conditional.End conditionalEnd);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.VariableBinding.Start} command.
     *
     * @param variableBindingStart the command
     */
    void visit(VariableBinding.Start variableBindingStart);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.VariableBinding.End} command.
     *
     * @param variableBindingEnd the command
     */
    void visit(VariableBinding.End variableBindingEnd);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.VariableBinding.Global} command.
     *
     * @param globalAssignment the command
     */
    void visit(VariableBinding.Global globalAssignment);

    /**
     * Process a {@link OutputVariable} command.
     *
     * @param outputVariable the command
     */
    void visit(OutputVariable outputVariable);

    /**
     * Process a {@link OutText} command.
     *
     * @param outText the command
     */
    void visit(OutText outText);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.Loop.Start} command.
     *
     * @param loopStart the command
     */
    void visit(Loop.Start loopStart);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.Loop.End} command.
     *
     * @param loopEnd the command
     */
    void visit(Loop.End loopEnd);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.Procedure.Start} command.
     *
     * @param startProcedure the command
     */
    void visit(Procedure.Start startProcedure);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.Procedure.End} command.
     *
     * @param endProcedure the command
     */
    void visit(Procedure.End endProcedure);

    /**
     * Process a {@link org.apache.sling.scripting.sightly.compiler.commands.Procedure.Call} command.
     *
     * @param procedureCall the command
     */
    void visit(Procedure.Call procedureCall);
}
