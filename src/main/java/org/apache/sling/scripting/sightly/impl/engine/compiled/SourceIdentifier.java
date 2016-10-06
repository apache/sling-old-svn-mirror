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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration;
import org.apache.sling.scripting.sightly.java.compiler.ClassInfo;
import org.apache.sling.scripting.sightly.java.compiler.JavaEscapeUtils;

/**
 * Identifies a Java source file based on a {@link Resource}. Depending on the used constructor this class might provide the abstraction
 * for either a Java source file generated for a HTL script or for a HTL {@link Resource}-based Java Use-API Object.
 */
public class SourceIdentifier implements ClassInfo {

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
                processingScriptName = scriptName.substring(0, scriptName.length() - 5);
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

}
