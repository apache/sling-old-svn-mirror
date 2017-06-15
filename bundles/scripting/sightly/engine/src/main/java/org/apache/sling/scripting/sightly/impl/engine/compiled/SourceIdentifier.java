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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration;
import org.apache.sling.scripting.sightly.java.compiler.ClassInfo;
import org.apache.sling.scripting.sightly.java.compiler.JavaEscapeUtils;

/**
 * Identifies a Java source file based on a {@link Resource}. Depending on the used constructor this class might provide the abstraction
 * for either a Java source file generated for a HTL script or for a HTL {@link Resource}-based Java Use-API Object.
 */
public class SourceIdentifier implements ClassInfo {

    private static final Pattern MANGLED_CHAR_PATTERN = Pattern.compile("(.*)(__[0-9a-f]{4}__)(.*)");
    private SightlyEngineConfiguration engineConfiguration;
    private String scriptName;
    private String simpleClassName;
    private String packageName;
    private String fullyQualifiedClassName;

    public SourceIdentifier(SightlyEngineConfiguration engineConfiguration, String resourcePath) {
        this.engineConfiguration = engineConfiguration;
        this.scriptName = resourcePath;
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
                simpleClassName = JavaEscapeUtils.makeJavaPackage(processingScriptName.substring(lastSlashIndex));
            } else {
                simpleClassName = JavaEscapeUtils.makeJavaPackage(processingScriptName);
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
                processingScriptName = scriptName.substring(0, scriptName.length() - 5).replaceAll("-", "_");
            }
            if (lastSlashIndex != -1) {
                packageName = JavaEscapeUtils.makeJavaPackage(processingScriptName.substring(0, lastSlashIndex));
            } else {
                packageName = JavaEscapeUtils.makeJavaPackage(processingScriptName);
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
     * Given a {@code fullyQualifiedClassName} and optionally a sub-package that should be stripped ({@code slashSubpackage}), this
     * method will try to locate a {@code Resource} in the repository that provides the source code for the Java class.
     *
     * @param resolver                a resource resolver with access to the script paths
     * @param slashSubpackage         an optional sub-package that will be stripped from the {@code fullyQualifiedClassName}
     * @param fullyQualifiedClassName the FQCN
     * @return the {@code Resource} backing the class, or {@code null} if one cannot be found
     */
    public static Resource getPOJOFromFQCN(ResourceResolver resolver, String slashSubpackage, String fullyQualifiedClassName) {
        String className = fullyQualifiedClassName;
        StringBuilder pathElements = new StringBuilder("/");
        if (StringUtils.isNotEmpty(slashSubpackage) && className.contains(slashSubpackage)) {
            className = className.replaceAll(slashSubpackage + "\\.", "");
        }
        String[] classElements = StringUtils.split(className, '.');
        for (int i = 0; i < classElements.length; i++) {
            String classElem = classElements[i];
            Matcher matcher = MANGLED_CHAR_PATTERN.matcher(classElem);
            if (matcher.matches()) {
                String group = matcher.group(2);
                char unmangled = JavaEscapeUtils.unmangle(group);
                classElem = classElem.replaceAll(group, Character.toString(unmangled));
                while (matcher.find()) {
                    group = matcher.group(2);
                    unmangled = JavaEscapeUtils.unmangle(group);
                    classElem = classElem.replaceAll(group, Character.toString(unmangled));
                }
            } else {
                int underscoreIndex = classElem.indexOf('_');
                if (underscoreIndex > -1) {
                    if (underscoreIndex == classElem.length() - 1) {
                        classElem = classElem.substring(0, classElem.length() -1);
                    } else if (underscoreIndex == 0 && !Character.isJavaIdentifierStart(classElem.charAt(1))){
                        classElem = classElem.substring(1);
                    }
                }
            }
            pathElements.append(classElem);
            if (i < classElements.length - 1) {
                pathElements.append("/");
            }
        }
        Set<String> possiblePOJOPaths = getPossiblePojoPaths(pathElements.toString() + ".java");
        for (String possiblePath : possiblePOJOPaths) {
            Resource r = resolver.getResource(possiblePath);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /**
     * For a JCR path obtained from expanding a generated class name this method generates all the alternative path names that can be
     * obtained by expanding the mentioned class' name.
     *
     * @param originalPath one of the possible paths
     * @return a {@link Set} containing all the alternative paths if symbol replacement was needed; otherwise the set will contain just
     * the {@code originalPath}
     */
    private static Set<String> getPossiblePojoPaths(String originalPath) {
        Set<String> possiblePaths = new LinkedHashSet<>();
        possiblePaths.add(originalPath);
        Map<Integer, String> chars = new HashMap<>();
        AmbiguousPathSymbol[] symbols = AmbiguousPathSymbol.values();
        for (AmbiguousPathSymbol symbol : symbols) {
            String pathCopy = originalPath.substring(0, originalPath.lastIndexOf("/"));
            int actualIndex = 0;
            boolean firstPass = true;
            while (pathCopy.indexOf(symbol.getSymbol()) != -1) {
                int pos = pathCopy.indexOf(symbol.getSymbol());
                actualIndex += pos;
                if (!firstPass) {
                    actualIndex += 1;
                }
                chars.put(actualIndex, symbol.getSymbol().toString());
                pathCopy = pathCopy.substring(pos + 1);
                firstPass = false;
            }
        }
        if (chars.size() > 0) {
            ArrayList<AmbiguousPathSymbol[]> possibleArrangements = new ArrayList<>();
            populateArray(possibleArrangements, new AmbiguousPathSymbol[chars.size()], 0);
            Integer[] indexes = chars.keySet().toArray(new Integer[chars.size()]);
            for (AmbiguousPathSymbol[] arrangement : possibleArrangements) {
                char[] possiblePath = originalPath.toCharArray();
                for (int i = 0; i < arrangement.length; i++) {
                    char currentSymbol = arrangement[i].getSymbol();
                    int currentIndex = indexes[i];
                    possiblePath[currentIndex] = currentSymbol;
                }
                possiblePaths.add(new String(possiblePath));
            }
        }
        return possiblePaths;
    }

    /**
     * Given an initial array with its size equal to the number of elements of a needed arrangement, this method will generate all
     * the possible arrangements of values for this array in the provided {@code arrayCollection}. The values with which the array is
     * populated are the {@link AmbiguousPathSymbol} constants.
     *
     * @param arrayCollection the collection that will store the arrays
     * @param symbolsArrangementArray an initial array that will be used for collecting the results
     * @param index the initial index of the array that will be populated (needed for recursion purposes; start with 0 for the initial call)
     */
    private static void populateArray(ArrayList<AmbiguousPathSymbol[]> arrayCollection, AmbiguousPathSymbol[] symbolsArrangementArray, int
            index) {
        if (symbolsArrangementArray.length > 0) {
            if (index == symbolsArrangementArray.length) {
                arrayCollection.add(symbolsArrangementArray.clone());
            } else {
                for (AmbiguousPathSymbol symbol : AmbiguousPathSymbol.values()) {
                    symbolsArrangementArray[index] = symbol;
                    populateArray(arrayCollection, symbolsArrangementArray, index + 1);
                }
            }
        }
    }

    /**
     * The {@code AmbiguousPathSymbol} holds symbols that are valid for a JCR path but that will get transformed to a "_" to obey the
     * Java naming conventions.
     */
    enum AmbiguousPathSymbol {
        DASH('-'),
        UNDERSCORE('_'),
        POINT('.');

        private Character symbol;

        AmbiguousPathSymbol(Character symbol) {
            this.symbol = symbol;
        }

        public Character getSymbol() {
            return symbol;
        }
    }

}
