/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * The {@code JavaEscapeUtils} provides useful methods for escaping or transforming invalid Java tokens to valid ones that could be used in
 * generated Java source code.
 */
public class JavaEscapeUtils {

    private static final Set<String> reservedKeywords = new HashSet<String>() {{
        add("abstract");
        add("assert");
        add("boolean");
        add("break");
        add("byte");
        add("case");
        add("catch");
        add("char");
        add("class");
        add("const");
        add("continue");
        add("default");
        add("do");
        add("double");
        add("else");
        add("enum");
        add("extends");
        add("final");
        add("finally");
        add("float");
        add("for");
        add("goto");
        add("if");
        add("implements");
        add("import");
        add("instanceof");
        add("int");
        add("interface");
        add("long");
        add("native");
        add("new");
        add("package");
        add("private");
        add("protected");
        add("public");
        add("return");
        add("short");
        add("static");
        add("strictfp");
        add("super");
        add("switch");
        add("synchronized");
        add("this");
        add("throw");
        add("throws");
        add("transient");
        add("try");
        add("void");
        add("volatile");
        add("while");
    }};

    /**
     * Escapes and / or transforms an invalid token (in case the token represents a reserved Java keyword) to provide a valid token.
     *
     * @param token the token to be transformed
     * @return a valid Java token
     */
    public static String getEscapedToken(String token) {
        String result = token.toLowerCase();
        if (reservedKeywords.contains(result)) {
            result = "_" + result;
        }
        return result.replaceAll("-", "_");
    }

}
