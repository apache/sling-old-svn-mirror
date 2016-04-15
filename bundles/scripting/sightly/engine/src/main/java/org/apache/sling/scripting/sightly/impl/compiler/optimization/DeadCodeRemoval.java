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

import java.util.Stack;

import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BooleanConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NullLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NumericConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.ris.Command;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandStream;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.EmitterVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.Streams;
import org.apache.sling.scripting.sightly.impl.compiler.visitor.StatefulRangeIgnore;
import org.apache.sling.scripting.sightly.impl.compiler.visitor.StatefulVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.visitor.TrackingVisitor;
import org.apache.sling.scripting.sightly.impl.utils.RenderUtils;

/**
 * Removes code under conditionals which are proven to fail. It is probably
 * a good idea to run this optimization after running
 * {@link org.apache.sling.scripting.sightly.impl.compiler.optimization.reduce.ConstantFolding}
 */
public class DeadCodeRemoval extends TrackingVisitor<Boolean> implements EmitterVisitor {
    // this could be merged with constant folding for better accuracy

    public static StreamTransformer transformer() {
        return new StreamTransformer() {
            @Override
            public CommandStream transform(CommandStream inStream) {
                StatefulVisitor visitor = new StatefulVisitor();
                DeadCodeRemoval dcr = new DeadCodeRemoval(visitor.getControl());
                visitor.initializeWith(dcr);
                Streams.connect(inStream, dcr.getOutputStream(), visitor);
                return dcr.getOutputStream();
            }
        };
    }

    private final PushStream outStream = new PushStream();
    private final StatefulVisitor.StateControl stateControl;
    private final Stack<Boolean> keepConditionalEndStack = new Stack<Boolean>();

    public DeadCodeRemoval(StatefulVisitor.StateControl stateControl) {
        this.stateControl = stateControl;
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        Boolean truthValue = null;
        ExpressionNode node = variableBindingStart.getExpression();
        if (node instanceof StringConstant) {
            truthValue = RenderUtils.toBoolean(((StringConstant) node).getText());
        }
        if (node instanceof BooleanConstant) {
            truthValue = ((BooleanConstant) node).getValue();
        }
        if (node instanceof NumericConstant) {
            truthValue = RenderUtils.toBoolean(((NumericConstant) node).getValue());
        }
        if (node instanceof NullLiteral) {
           truthValue = RenderUtils.toBoolean(null);
        }
        tracker.pushVariable(variableBindingStart.getVariableName(), truthValue);
        outStream.emit(variableBindingStart);
    }

    @Override
    public void visit(Conditional.Start conditionalStart) {
        Boolean truthValue = tracker.get(conditionalStart.getVariable());
        boolean keepConditionalEnd;
        if (truthValue == null) { //no information about the value of this variable
            keepConditionalEnd = true;
            outStream.emit(conditionalStart);
        } else { //we already know what happens with this conditional. We can remove it
            keepConditionalEnd = false;
            if (truthValue != conditionalStart.getExpectedTruthValue()) {
                //this conditional will always fail. We can ignore everything until
                //the corresponding end-conditional
                stateControl.push(new StatefulRangeIgnore(stateControl, Conditional.Start.class, Conditional.End.class));
                return;
            }
        }
        keepConditionalEndStack.push(keepConditionalEnd);
    }

    @Override
    public void visit(Conditional.End conditionalEnd) {
        boolean keep = keepConditionalEndStack.pop();
        if (keep) {
            outStream.emit(conditionalEnd);
        }
    }

    @Override
    public PushStream getOutputStream() {
        return outStream;
    }

    @Override
    protected Boolean assignDefault(Command command) {
        return false;
    }

    @Override
    protected void onCommand(Command command) {
        outStream.emit(command);
    }

}
