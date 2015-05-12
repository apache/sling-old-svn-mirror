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
package org.apache.sling.scripting.sightly.impl.compiler.ris;

import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Loop;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutText;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Procedure;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;

/**
 * Processor of rendering commands
 */
public interface CommandVisitor {

    void visit(Conditional.Start conditionalStart);

    void visit(Conditional.End conditionalEnd);

    void visit(VariableBinding.Start variableBindingStart);

    void visit(VariableBinding.End variableBindingEnd);

    void visit(VariableBinding.Global globalAssignment);

    void visit(OutVariable outVariable);

    void visit(OutText outText);

    void visit(Loop.Start loopStart);

    void visit(Loop.End loopEnd);

    void visit(Procedure.Start startProcedure);

    void visit(Procedure.End endProcedure);

    void visit(Procedure.Call procedureCall);
}
