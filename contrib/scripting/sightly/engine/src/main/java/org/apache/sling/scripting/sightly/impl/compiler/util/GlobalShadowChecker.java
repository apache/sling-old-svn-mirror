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

package org.apache.sling.scripting.sightly.impl.compiler.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Loop;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutText;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Procedure;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks whether variable declarations shadow global bindings
 */
public class GlobalShadowChecker implements CommandVisitor {

    private static final Logger log = LoggerFactory.getLogger(GlobalShadowChecker.class);
    private final Map<String, String> globals;

    public GlobalShadowChecker(Set<String> globals) {
        this.globals = new HashMap<String, String>();
        for (String global : globals) {
            this.globals.put(global.toLowerCase(), global);
        }
    }

    @Override
    public void visit(Conditional.Start conditionalStart) {
    }

    @Override
    public void visit(Conditional.End conditionalEnd) {
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        checkVariable(variableBindingStart.getVariableName());
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {

    }

    @Override
    public void visit(VariableBinding.Global globalAssignment) {
        checkVariable(globalAssignment.getVariableName());
    }

    @Override
    public void visit(OutVariable outVariable) {

    }

    @Override
    public void visit(OutText outText) {

    }

    @Override
    public void visit(Loop.Start loopStart) {
        checkVariable(loopStart.getItemVariable());
        checkVariable(loopStart.getIndexVariable());
    }

    @Override
    public void visit(Loop.End loopEnd) {
    }

    @Override
    public void visit(Procedure.Start startProcedure) {
        checkVariable(startProcedure.getName());
    }

    @Override
    public void visit(Procedure.End endProcedure) {
    }

    @Override
    public void visit(Procedure.Call procedureCall) {
    }

    private void checkVariable(String variableName) {
        variableName = variableName.toLowerCase();
        if (globals.containsKey(variableName)) {
            String originalName = globals.get(variableName);
            log.warn("Global variable '{}' is being overridden in template", originalName);
        }
    }
}
