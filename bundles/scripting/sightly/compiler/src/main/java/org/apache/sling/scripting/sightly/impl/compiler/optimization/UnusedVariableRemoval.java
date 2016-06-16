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
import java.util.Stack;

import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.EmitterVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.Streams;
import org.apache.sling.scripting.sightly.impl.compiler.visitor.TrackingVisitor;

/**
 * This optimization removes variables which are bound but never used in the command stream.
 */
public final class UnusedVariableRemoval extends TrackingVisitor<UnusedVariableRemoval.VariableActivity> implements EmitterVisitor {

    public static final StreamTransformer TRANSFORMER = new StreamTransformer() {
        @Override
        public CommandStream transform(CommandStream inStream) {
            return Streams.map(inStream, new UnusedVariableRemoval());
        }
    };

    private final PushStream outputStream = new PushStream();
    private final Stack<List<Command>> storedCommandsStack = new Stack<>();

    private UnusedVariableRemoval() {
    }

    @Override
    public PushStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        //push a new buffer where we will store the following commands
        //these commands will be emitted only if this variable will be used in
        //it's scope
        storedCommandsStack.push(new ArrayList<Command>());
        //start tracking the variable
        tracker.pushVariable(variableBindingStart.getVariableName(), new VariableActivity(variableBindingStart));
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {
        // Get the activity of the exiting variable
        VariableActivity variableActivity = tracker.peek().getValue();
        tracker.popVariable();
        boolean emitBindingEnd = true;
        if (variableActivity != null) {
            //this was a tracked variable. Popping all the commands
            //which were delayed for this variable
            List<Command> commands = storedCommandsStack.pop();
            //if the variable binding is emitted than this binding
            //end must be emitted as well
            emitBindingEnd = variableActivity.isUsed();
            if (variableActivity.isUsed()) {
                VariableBinding.Start variableBindingStart = variableActivity.getCommand();
                //variable was used. we can let it pass through
                emit(variableBindingStart);
                //register the usage of all the variables that appear in the bound expression
                registerUsage(variableBindingStart);
            }
            //write all the delayed commands
            for (Command command : commands) {
                emit(command);
            }
        }
        if (emitBindingEnd) {
            emit(variableBindingEnd);
        }
    }

    @Override
    protected VariableActivity assignDefault(Command command) {
        return null;
    }

    @Override
    protected void onCommand(Command command) {
        registerUsage(command);
        emit(command);
    }

    /**
     * Emit the current command. If the command is delayed by
     * a variable tracking process, than add it to the top command list
     * @param command a stream command
     */
    private void emit(Command command) {
        if (storedCommandsStack.isEmpty()) {
            outputStream.write(command);
        } else {
            List<Command> list = storedCommandsStack.peek();
            list.add(command);
        }
    }

    /**
     * Extract all the variables in this command and mark them
     * as used
     * @param command - a stream command
     */
    private void registerUsage(Command command) {
        List<String> usedVariables = CommandVariableUsage.extractVariables(command);
        for (String usedVariable : usedVariables) {
            VariableActivity activity = tracker.get(usedVariable);
            if (activity != null) {
                activity.markUsed();
            }
        }
    }

    /**
     * Track the activity of a variable binding
     */
    static class VariableActivity {
        private boolean used;
        private VariableBinding.Start command;

        VariableActivity(VariableBinding.Start command) {
            this.command = command;
        }

        public void markUsed() {
            used = true;
        }

        public boolean isUsed() {
            return used;
        }

        public VariableBinding.Start getCommand() {
            return command;
        }
    }
}
