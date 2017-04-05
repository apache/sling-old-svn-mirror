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
package org.apache.sling.scripting.sightly.java.compiler.impl;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Builder object for a Java class printing method.
 */
public class JavaSource {

    private static final String INDENT = "    ";

    private int indentLevel;
    private final StringBuilder builder = new StringBuilder();

    public JavaSource prepend(String str) {
        builder.insert(0, str);
        return this;
    }

    public JavaSource beginIf() {
        indent().append("if (");
        return this;
    }

    public JavaSource completeIf() {
        append(") {\n");
        incIndent();
        return this;
    }

    public JavaSource endBlock() {
        decIndent();
        indent().append("}\n");
        return this;
    }

    public JavaSource comment(String text) {
        indent().append("//").append(text).append("\n");
        return this;
    }

    public JavaSource assign() {
        return append(" = ");
    }

    public JavaSource number(int value) {
        return append(Integer.toString(value));
    }

    public JavaSource increment() {
        return append("++");
    }

    public JavaSource beginAssignment(String variable, String type) {
        return declare(variable, type).assign();
    }

    public JavaSource beginAssignment(String variable) {
        return beginAssignment(variable, "Object");
    }

    public JavaSource declare(String variable, String type) {
        return startStatement().append(type).append(" ").append(variable);
    }

    public JavaSource declare(String variable) {
        return declare(variable, "Object");
    }

    public JavaSource equality() {
        return append(" == ");
    }

    public JavaSource startCall(String methodName, boolean useDot) {
        if (useDot) {
            builder.append('.');
        }
        builder.append(methodName).append('(');
        return this;
    }

    public JavaSource startCall(String methodName) {
        return startCall(methodName, false);
    }

    public JavaSource startMethodCall(String target, String method) {
        return append(target).startCall(method, true);
    }

    public JavaSource startArray() {
        return append("new Object[] {");
    }

    public JavaSource endArray() {
        return append("}");
    }

    public JavaSource startStatement() {
        indent();
        return this;
    }

    public JavaSource nullLiteral() {
        return append("null");
    }

    public JavaSource separateArgument() {
        builder.append(", ");
        return this;
    }

    public JavaSource stringLiteral(String text) {
        builder.append('"')
                .append(StringEscapeUtils.escapeJava(text))
                .append('"');
        return this;
    }

    public JavaSource startExpression() {
        builder.append('(');
        return this;
    }

    public JavaSource endExpression() {
        builder.append(')');
        return this;
    }

    public JavaSource endCall() {
        builder.append(')');
        return this;
    }

    public JavaSource startBlock() {
        indent().append("{\n");
        incIndent();
        return this;
    }

    public JavaSource beginFor(String itemVariable, String listVariable) {
        startStatement()
                .append("for (Object ")
                .append(itemVariable)
                .append(" : ")
                .append(listVariable)
                .append(") {\n");
        incIndent();
        return this;
    }

    public JavaSource endFor() {
        return endBlock();
    }

    public JavaSource endIf() {
        return endBlock();
    }

    public JavaSource beginNewInstanceCall(String className) {
        return append("new ").append(className).append("(");
    }

    public JavaSource append(String str) {
        builder.append(str);
        return this;
    }

    public JavaSource endStatement() {
        builder.append(";\n");
        return this;
    }

    public JavaSource cast(String type) {
        return append("(").append(type).append(")");
    }

    public JavaSource operator(String op) {
        return append(" ").append(op).append(" ");
    }

    public JavaSource beginAnonymousInstance(String className) {
        append("new ").append(className).append("() {\n");
        incIndent();
        return this;
    }

    public JavaSource endAnonymousInstance() {
        decIndent();
        append("}");
        return this;
    }

    public JavaSource beginMethod(String name, String returnType) {
        return startStatement().append("public ").append(returnType).append(" ").append(name).append("(");
    }

    public JavaSource methodParameter(String name, String type) {
        return append(type).append(" ").append(name);
    }

    public JavaSource completeMethodSignature() {
        append(") {\n");
        incIndent();
        return this;
    }

    public JavaSource endMethod() {
        return endBlock();
    }

    public JavaSource conditional() {
        return append(" ? ");
    }

    public JavaSource conditionalBranchSep() {
        return append(" : ");
    }

    public JavaSource negation() {
        return append("!");
    }

    public JavaSource newLine() {
        return append("\n");
    }

    public JavaSource objectModel() {
        return startMethodCall(SourceGenConstants.RENDER_CONTEXT_INSTANCE, SourceGenConstants.RENDER_CONTEXT_GET_OBJECT_MODEL).endCall();
    }

    private StringBuilder indent() {
        for (int i = 0; i < indentLevel; i++) {
            builder.append(INDENT);
        }
        return builder;
    }

    private void incIndent() {
        indentLevel++;
    }

    private void decIndent() {
        indentLevel--;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
