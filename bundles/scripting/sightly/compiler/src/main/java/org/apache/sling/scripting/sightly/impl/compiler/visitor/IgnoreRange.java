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

import org.apache.sling.scripting.sightly.compiler.commands.Command;

/**
 * Ignore a nested range of commands.
 */
public abstract class IgnoreRange extends UniformVisitor {

    private final Class<? extends Command> rangeStart;
    private final Class<? extends Command> rangeEnd;

    private int skipCount = 1;

    public IgnoreRange(Class<? extends Command> rangeStart, Class<? extends Command> rangeEnd) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    @Override
    public void onCommand(Command command) {
        Class<? extends Command> commandClass = command.getClass();
        if (commandClass.equals(rangeStart)) {
            skipCount++;
        } else if (commandClass.equals(rangeEnd)) {
            skipCount--;
        }
        if (skipCount == 0) {
            onCompleted();
        }
    }

    protected abstract void onCompleted();
}
