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
package org.apache.sling.scripting.sightly.impl.compiler.ris.command;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.scripting.sightly.impl.compiler.ris.Command;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandVisitor;

/**
 * Commands that delimit a procedure
 */
public class Procedure {

    public static class Start implements Command {

        private String name;
        private Set<String> parameters;

        public Start(String name, Set<String> parameters) {
            this.name = name;
            this.parameters = new HashSet<String>(parameters);
        }

        public String getName() {
            return name;
        }

        public Set<String> getParameters() {
            return Collections.unmodifiableSet(parameters);
        }

        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final End END = new End();

    public static final class End implements Command {

        private End() {
        }

        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class Call implements Command {

        private final String templateVariable;
        private final String argumentsVariable;

        public Call(String templateVariable, String argumentsVariable) {
            this.templateVariable = templateVariable;
            this.argumentsVariable = argumentsVariable;
        }

        public String getTemplateVariable() {
            return templateVariable;
        }

        public String getArgumentsVariable() {
            return argumentsVariable;
        }

        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }
    }

}
