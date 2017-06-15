/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.java.compiler;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * The {@code JavaEscapeUtils} provides useful methods for escaping or transforming invalid Java tokens to valid ones that could be used in
 * generated Java source code.
 */
public class JavaEscapeUtils {

    private static final Set<String> javaKeywords = new HashSet<String>() {{
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
     * Converts the given identifier to a legal Java identifier
     *
     * @param identifier the identifier to convert
     * @return legal Java identifier corresponding to the given identifier
     */
    public static String makeJavaIdentifier(String identifier) {
        StringBuilder modifiedIdentifier = new StringBuilder(identifier.length());
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            modifiedIdentifier.append('_');
        }
        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) {
                modifiedIdentifier.append(ch);
            } else if (ch == '.') {
                modifiedIdentifier.append('_');
            } else {
                modifiedIdentifier.append(mangleChar(ch));
            }
        }
        if (isJavaKeyword(modifiedIdentifier.toString())) {
            modifiedIdentifier.append('_');
        }
        return modifiedIdentifier.toString();
    }

    /**
     * Mangle the specified character to create a legal Java class name.
     *
     * @param ch the character to mangle
     * @return the mangled
     */
    public static String mangleChar(char ch) {
        return String.format("__%04x__", (int) ch);
    }

    /**
     * Provided a mangled string (obtained by calling {@link #mangleChar(char)}) it will will return the character that was mangled.
     *
     * @param mangled the mangled string
     * @return the original character
     */
    public static char unmangle(String mangled) {
        String toProcess = mangled.replaceAll("__", "");
        return (char) Integer.parseInt(toProcess, 16);
    }

    /**
     * Converts the given scriptName to a Java package or fully-qualified class name
     *
     * @param scriptName the scriptName to convert
     * @return Java package corresponding to the given scriptName
     */
    public static String makeJavaPackage(String scriptName) {
        String classNameComponents[] = StringUtils.split(scriptName, '/');
        StringBuilder legalClassNames = new StringBuilder();
        for (int i = 0; i < classNameComponents.length; i++) {
            legalClassNames.append(makeJavaIdentifier(classNameComponents[i]));
            if (i < classNameComponents.length - 1) {
                legalClassNames.append('.');
            }
        }
        return legalClassNames.toString();
    }

    /**
     * Test whether the argument is a Java keyword.
     *
     * @param key the String to test
     * @return {@code true} if the String is a Java keyword, {@code false} otherwise
     */
    public static boolean isJavaKeyword(String key) {
        return javaKeywords.contains(key);
    }

}
