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
package org.apache.sling.scripting.sightly.compiler.api.ris.command;

import org.apache.sling.scripting.sightly.compiler.api.ris.Command;
import org.apache.sling.scripting.sightly.compiler.api.ris.CommandVisitor;
import org.apache.sling.scripting.sightly.compiler.api.ris.Command;
import org.apache.sling.scripting.sightly.compiler.api.ris.CommandVisitor;

/**
 * Commands to control the write buffer
 */
public class BufferControl {

    public static final Push PUSH = new Push();

    public static final class Push implements Command {
        private Push() {

        }

        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * Pop the current stored buffer into a variable. This command
     * must effectively be matched by a VariableBinding.End command
     */
    public static final class Pop implements Command {

        private String variableName;

        public Pop(String variableName) {
            this.variableName = variableName;
        }

        /**
         * Get the variable that will store the output
         * @return - a variable name
         */
        public String getVariableName() {
            return variableName;
        }

        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }
    }
}
