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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NullLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.PropertyAccess;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.StringConstant;
import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.util.expression.NodeTransformer;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.EmitterVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.Streams;
import org.apache.sling.scripting.sightly.impl.compiler.visitor.TrackingVisitor;

/**
 * This optimization handles variables initialized to map literals. It initially assigns the values in the map to variables and wherever
 * the map literal is accessed, the property access is replaced with the value variable, thus removing the need for a map lookup.
 */
public final class SyntheticMapRemoval extends TrackingVisitor<MapLiteral> implements EmitterVisitor {

    public static final StreamTransformer TRANSFORMER = new StreamTransformer() {
        @Override
        public CommandStream transform(CommandStream inStream) {
            return Streams.map(inStream, new SyntheticMapRemoval());
        }
    };

    private static final String VARIABLE_MARKER = "_field$_";

    private final PushStream outputStream = new PushStream();
    private final NodeTransformer transformer = new PropertyAccessTransformer();

    private SyntheticMapRemoval() {
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        ExpressionNode node = variableBindingStart.getExpression();
        String variable = variableBindingStart.getVariableName();
        ExpressionNode transformed = transform(node);
        if (transformed instanceof MapLiteral) {
            MapLiteral newLiteral = overrideMap(variable, (MapLiteral) transformed);
            tracker.pushVariable(variable, newLiteral);
            transformed = newLiteral;
        } else {
            tracker.pushVariable(variable, null);
        }
        outputStream.write(new VariableBinding.Start(variable, transformed));
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {
        Map.Entry<String, MapLiteral> entry = tracker.peek();
        super.visit(variableBindingEnd);
        MapLiteral literal = entry.getValue();
        if (literal != null) {
            //need to un-bind all the introduced variables
            for (int i = 0; i < literal.getMap().size(); i++) {
                outputStream.write(VariableBinding.END);
            }
        }
    }

    private ExpressionNode transform(ExpressionNode node) {
        return transformer.transform(node);
    }

    private MapLiteral overrideMap(String variableName, MapLiteral mapLiteral) {
        Map<String, ExpressionNode> newLiteral = new HashMap<>();
        for (Map.Entry<String, ExpressionNode> entry : mapLiteral.getMap().entrySet()) {
            String property = entry.getKey();
            ExpressionNode valueNode = entry.getValue();
            String valueVariable = valueVariableName(variableName, property);
            newLiteral.put(property, new Identifier(valueVariable));
            outputStream.write(new VariableBinding.Start(valueVariable, valueNode));
        }
        return new MapLiteral(newLiteral);
    }

    private String valueVariableName(String variableName, String propertyName) {
        return variableName + VARIABLE_MARKER + propertyName;
    }

    @Override
    protected MapLiteral assignDefault(Command command) {
        return null;
    }

    @Override
    public void onCommand(Command command) {
        outputStream.write(command);
    }

    @Override
    public PushStream getOutputStream() {
        return outputStream;
    }

    private class PropertyAccessTransformer extends NodeTransformer {
        @Override
        public ExpressionNode evaluate(PropertyAccess propertyAccess) {
            ExpressionNode target = propertyAccess.getTarget();
            String variable = extractIdentifier(target);
            if (variable != null) {
                MapLiteral literal = tracker.get(variable);
                if (literal != null) {
                    String property = extractProperty(propertyAccess.getProperty());
                    if (property != null) {
                        ExpressionNode replacementNode = literal.getValue(property);
                        if (replacementNode == null) {
                            replacementNode = NullLiteral.INSTANCE;
                        }
                        return replacementNode;
                    }
                }
            }
            return super.evaluate(propertyAccess);
        }

        private String extractProperty(ExpressionNode expressionNode) {
            if (expressionNode instanceof StringConstant) {
                return ((StringConstant) expressionNode).getText();
            }
            return null;
        }

        private String extractIdentifier(ExpressionNode node) {
            if (node instanceof Identifier) {
                return ((Identifier) node).getName();
            }
            return null;
        }
    }
}
