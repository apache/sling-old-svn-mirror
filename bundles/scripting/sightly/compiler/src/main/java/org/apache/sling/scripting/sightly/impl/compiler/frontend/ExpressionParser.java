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

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;
import org.apache.sling.scripting.sightly.impl.parser.expr.generated.SightlyLexer;
import org.apache.sling.scripting.sightly.impl.parser.expr.generated.SightlyParser;

public class ExpressionParser {

    /**
     * Parses the expression string.
     *
     * @param expressionString as defined by the Sightly spec (https://github.com/Adobe-Marketing-Cloud/sightly-spec/blob/master/SPECIFICATION.md)
     * @return Parsed Expression object
     * @throws NullPointerException     is the given exprString is null
     * @throws SightlyCompilerException if an error occurs while parsing the expression
     */
    public Interpolation parseInterpolation(String expressionString) throws SightlyCompilerException {
        SightlyParser parser = createParser(expressionString);
        try {
            Interpolation interpolation = parser.interpolation().interp;
            interpolation.setContent(expressionString);
            return interpolation;
        } catch (RecognitionException e) {
            throw new SightlyCompilerException(e);
        }
    }

    private SightlyParser createParser(String string) {
        SightlyLexer lexer = new SightlyLexer(new ANTLRInputStream(string));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new SightlyParserErrorListener());
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        SightlyParser parser = new SightlyParser(tokenStream);
        parser.removeErrorListeners();
        parser.addErrorListener(new SightlyParserErrorListener());
        return parser;
    }

}
