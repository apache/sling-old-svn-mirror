/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.java;

public class CompilerUtil {

    /**
     * The path has to be mapped to form a proper java class name.
     * @param path
     * @return The mapped source path.
     */
    public static String mapSourcePath(final String path) {
        final String str;
        if ( path.endsWith(".java") ) {
            str = path.substring(0, path.length() - 5);
        } else {
            str = path;
        }
        final int pos = str.lastIndexOf("/");
        if ( pos == -1 ) {
            return makeJavaIdentifier(str);
        }
        return str.substring(0, pos + 1) + makeJavaIdentifier(str.substring(pos + 1));
    }

    /**
     * Create the class name from the source name
     * @param sourcePath The source path
     * @return The corresponding class file path.
     */
    public static String makeClassPath(String sourcePath) {
        String str = sourcePath;
        if (str.endsWith(".java")) {
            str = str.substring(0, str.length() - 5);
        }
        return str;
    }


    /**
     * Converts the given identifier to a legal Java identifier
     *
     * @param identifier Identifier to convert
     *
     * @return Legal Java identifier corresponding to the given identifier
     */
    public static final String makeJavaIdentifier(String identifier) {
        StringBuffer modifiedIdentifier =
            new StringBuffer(identifier.length());
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            modifiedIdentifier.append('_');
        }
        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(ch) && ch != '_') {
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
     */
    public static final String mangleChar(char ch) {
        char[] result = new char[5];
        result[0] = '_';
        result[1] = Character.forDigit((ch >> 12) & 0xf, 16);
        result[2] = Character.forDigit((ch >> 8) & 0xf, 16);
        result[3] = Character.forDigit((ch >> 4) & 0xf, 16);
        result[4] = Character.forDigit(ch & 0xf, 16);
        return new String(result);
    }

    private static final String javaKeywords[] = {
        "abstract", "assert", "boolean", "break", "byte", "case",
        "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends",
        "final", "finally", "float", "for", "goto",
        "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short",
        "static", "strictfp", "super", "switch", "synchronized",
        "this", "throws", "transient", "try", "void",
        "volatile", "while" };

    /**
     * Test whether the argument is a Java keyword
     */
    public static boolean isJavaKeyword(String key) {
        int i = 0;
        int j = javaKeywords.length;
        while (i < j) {
            int k = (i+j)/2;
            int result = javaKeywords[k].compareTo(key);
            if (result == 0) {
                return true;
            }
            if (result < 0) {
                i = k+1;
            } else {
                j = k;
            }
        }
        return false;
    }

}
