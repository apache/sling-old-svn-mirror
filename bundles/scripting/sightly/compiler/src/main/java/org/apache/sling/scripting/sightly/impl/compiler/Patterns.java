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
package org.apache.sling.scripting.sightly.impl.compiler;

import org.apache.sling.scripting.sightly.compiler.commands.Conditional;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BooleanConstant;

/**
 * The {@code Patterns} class provides various static methods that implement commonly used stream processing commands.
 */
public final class Patterns {

    private static final String ALWAYS_FALSE_VAR = "always_false";

    /**
     * Inserts a sequence of commands that will ignore the rest of the stream until
     * the end stream sequence is inserted
     * @param stream - the stream
     */
    public static void beginStreamIgnore(PushStream stream) {
        stream.write(new VariableBinding.Start(ALWAYS_FALSE_VAR, BooleanConstant.FALSE));
        stream.write(new Conditional.Start(ALWAYS_FALSE_VAR, true));
    }

    /**
     * Inserts a sequence of commands that cancels stream ignore
     * @param stream - the input stream
     */
    public static void endStreamIgnore(PushStream stream) {
        stream.write(Conditional.END);
        stream.write(VariableBinding.END);
    }


}
