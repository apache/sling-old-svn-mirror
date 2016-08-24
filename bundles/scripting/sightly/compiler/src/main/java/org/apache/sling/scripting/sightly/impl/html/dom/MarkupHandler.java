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

package org.apache.sling.scripting.sightly.impl.html.dom;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.compiler.commands.Conditional;
import org.apache.sling.scripting.sightly.compiler.commands.OutText;
import org.apache.sling.scripting.sightly.compiler.commands.OutputVariable;
import org.apache.sling.scripting.sightly.compiler.commands.VariableBinding;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperator;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BooleanConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.Patterns;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.ElementContext;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.ExpressionParser;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.ExpressionWrapper;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.Fragment;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.Interpolation;
import org.apache.sling.scripting.sightly.impl.compiler.util.SymbolGenerator;
import org.apache.sling.scripting.sightly.impl.filter.ExpressionContext;
import org.apache.sling.scripting.sightly.impl.filter.Filter;
import org.apache.sling.scripting.sightly.impl.html.MarkupUtils;
import org.apache.sling.scripting.sightly.compiler.expression.MarkupContext;
import org.apache.sling.scripting.sightly.impl.plugin.Plugin;
import org.apache.sling.scripting.sightly.impl.plugin.PluginCallInfo;
import org.apache.sling.scripting.sightly.impl.plugin.PluginInvoke;

/**
 * Implementation for the markup handler
 */
public class MarkupHandler {

    private final PushStream stream;
    private final SymbolGenerator symbolGenerator = new SymbolGenerator();
    private final ExpressionParser expressionParser = new ExpressionParser();
    private final Map<String, Plugin> pluginRegistry;
    private final CompilerContext compilerContext;
    private final ExpressionWrapper expressionWrapper;

    private final Stack<ElementContext> elementStack = new Stack<>();

    public MarkupHandler(PushStream stream, Map<String, Plugin> pluginRegistry, List<Filter> filters) {
        this.stream = stream;
        this.pluginRegistry = pluginRegistry;
        this.expressionWrapper = new ExpressionWrapper(filters);
        this.compilerContext = new CompilerContext(symbolGenerator, expressionWrapper, stream);
    }

    public void onOpenTagStart(String markup, String tagName) {
        ElementContext context = new ElementContext(tagName, markup);
        elementStack.push(context);
    }

    public void onAttribute(String name, String value, char quoteChar) {
        ElementContext context = elementStack.peek();
        if (Syntax.isPluginAttribute(name)) {
            handlePlugin(name, StringUtils.defaultString(value, ""), context);
        } else {
            context.addAttribute(name, value, quoteChar);
        }
    }

    public void onOpenTagEnd(String markup) {
        ElementContext context = elementStack.peek();
        PluginInvoke invoke = context.pluginInvoke();
        invoke.beforeElement(stream, context.getTagName());
        boolean slyTag = "sly".equalsIgnoreCase(context.getTagName());
        if (slyTag) {
            Patterns.beginStreamIgnore(stream);
        }
        invoke.beforeTagOpen(stream);
        out(context.getOpenTagStartMarkup());
        invoke.beforeAttributes(stream);
        traverseAttributes(context, invoke);
        invoke.afterAttributes(stream);
        out(markup);
        invoke.afterTagOpen(stream);
        if (slyTag) {
            Patterns.endStreamIgnore(stream);
        }
        invoke.beforeChildren(stream);
    }

    private void traverseAttributes(ElementContext context, PluginInvoke invoke) {
        for (ElementContext.Attribute attribute : context.getAttributes()) {
            String attrName = attribute.getName();
            Object contentObj = attribute.getValue();
            if (contentObj == null || contentObj instanceof String) {
                String content = (String) contentObj;
                emitAttribute(attrName, content, attribute.getQuoteChar(), invoke);
            } else if (contentObj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) contentObj;
                PluginCallInfo info = (PluginCallInfo) entry.getKey();
                Expression expression = (Expression) entry.getValue();
                invoke.onPluginCall(stream, info, expression);
            }
        }
    }

    private void emitAttribute(String name, String content, char quoteChar, PluginInvoke invoke) {
        invoke.beforeAttribute(stream, name);
        if (content == null) {
            emitSimpleTextAttribute(name, null, quoteChar, invoke);
        } else {
            Interpolation interpolation = expressionParser.parseInterpolation(content);
            String text = tryAsSimpleText(interpolation);
            if (text != null) {
                emitSimpleTextAttribute(name, text, quoteChar, invoke);
            } else {
                emitExpressionAttribute(name, interpolation, quoteChar, invoke);
            }
        }
        invoke.afterAttribute(stream, name);
    }

    private void emitSimpleTextAttribute(String name, String textValue, char quoteChar, PluginInvoke invoke) {
        emitAttributeStart(name);
        invoke.beforeAttributeValue(stream, name, new StringConstant(textValue));
        if (textValue != null) {
            emitAttributeValueStart(quoteChar);
            textValue = escapeQuotes(textValue);
            out(textValue);
            emitAttributeEnd(quoteChar);
        }
        invoke.afterAttributeValue(stream, name);
    }

    private String escapeQuotes(String textValue) {
        return textValue.replace("\"", "&quot;");
    }

    private void emitExpressionAttribute(String name, Interpolation interpolation, char quoteChar, PluginInvoke invoke) {
        interpolation = attributeChecked(name, interpolation);
        if (interpolation.size() == 1) {
            emitSingleFragment(name, interpolation, quoteChar, invoke);
        } else {
            emitMultipleFragment(name, interpolation, quoteChar, invoke);
        }
    }

    private void emitMultipleFragment(String name, Interpolation interpolation, char quoteChar, PluginInvoke invoke) {
        // Simplified algorithm for attribute output, which works when the interpolation is not of size 1. In this
        // case we are certain that the attribute value cannot be the boolean value true, so we can skip this test
        // altogether
        Expression expression = expressionWrapper.transform(interpolation, getAttributeMarkupContext(name), ExpressionContext.ATTRIBUTE);
        String attrContent = symbolGenerator.next("attrContent");
        String shouldDisplayAttr = symbolGenerator.next("shouldDisplayAttr");
        stream.write(new VariableBinding.Start(attrContent, expression.getRoot()));
        stream.write(
                new VariableBinding.Start(
                        shouldDisplayAttr,
                        new BinaryOperation(
                                BinaryOperator.OR,
                                new Identifier(attrContent),
                                new BinaryOperation(BinaryOperator.EQ, new StringConstant("false"), new Identifier(attrContent))
                        )
                )
        );
        stream.write(new Conditional.Start(shouldDisplayAttr, true));
        emitAttributeStart(name);
        invoke.beforeAttributeValue(stream, name, expression.getRoot());
        emitAttributeValueStart(quoteChar);
        stream.write(new OutputVariable(attrContent));
        emitAttributeEnd(quoteChar);
        invoke.afterAttributeValue(stream, name);
        stream.write(Conditional.END);
        stream.write(VariableBinding.END);
        stream.write(VariableBinding.END);
    }

    private void emitSingleFragment(String name, Interpolation interpolation, char quoteChar, PluginInvoke invoke) {
        Expression valueExpression = expressionWrapper.transform(interpolation, null, ExpressionContext.ATTRIBUTE); //raw expression
        String attrValue = symbolGenerator.next("attrValue"); //holds the raw attribute value
        String attrContent = symbolGenerator.next("attrContent"); //holds the escaped attribute value
        String isTrueVar = symbolGenerator.next("isTrueAttr"); // holds the comparison (attrValue == true)
        String shouldDisplayAttr = symbolGenerator.next("shouldDisplayAttr");
        MarkupContext markupContext = getAttributeMarkupContext(name);
        boolean alreadyEscaped = false;
        if (valueExpression.getRoot() instanceof RuntimeCall) {
            RuntimeCall rc = (RuntimeCall) valueExpression.getRoot();
            if (RuntimeFunction.XSS.equals(rc.getFunctionName())) {
                alreadyEscaped = true;
            }
        }
        ExpressionNode node = valueExpression.getRoot();
        stream.write(new VariableBinding.Start(attrValue, node)); //attrContent = <expr>
        if (!alreadyEscaped) {
            Expression contentExpression = valueExpression.withNode(new Identifier(attrValue));
            stream.write(new VariableBinding.Start(attrContent, adjustContext(compilerContext, contentExpression, markupContext,
                    ExpressionContext.ATTRIBUTE).getRoot()));
            stream.write(
                    new VariableBinding.Start(
                            shouldDisplayAttr,
                            new BinaryOperation(
                                    BinaryOperator.OR,
                                    new Identifier(attrContent),
                                    new BinaryOperation(BinaryOperator.EQ, new StringConstant("false"), new Identifier(attrValue))
                            )
                    )
            );

        } else {
            stream.write(
                    new VariableBinding.Start(
                            shouldDisplayAttr,
                            new BinaryOperation(
                                    BinaryOperator.OR,
                                    new Identifier(attrValue),
                                    new BinaryOperation(BinaryOperator.EQ, new StringConstant("false"), new Identifier(attrValue))
                            )
                    )
            );
        }
        stream.write(new Conditional.Start(shouldDisplayAttr, true)); // if (attrContent)

        emitAttributeStart(name);   //write("attrName");
        invoke.beforeAttributeValue(stream, name, node);
        stream.write(new VariableBinding.Start(isTrueVar, //isTrueAttr = (attrValue == true)
                new BinaryOperation(BinaryOperator.EQ, new Identifier(attrValue), BooleanConstant.TRUE)));
        stream.write(new Conditional.Start(isTrueVar, false)); //if (!isTrueAttr)
        emitAttributeValueStart(quoteChar); // write("='");
        if (!alreadyEscaped) {
            stream.write(new OutputVariable(attrContent)); //write(attrContent)
        } else {
            stream.write(new OutputVariable(attrValue)); // write(attrValue)
        }
        emitAttributeEnd(quoteChar); //write("'");
        stream.write(Conditional.END); //end if isTrueAttr
        stream.write(VariableBinding.END); //end scope for isTrueAttr
        invoke.afterAttributeValue(stream, name);
        stream.write(Conditional.END); //end if attrContent
        stream.write(VariableBinding.END); //end scope for attrContent
        if (!alreadyEscaped) {
            stream.write(VariableBinding.END);
        }
        stream.write(VariableBinding.END); //end scope for attrValue
    }


    private void emitAttributeStart(String name) {
        out(" " + name);
    }

    private void emitAttributeValueStart(char quoteChar) {
        char quote = '"';
        if (quoteChar != 0) {
            quote = quoteChar;
        }
        out("=");
        out(String.valueOf(quote));
    }

    private void emitAttributeEnd(char quoteChar) {
        char quote = '"';
        if (quoteChar != 0) {
            quote = quoteChar;
        }
        out(String.valueOf(quote));
    }


    public void onCloseTag(String markup) {
        ElementContext context = elementStack.pop();
        PluginInvoke invoke = context.pluginInvoke();
        invoke.afterChildren(stream);
        boolean selfClosingTag = StringUtils.isEmpty(markup);
        boolean slyTag = "sly".equalsIgnoreCase(context.getTagName());
        if (slyTag) {
            Patterns.beginStreamIgnore(stream);
        }
        invoke.beforeTagClose(stream, selfClosingTag);
        out(markup);
        invoke.afterTagClose(stream, selfClosingTag);
        if (slyTag) {
            Patterns.endStreamIgnore(stream);
        }
        invoke.afterElement(stream);
    }


    public void onText(String text) {
        String tag = currentElementTag();
        boolean explicitContextRequired = isExplicitContextRequired(tag);
        MarkupContext markupContext = (explicitContextRequired) ? null : MarkupContext.TEXT;
        outText(text, markupContext);
    }


    public void onComment(String markup) {
        if (!Syntax.isSightlyComment(markup)) {
            outText(markup, MarkupContext.COMMENT);
        }
    }


    public void onDataNode(String markup) {
        out(markup);
    }


    public void onDocType(String markup) {
        out(markup);
    }


    public void onDocumentFinished() {
        this.stream.close();
    }

    private void outText(String content, MarkupContext context) {
        Interpolation interpolation = expressionParser.parseInterpolation(content);
        if (context == null) {
            interpolation = requireContext(interpolation);
        }
        String text = tryAsSimpleText(interpolation);
        if (text != null) {
            out(text);
        } else {
            outExprNode(expressionWrapper.transform(interpolation, context, ExpressionContext.TEXT).getRoot());
        }
    }

    private Interpolation requireContext(Interpolation interpolation) {
        Interpolation result = new Interpolation();
        for (Fragment fragment : interpolation.getFragments()) {
            Fragment addedFragment;
            if (fragment.isString()) {
                addedFragment = fragment;
            } else {
                if (fragment.getExpression().containsOption(Syntax.CONTEXT_OPTION)) {
                    addedFragment = fragment;
                } else {
                    String currentTag = currentElementTag();
                    String warningMessage = String.format("Element %s requires that all expressions have an explicit context specified. " +
                            "The expression will be replaced with an empty string.", currentTag);
                    stream.write(new PushStream.Warning(warningMessage, fragment.getExpression().getRawText()));
                    addedFragment = new Fragment.Expr(new Expression(StringConstant.EMPTY));
                }
            }
            result.addFragment(addedFragment);
        }
        return result;
    }

    private Interpolation attributeChecked(String attributeName, Interpolation interpolation) {
        if (!MarkupUtils.isSensitiveAttribute(attributeName)) {
            return interpolation;
        }
        Interpolation newInterpolation = new Interpolation();
        for (Fragment fragment : interpolation.getFragments()) {
            Fragment addedFragment = fragment;
            if (fragment.isExpression()) {
                Expression expression = fragment.getExpression();
                if (!expression.containsOption(Syntax.CONTEXT_OPTION)) {
                    String warningMessage = String.format("Expressions within the value of attribute %s need to have an explicit context " +
                            "option. The expression will be replaced with an empty string.", attributeName);
                    stream.write(new PushStream.Warning(warningMessage, expression.getRawText()));
                    addedFragment = new Fragment.Text("");
                }
            }
            newInterpolation.addFragment(addedFragment);
        }
        return newInterpolation;
    }


    private void outExprNode(ExpressionNode node) {
        String variable = symbolGenerator.next();
        stream.write(new VariableBinding.Start(variable, node));
        stream.write(new OutputVariable(variable));
        stream.write(VariableBinding.END);
    }


    private String tryAsSimpleText(Interpolation interpolation) {
        if (interpolation.size() == 1) {
            Fragment fragment = interpolation.getFragment(0);
            if (fragment.isString()) {
                return fragment.getText();
            }
        } else if (interpolation.size() == 0) {
            return "";
        }
        return null;
    }

    private void out(String text) {
        stream.write(new OutText(text));
    }

    private void handlePlugin(String name, String value, ElementContext context) {
        PluginCallInfo callInfo = Syntax.parsePluginAttribute(name);
        if (callInfo != null) {
            Plugin plugin = obtainPlugin(callInfo.getName());
            ExpressionContext expressionContext = ExpressionContext.getContextForPlugin(plugin.name());
            Expression expr = expressionWrapper.transform(expressionParser.parseInterpolation(value), null, expressionContext);
            PluginInvoke invoke = plugin.invoke(expr, callInfo, compilerContext);
            context.addPlugin(invoke, plugin.priority());
            context.addPluginCall(name, callInfo, expr);
        }
    }

    private Plugin obtainPlugin(String name) {
        Plugin plugin = pluginRegistry.get(name);
        if (plugin == null) {
            throw new UnsupportedOperationException(String.format("Plugin %s does not exist", name));
        }
        return plugin;
    }

    private MarkupContext getAttributeMarkupContext(String attributeName) {
        if ("src".equalsIgnoreCase(attributeName) || "href".equalsIgnoreCase(attributeName)) {
            return MarkupContext.URI;
        }
        return MarkupContext.ATTRIBUTE;
    }

    private String currentElementTag() {
        if (elementStack.isEmpty()) {
            return null;
        }
        ElementContext current = elementStack.peek();
        return current.getTagName();
    }

    private boolean isExplicitContextRequired(String parentElementName) {
        return parentElementName != null &&
                ("script".equals(parentElementName) || "style".equals(parentElementName));
    }

    private Expression adjustContext(CompilerContext compilerContext, Expression expression, MarkupContext markupContext,
                                     ExpressionContext expressionContext) {
        ExpressionNode root = expression.getRoot();
        if (root instanceof RuntimeCall) {
            RuntimeCall runtimeCall = (RuntimeCall) root;
            if (runtimeCall.getFunctionName().equals(RuntimeFunction.XSS)) {
                return expression;
            }
        }
        return compilerContext.adjustToContext(expression, markupContext, expressionContext);
    }
}
