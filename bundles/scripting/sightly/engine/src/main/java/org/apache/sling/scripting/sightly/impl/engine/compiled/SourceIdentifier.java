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
package org.apache.sling.scripting.sightly.impl.engine.compiled;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration;
import org.apache.sling.scripting.sightly.java.compiler.ClassInfo;

/**
 * Identifies a Java source file based on a {@link Resource}. Depending on the used constructor this class might provide the abstraction
 * for either a Java source file generated for a HTL script or for a HTL {@link Resource}-based Java Use-API Object.
 */
public class SourceIdentifier implements ClassInfo {

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
        add("throws");
        add("transient");
        add("try");
        add("void");
        add("volatile");
        add("while");
    }};

    public static final Pattern MANGLED_CHAR_PATTER = Pattern.compile("(.*)(__[0-9a-f]{4}__)(.*)");

    private SightlyEngineConfiguration engineConfiguration;
    private String scriptName;
    private String simpleClassName;
    private String packageName;
    private String fullyQualifiedClassName;

    public SourceIdentifier(SightlyEngineConfiguration engineConfiguration, String scriptName) {
        this.engineConfiguration = engineConfiguration;
        this.scriptName = scriptName;
    }

    @Override
    public String getSimpleClassName() {
        if (simpleClassName == null) {
            int lastSlashIndex = scriptName.lastIndexOf("/");
            String processingScriptName = scriptName;
            if (scriptName.endsWith(".java")) {
                processingScriptName = scriptName.substring(0, scriptName.length() - 5);
            }
            if (lastSlashIndex != -1) {
                simpleClassName = makeJavaPackage(processingScriptName.substring(lastSlashIndex));
            } else {
                simpleClassName = makeJavaPackage(processingScriptName);
            }
        }
        return simpleClassName;
    }

    @Override
    public String getPackageName() {
        if (packageName == null) {
            int lastSlashIndex = scriptName.lastIndexOf("/");
            String processingScriptName = scriptName;
            boolean javaFile = scriptName.endsWith(".java");
            if (javaFile) {
                processingScriptName = scriptName.substring(0, scriptName.length() - 5);
            }
            if (lastSlashIndex != -1) {
                packageName = makeJavaPackage(processingScriptName.substring(0, lastSlashIndex));
            } else {
                packageName = makeJavaPackage(processingScriptName);
            }
            if (!javaFile) {
                packageName = engineConfiguration.getBundleSymbolicName() + "." + packageName;
            }
        }
        return packageName;
    }

    @Override
    public String getFullyQualifiedClassName() {
        if (fullyQualifiedClassName == null) {
            fullyQualifiedClassName = getPackageName() + "." + getSimpleClassName();
        }
        return fullyQualifiedClassName;
    }

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

    public static String getScriptName(String slashSubpackage, String fullyQualifiedClassName) {
        String className = fullyQualifiedClassName;
        StringBuilder pathElements = new StringBuilder("/");
        if (StringUtils.isNotEmpty(slashSubpackage) && className.contains(slashSubpackage)) {
            className = className.replaceAll(slashSubpackage + "\\.", "");
        }
        String[] classElements = StringUtils.split(className, '.');
        for (int i = 0; i < classElements.length; i++) {
            String classElem = classElements[i];
            Matcher matcher = MANGLED_CHAR_PATTER.matcher(classElem);
            if (matcher.matches()) {
                String group = matcher.group(2);
                char unmangled = unmangle(group);
                classElem = classElem.replaceAll(group, Character.toString(unmangled));
                while (matcher.find()) {
                    group = matcher.group(2);
                    unmangled = unmangle(group);
                    classElem = classElem.replaceAll(group, Character.toString(unmangled));
                }
            } else {
                int underscoreIndex = classElem.indexOf('_');
                if (underscoreIndex > -1) {
                    if (underscoreIndex == classElem.length() - 1) {
                        classElem = classElem.substring(0, classElem.length() -1);
                    } else {
                        classElem = classElem.replaceAll("_", ".");
                    }
                }
            }
            pathElements.append(classElem);
            if (i < classElements.length - 1) {
                pathElements.append("/");
            }
        }
        return pathElements.toString();
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
     * Test whether the argument is a Java keyword.
     *
     * @param key the String to test
     * @return {@code true} if the String is a Java keyword, {@code false} otherwise
     */
    public static boolean isJavaKeyword(String key) {
        return javaKeywords.contains(key);
    }
}
