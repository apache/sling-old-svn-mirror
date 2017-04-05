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
package org.apache.sling.scripting.sightly.compiler.commands;

/**
 * This {@link Command} renders a sequence of commands repeatedly. The command must have a corresponding end loop later in the stream.
 */
public final class Loop {

    public static class Start implements Command {

        private String listVariable;
        private String itemVariable;
        private String indexVariable;

        public Start(String listVariable, String itemVariable, String indexVariable) {
            this.listVariable = listVariable;
            this.itemVariable = itemVariable;
            this.indexVariable = indexVariable;
        }

        public String getListVariable() {
            return listVariable;
        }

        public String getItemVariable() {
            return itemVariable;
        }

        public String getIndexVariable() {
            return indexVariable;
        }

        @Override
        public void accept(CommandVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return "Loop.Start{" +
                    "listVariable='" + listVariable + '\'' +
                    ", itemVariable='" + itemVariable + '\'' +
                    ", indexVariable='" + indexVariable + '\'' +
                    '}';
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

        @Override
        public String toString() {
            return "Loop.End{}";
        }
    }

}
