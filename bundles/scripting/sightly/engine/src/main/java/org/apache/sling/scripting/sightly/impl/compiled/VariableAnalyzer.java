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

package org.apache.sling.scripting.sightly.impl.compiled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.sling.scripting.sightly.impl.compiler.SightlyParsingException;
import org.apache.sling.scripting.sightly.impl.compiler.util.VariableTracker;

/**
 * Data structure used in the analysis of variables
 * during the compilation process
 */
public class VariableAnalyzer {

    private static final HashSet<String> javaKeywords = new HashSet<String>();
    private final VariableTracker<VariableDescriptor> tracker = new VariableTracker<VariableDescriptor>();
    private final List<VariableDescriptor> variables = new ArrayList<VariableDescriptor>();
    private final HashMap<String, VariableDescriptor> dynamicVariables = new HashMap<String, VariableDescriptor>();
    private final HashMap<String, VariableDescriptor> staticVariables = new HashMap<String, VariableDescriptor>();
    private static final String DYNAMIC_PREFIX = "_dynamic_";
    private static final String GLOBAL_PREFIX = "_global_";

    /**
     * Mark the declaration of a variable in the Java code
     * @param originalName the original name of the variable
     * @param type the variable's type
     * @return - a variable descriptor uniquely assigned to this variable
     */
    public VariableDescriptor declareVariable(String originalName, Type type) {
        originalName = originalName.toLowerCase();
        String assignedName = findSafeName(originalName);
        VariableDescriptor descriptor = new VariableDescriptor(originalName, assignedName, type, VariableScope.SCOPED);
        tracker.pushVariable(originalName, descriptor);
        variables.add(descriptor);
        return descriptor;
    }

    /**
     * Declare a global variable. Redundant declarations are ignored
     * @param originalName the original name of the variable
     * @return a variable descriptor
     */
    public VariableDescriptor declareGlobal(String originalName) {
        originalName = originalName.toLowerCase();
        VariableDescriptor descriptor = staticVariables.get(originalName);
        if (descriptor == null) {
            String assignedName = findGlobalName(originalName);
            descriptor = new VariableDescriptor(originalName, assignedName, Type.UNKNOWN, VariableScope.GLOBAL);
            variables.add(descriptor);
            staticVariables.put(originalName, descriptor);
        }
        return descriptor;
    }

    /**
     * Mark this variable as a template
     * @param originalName the original name of the variable
     * @return a variable descriptor
     */
    public VariableDescriptor declareTemplate(String originalName) {
        originalName = originalName.toLowerCase();
        VariableDescriptor descriptor = dynamicDescriptor(originalName);
        descriptor.markAsTemplate();
        return descriptor;
    }

    /**
     * Mark the end of a variable scope
     */
    public VariableDescriptor endVariable() {
        VariableDescriptor descriptor = tracker.peek().getValue();
        tracker.popVariable();
        return descriptor;
    }

    /**
     * Get a the descriptor for the given variable
     * @param name the original lowerName of the variable
     * @return the variable descriptor. If the variable is not in scope,
     * then a dynamic variable descriptor is provided
     */
    public VariableDescriptor descriptor(String name) {
        String lowerName = name.toLowerCase();
        VariableDescriptor descriptor = tracker.get(lowerName);
        if (descriptor == null) {
            descriptor = staticVariables.get(lowerName);
        }
        if (descriptor == null) {
            descriptor = dynamicDescriptor(lowerName);
        }
        return descriptor;
    }

    /**
     * Get the collection of all the variables encountered so far
     * @return an unmodifiable list of all the variables tracked by this analyzer
     */
    public Collection<VariableDescriptor> allVariables() {
        return Collections.unmodifiableList(variables);
    }

    /**
     * Shortcut method that returns the assigned name for the given
     * variable
     * @param original the original variable name
     * @return the assigned name for this compilation process
     */
    public String assignedName(String original) {
        return descriptor(original).getAssignedName();
    }

    private VariableDescriptor dynamicDescriptor(String original) {
        VariableDescriptor descriptor = dynamicVariables.get(original);
        if (descriptor == null) {
            String dynamicName = findDynamicName(validName(original));
            descriptor = new VariableDescriptor(original, dynamicName, Type.UNKNOWN, VariableScope.DYNAMIC);
            dynamicVariables.put(original, descriptor);
            variables.add(descriptor);
        }
        return descriptor;
    }

    private String findDynamicName(String original) {
        return DYNAMIC_PREFIX + syntaxSafeName(original);
    }

    private String findGlobalName(String original) {
        return GLOBAL_PREFIX + syntaxSafeName(original);
    }

    private String findSafeName(String original) {
        int occurrenceCount = tracker.getOccurrenceCount(original);
        String syntaxSafe = syntaxSafeName(original);
        if (occurrenceCount == 0) {
            return syntaxSafe; //no other declarations in scope. Use this very name
        } else {
            return original + "_" + occurrenceCount;
        }
    }

    private String syntaxSafeName(String original) {
        if (javaKeywords.contains(original)) {
            return "_" + original;
        }
        return original.replaceAll("-", "_");
    }

    private String validName(String name) {
        if (name == null || name.contains("-")) {
            throw new SightlyParsingException("Unsupported identifier name: " + name);
        }
        return name.toLowerCase();
    }

    static {
        javaKeywords.add("abstract");
        javaKeywords.add("continue");
        javaKeywords.add("for");
        javaKeywords.add("new");
        javaKeywords.add("switch");
        javaKeywords.add("assert");
        javaKeywords.add("default");
        javaKeywords.add("goto");
        javaKeywords.add("package");
        javaKeywords.add("synchronized");
        javaKeywords.add("boolean");
        javaKeywords.add("do");
        javaKeywords.add("if");
        javaKeywords.add("private");
        javaKeywords.add("this");
        javaKeywords.add("break");
        javaKeywords.add("double");
        javaKeywords.add("implements");
        javaKeywords.add("protected");
        javaKeywords.add("throw");
        javaKeywords.add("byte");
        javaKeywords.add("else");
        javaKeywords.add("import");
        javaKeywords.add("public");
        javaKeywords.add("throws");
        javaKeywords.add("case");
        javaKeywords.add("enum");
        javaKeywords.add("instanceof");
        javaKeywords.add("return");
        javaKeywords.add("transient");
        javaKeywords.add("catch");
        javaKeywords.add("extends");
        javaKeywords.add("int");
        javaKeywords.add("short");
        javaKeywords.add("try");
        javaKeywords.add("char");
        javaKeywords.add("final");
        javaKeywords.add("interface");
        javaKeywords.add("static");
        javaKeywords.add("void");
        javaKeywords.add("class");
        javaKeywords.add("finally");
        javaKeywords.add("long");
        javaKeywords.add("strictfp");
        javaKeywords.add("volatile");
        javaKeywords.add("const");
        javaKeywords.add("float");
        javaKeywords.add("native");
        javaKeywords.add("super");
        javaKeywords.add("while");
    }

}
