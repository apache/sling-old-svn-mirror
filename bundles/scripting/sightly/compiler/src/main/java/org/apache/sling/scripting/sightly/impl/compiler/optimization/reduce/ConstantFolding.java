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
package org.apache.sling.scripting.sightly.impl.compiler.optimization.reduce;

import java.util.Collection;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.StreamTransformer;
import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.EmitterVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.Streams;
import org.apache.sling.scripting.sightly.impl.compiler.visitor.TrackingVisitor;

/**
 * Optimization which evaluates constant expressions during compilation-time
 */
public final class ConstantFolding extends TrackingVisitor<EvalResult> implements EmitterVisitor {

    private final PushStream outStream = new PushStream();

    private ConstantFolding() {
    }

    public static StreamTransformer transformer() {
        return new StreamTransformer() {
            @Override
            public CommandStream transform(CommandStream inStream) {
                return Streams.map(inStream, new ConstantFolding());
            }
        };
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        String variable = variableBindingStart.getVariableName();
        ExpressionNode node = variableBindingStart.getExpression();
        EvalResult result = ExpressionReducer.reduce(node, tracker);
        result = avoidFoldingDataStructures(result);
        tracker.pushVariable(variable, result);
        outStream.write(new VariableBinding.Start(variable, result.getNode()));
    }

    private EvalResult avoidFoldingDataStructures(EvalResult evalResult) {
        //this prevents us from replacing variables that are bound to maps & collections
        //in expressions since that would mean we rebuild the same constant data structures
        //each time
        if (evalResult.isConstant() && isDataStructure(evalResult.getValue())) {
            return EvalResult.nonConstant(evalResult.getNode());
        }
        return evalResult;
    }

    private boolean isDataStructure(Object obj) {
        return (obj instanceof Collection) || (obj instanceof Map);
    }

    @Override
    protected EvalResult assignDefault(Command command) {
        return null;
    }

    @Override
    protected void onCommand(Command command) {
        outStream.write(command);
    }

    @Override
    public PushStream getOutputStream() {
        return outStream;
    }
}
