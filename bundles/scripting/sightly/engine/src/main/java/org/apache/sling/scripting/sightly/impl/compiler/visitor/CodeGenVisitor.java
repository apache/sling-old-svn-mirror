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

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.sling.scripting.sightly.impl.compiled.ExpressionTranslator;
import org.apache.sling.scripting.sightly.impl.compiled.JavaSource;
import org.apache.sling.scripting.sightly.impl.compiled.SourceGenConstants;
import org.apache.sling.scripting.sightly.impl.compiled.Type;
import org.apache.sling.scripting.sightly.impl.compiled.TypeInference;
import org.apache.sling.scripting.sightly.impl.compiled.TypeInfo;
import org.apache.sling.scripting.sightly.impl.compiled.UnitBuilder;
import org.apache.sling.scripting.sightly.impl.compiled.VariableAnalyzer;
import org.apache.sling.scripting.sightly.impl.compiled.VariableDescriptor;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Loop;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutText;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Procedure;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.utils.RenderUtils;

/**
 * Java code generator
 */
public class CodeGenVisitor implements CommandVisitor {

    private final JavaSource source;
    private final UnitBuilder unitBuilder;
    private final Stack<String> loopStatusStack = new Stack<String>();
    private final VariableAnalyzer analyzer = new VariableAnalyzer();
    private final StatefulVisitor.StateControl control;
    private final Set<String> unitParameters;

    public CodeGenVisitor(UnitBuilder unitBuilder, StatefulVisitor.StateControl control) {
        this.unitBuilder = unitBuilder;
        this.source = unitBuilder.getSource();
        this.control = control;
        this.unitParameters = new HashSet<String>();
        for (String param : unitBuilder.getParameters()) {
            this.unitParameters.add(param.toLowerCase());
        }
    }

    /**
     * Complete building the source
     */
    public void finish() {
        source.prepend(initializations());
    }

    private String initializations() {
        JavaSource initSource = new JavaSource();
        for (VariableDescriptor descriptor : analyzer.allVariables()) {
            initVariable(descriptor, initSource);
        }
        return initSource.toString();
    }

    private void initVariable(VariableDescriptor descriptor, JavaSource initSource) {
        switch (descriptor.getScope()) {
            case DYNAMIC:
                initSource.beginAssignment(descriptor.getAssignedName());
                if (descriptor.isTemplateVariable()) {
                    initSource.startCall(SourceGenConstants.RECORD_GET_VALUE);
                } else if (unitParameters.contains(descriptor.getOriginalName().toLowerCase())) {
                    initSource.startMethodCall(SourceGenConstants.ARGUMENTS_FIELD, SourceGenConstants.BINDINGS_GET_METHOD);
                } else {
                    initSource.startMethodCall(SourceGenConstants.BINDINGS_FIELD, SourceGenConstants.BINDINGS_GET_METHOD);
                }
                initSource.stringLiteral(descriptor.getOriginalName())
                        .endCall()
                        .endStatement();
                break;
            case GLOBAL:
                initSource.beginAssignment(descriptor.getAssignedName())
                        .nullLiteral()
                        .endStatement();
                break;
        }
        String listCoercionVar = descriptor.getListCoercion();
        if (listCoercionVar != null) {
            //need to initialize the list coercion to null
            initSource.beginAssignment(listCoercionVar, SourceGenConstants.COLLECTION_TYPE)
                    .nullLiteral().endStatement();
        }
    }

    @Override
    public void visit(Conditional.Start conditional) {
        VariableDescriptor descriptor = analyzer.descriptor(conditional.getVariable());
        boolean negate = !conditional.getExpectedTruthValue();
        source.beginIf();
        if (negate) {
            source.negation();
        }
        if (descriptor.getType() == Type.BOOLEAN) {
            source.append(descriptor.getAssignedName());
        } else {
            source.startMethodCall(SourceGenConstants.RENDER_UTILS, RenderUtils.BOOLEAN_COERCE)
                    .append(descriptor.getAssignedName())
                    .endCall();
        }
        source.completeIf();
    }

    @Override
    public void visit(Conditional.End conditionalEnd) {
        source.endIf();
    }

    @Override
    public void visit(VariableBinding.Start variableBinding) {
        source.startBlock();
        TypeInfo typeInfo = TypeInference.inferTypes(variableBinding.getExpression(), analyzer);
        Type type = typeInfo.typeOf(variableBinding.getExpression());
        String properName = declare(variableBinding.getVariableName(), type);
        source.beginAssignment(properName, type.getNativeClass());
        ExpressionTranslator.buildExpression(
                variableBinding.getExpression(),
                source,
                analyzer,
                typeInfo);
        source.endStatement();
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {
        VariableDescriptor descriptor = analyzer.endVariable();
        String listCoercionVar = descriptor.getListCoercion();
        if (listCoercionVar != null) {
            //this variable was coerced to list at some point
            generateCoercionClearing(listCoercionVar);
        }
        source.endBlock();
    }

    @Override
    public void visit(VariableBinding.Global globalAssignment) {
        TypeInfo typeInfo = TypeInference.inferTypes(globalAssignment.getExpression(), analyzer);
        VariableDescriptor descriptor = analyzer.declareGlobal(globalAssignment.getVariableName());
        String name = descriptor.getAssignedName();
        source.append(name).assign();
        ExpressionTranslator.buildExpression(
                globalAssignment.getExpression(),
                source,
                analyzer,
                typeInfo);
        source.endStatement();
        String listCoercionVar = descriptor.getListCoercion();
        if (listCoercionVar != null) {
            //variable was used for list coercion. Generating a coercion clearing
            generateCoercionClearing(listCoercionVar);
        }
    }

    @Override
    public void visit(OutVariable outVariable) {
        String variable = analyzer.assignedName(outVariable.getVariableName());
        source.startStatement()
                .startMethodCall(SourceGenConstants.OUT_BUFFER, SourceGenConstants.WRITE_METHOD)
                .startMethodCall(SourceGenConstants.RENDER_UTILS, RenderUtils.STRING_COERCE)
                .append(variable)
                .endCall()
                .endCall()
                .endStatement();
    }

    @Override
    public void visit(OutText outText) {
        source.startStatement()
                .startMethodCall(SourceGenConstants.OUT_BUFFER, SourceGenConstants.WRITE_METHOD)
                .stringLiteral(outText.getText())
                .endCall()
                .endStatement();
    }

    @Override
    public void visit(Loop.Start loop) {
        VariableDescriptor descriptor = analyzer.descriptor(loop.getListVariable());
        String listVariable = descriptor.getAssignedName();
        String collectionVar = descriptor.requireListCoercion();
        source.beginIf().append(collectionVar).equality().nullLiteral().completeIf()
                .startStatement()
                .append(collectionVar)
                .assign()
                .startMethodCall(SourceGenConstants.RENDER_UTILS, RenderUtils.COLLECTION_COERCE)
                .append(listVariable)
                .endCall()
                .endStatement()
                .endIf();
        String indexVar = declare(loop.getIndexVariable(), Type.LONG);
        source.beginAssignment(indexVar, Type.LONG.getNativeClass()).number(0).endStatement();
        String itemVar = declare(loop.getItemVariable(), Type.UNKNOWN);
        source.beginFor(itemVar, collectionVar);
        loopStatusStack.push(indexVar);
    }

    @Override
    public void visit(Loop.End loopEnd) {
        String indexVar = loopStatusStack.pop();
        source.startStatement().append(indexVar).increment().endStatement();
        source.endFor();
        analyzer.endVariable();
        analyzer.endVariable();
    }

    @Override
    public void visit(Procedure.Start startProcedure) {
        UnitBuilder subTemplateUnit = unitBuilder.newSubBuilder(startProcedure.getName(), startProcedure.getParameters());
        analyzer.declareTemplate(startProcedure.getName());
        control.push(new CodeGenVisitor(subTemplateUnit, control));
    }

    @Override
    public void visit(Procedure.End endProcedure) {
        CodeGenVisitor previous = (CodeGenVisitor) control.pop();
        previous.finish();
    }

    @Override
    public void visit(Procedure.Call procedureCall) {
        String templateVar = analyzer.assignedName(procedureCall.getTemplateVariable());
        String argVar = analyzer.assignedName(procedureCall.getArgumentsVariable());
        source.startStatement()
                .startCall(SourceGenConstants.CALL_UNIT_METHOD, false)
                .append(SourceGenConstants.RENDER_CONTEXT_INSTANCE)
                .separateArgument()
                .append(templateVar)
                .separateArgument()
                .append(argVar)
                .endCall()
                .endStatement();
    }

    private String declare(String originalName, Type type) {
        return analyzer.declareVariable(originalName, type).getAssignedName();
    }

    private void generateCoercionClearing(String coercionVariableName) {
        source.startStatement().append(coercionVariableName).assign().nullLiteral().endStatement();
    }
}
