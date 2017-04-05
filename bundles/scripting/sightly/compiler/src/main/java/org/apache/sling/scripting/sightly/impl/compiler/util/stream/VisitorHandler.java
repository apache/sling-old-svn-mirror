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
package org.apache.sling.scripting.sightly.impl.compiler.util.stream;

import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandHandler;
import org.apache.sling.scripting.sightly.compiler.commands.CommandVisitor;

/**
 * Delegates commands to a visitor.
 */
public class VisitorHandler implements CommandHandler {

    private CommandVisitor visitor;

    public VisitorHandler(CommandVisitor visitor) {
        this.visitor = visitor;
    }

    @Override
    public void onEmit(Command command) {
        command.accept(visitor);
    }

    @Override
    public void onError(String errorMessage) {
        throw new RuntimeException(errorMessage);
    }

    @Override
    public void onDone() {

    }
}
