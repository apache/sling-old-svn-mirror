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
package org.apache.sling.scripting.sightly.impl.compiler.frontend;

import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;

/**
 * The {@code SightlyParserErrorListener} handles parsing error reporting by sending offending input to a logger.
 */
public class SightlyParserErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        String offendingInput;
        if (Parser.class.isAssignableFrom(recognizer.getClass())) {
            List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
            Collections.reverse(stack);
            offendingInput = ((CommonTokenStream) recognizer.getInputStream()).getTokenSource().getInputStream().toString();
        } else {
            offendingInput = recognizer.getInputStream().toString();
        }
        if (e != null) {
            throw new SightlyCompilerException(msg, offendingInput, line, charPositionInLine, e);
        }
        throw new SightlyCompilerException(msg, offendingInput, line, charPositionInLine);
    }
}
