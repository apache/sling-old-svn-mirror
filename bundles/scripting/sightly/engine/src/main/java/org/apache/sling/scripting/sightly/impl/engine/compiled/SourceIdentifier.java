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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.scripting.sightly.impl.compiler.UnitChangeMonitor;
import org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration;

/**
 * Identifies a Java source file in a JCR repository.
 */
public class SourceIdentifier {

    private static final Set<String> reservedKeywords = new HashSet<String>(){{
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

    private final String className;
    private final Resource resource;
    private final String packageName;
    private final String fullyQualifiedName;
    private SightlyEngineConfiguration configuration;
    private UnitChangeMonitor unitChangeMonitor;
    private ClassLoaderWriter writer;

    public SourceIdentifier(SightlyEngineConfiguration configuration, UnitChangeMonitor unitChangeMonitor, ClassLoaderWriter writer,
                            Resource resource, String classNamePrefix) {
        this.resource = resource;
        this.className = buildClassName(resource, classNamePrefix);
        this.packageName = buildPackageName(resource);
        this.fullyQualifiedName = buildFullyQualifiedName(packageName, className);
        this.configuration = configuration;
        this.unitChangeMonitor = unitChangeMonitor;
        this.writer = writer;
    }

    public String getClassName() {
        return className;
    }

    public Resource getResource() {
        return resource;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public boolean needsUpdate() {
        if (configuration.isDevMode()) {
            return true;
        }
        String slyPath = getResource().getPath();
        long slyScriptChangeDate = unitChangeMonitor.getLastModifiedDateForScript(slyPath);
        String javaCompilerPath = "/" + getFullyQualifiedName().replaceAll("\\.", "/") + ".class";
        long javaFileDate = writer.getLastModified(javaCompilerPath);
        return ((slyScriptChangeDate == 0 && javaFileDate > -1) || (slyScriptChangeDate > javaFileDate));
    }

    private String buildFullyQualifiedName(String packageName, String className) {
        return packageName + "." + className;
    }

    private String buildClassName(Resource resource, String classNamePrefix) {
        String scriptName = ResourceUtil.getName(resource.getPath());
        scriptName = scriptName.substring(0, scriptName.lastIndexOf(getExtension(scriptName)));
        String className = classNamePrefix + scriptName;
        return className.replaceAll("-", "_").replaceAll("\\.", "_");
    }

    private String buildPackageName(Resource resource) {
        String packageName = ResourceUtil.getParent(resource.getPath()).replaceAll("/", ".").substring(1).replaceAll("-", "_");
        String[] packageNameElements = packageName.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packageNameElements.length; i++) {
            String subPackage = packageNameElements[i];
            if (reservedKeywords.contains(subPackage)) {
                sb.append(subPackage).append("_");
            } else {
                sb.append(subPackage);
            }
            if (i != packageNameElements.length - 1) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private String getExtension(String scriptName) {
        int lastDotIndex = scriptName.lastIndexOf('.');
        return scriptName.substring(lastDotIndex);
    }

}
