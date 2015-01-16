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

import java.util.Collection;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.ElementContext;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.ExpressionParser;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.ExpressionWrapper;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.Fragment;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.Interpolation;
import org.apache.sling.scripting.sightly.impl.filter.Filter;
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperation;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperator;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BooleanConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.Identifier;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.StringConstant;
import org.apache.sling.scripting.sightly.impl.plugin.MarkupContext;
import org.apache.sling.scripting.sightly.impl.plugin.Plugin;
import org.apache.sling.scripting.sightly.impl.plugin.PluginCallInfo;
import org.apache.sling.scripting.sightly.impl.plugin.PluginInvoke;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutText;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.util.SymbolGenerator;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;
import org.apache.sling.scripting.sightly.impl.html.MarkupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for the markup handler
 */
public class MarkupHandler {

    private static final Logger log = LoggerFactory.getLogger(MarkupHandler.class);

    private final PushStream stream;
    private final SymbolGenerator symbolGenerator = new SymbolGenerator();
    private final ExpressionParser expressionParser = new ExpressionParser();
    private final Map<String, Plugin> pluginRegistry;
    private final CompilerContext compilerContext;
    private final ExpressionWrapper expressionWrapper;

    private final Stack<ElementContext> elementStack = new Stack<ElementContext>();

    public MarkupHandler(PushStream stream, Map<String, Plugin> pluginRegistry, Collection<Filter> filters) {
        this.stream = stream;
        this.pluginRegistry = pluginRegistry;
        this.expressionWrapper = new ExpressionWrapper(filters);
        this.compilerContext = new CompilerContext(symbolGenerator, expressionWrapper);
    }

    public void onOpenTagStart(String markup, String tagName) {
        ElementContext context = new ElementContext(tagName, markup);
        elementStack.push(context);
    }

    public void onAttribute(String name, String value) {
        ElementContext context = elementStack.peek();
        if (Syntax.isPluginAttribute(name)) {
            handlePlugin(name, StringUtils.defaultString(value, ""), context);
        } else {
            context.addAttribute(name, value);
        }
    }

    public void onOpenTagEnd(String markup) {
        ElementContext context = elementStack.peek();
        PluginInvoke invoke = context.pluginInvoke();
        invoke.beforeElement(stream, context.getTagName());
        invoke.beforeTagOpen(stream);
        out(context.getOpenTagStartMarkup());
        invoke.beforeAttributes(stream);
        traverseAttributes(context, invoke);
        invoke.afterAttributes(stream);
        out(markup);
        invoke.afterTagOpen(stream);
        invoke.beforeChildren(stream);
    }

    private void traverseAttributes(ElementContext context, PluginInvoke invoke) {
        for (Map.Entry<String, Object> attribute : context.getAttributes()) {
            String attrName = attribute.getKey();
            Object contentObj = attribute.getValue();
            if (contentObj == null || contentObj instanceof String) {
                String content = (String) contentObj;
                emitAttribute(attrName, content, invoke);
            } else if (contentObj instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry) contentObj;
                PluginCallInfo info = (PluginCallInfo) entry.getKey();
                Expression expression = (Expression) entry.getValue();
                invoke.onPluginCall(stream, info, expression);
            }
        }
    }

    private void emitAttribute(String name, String content, PluginInvoke invoke) {
        invoke.beforeAttribute(stream, name);
        if (content == null) {
            emitSimpleTextAttribute(name, null, invoke);
        } else {
            Interpolation interpolation = expressionParser.parseInterpolation(content);
            String text = tryAsSimpleText(interpolation);
            if (text != null) {
                emitSimpleTextAttribute(name, text, invoke);
            } else {
                emitExpressionAttribute(name, interpolation, invoke);
            }
        }
        invoke.afterAttribute(stream, name);
    }

    private void emitSimpleTextAttribute(String name, String textValue, PluginInvoke invoke) {
        emitAttributeStart(name);
        invoke.beforeAttributeValue(stream, name, new StringConstant(textValue));
        if (textValue != null) {
            emitAttributeValueStart();
            textValue = escapeQuotes(textValue);
            out(textValue);
            emitAttributeEnd();
        }
        invoke.afterAttributeValue(stream, name);
    }

    private String escapeQuotes(String textValue) {
        return textValue.replace("\"", "&quot;");
    }

    private void emitExpressionAttribute(String name, Interpolation interpolation, PluginInvoke invoke) {
        interpolation = attributeChecked(name, interpolation);
        if (interpolation.size() == 1) {
            emitSingleFragment(name, interpolation, invoke);
        } else {
            emitMultipleFragment(name, interpolation, invoke);
        }
    }

    private void emitMultipleFragment(String name, Interpolation interpolation, PluginInvoke invoke) {
        // Simplified algorithm for attribute output, which works when the interpolation is not of size 1. In this
        // case we are certain that the attribute value cannot be the boolean value true, so we can skip this test
        // altogether
        Expression expression = expressionWrapper.transform(interpolation, getAttributeMarkupContext(name));
        String attrContent = symbolGenerator.next("attrContent");
        String shouldDisplayAttr = symbolGenerator.next("shouldDisplayAttr");
        stream.emit(new VariableBinding.Start(attrContent, expression.getRoot()));
        stream.emit(
                new VariableBinding.Start(
                        shouldDisplayAttr,
                        new BinaryOperation(
                                BinaryOperator.OR,
                                new Identifier(attrContent),
                                new BinaryOperation(BinaryOperator.EQ, new StringConstant("false"), new Identifier(attrContent))
                        )
                )
        );
        stream.emit(new Conditional.Start(shouldDisplayAttr, true));
        emitAttributeStart(name);
        invoke.beforeAttributeValue(stream, name, expression.getRoot());
        emitAttributeValueStart();
        stream.emit(new OutVariable(attrContent));
        emitAttributeEnd();
        invoke.afterAttributeValue(stream, name);
        stream.emit(Conditional.END);
        stream.emit(VariableBinding.END);
        stream.emit(VariableBinding.END);
    }

    private void emitSingleFragment(String name, Interpolation interpolation, PluginInvoke invoke) {
        Expression valueExpression = expressionWrapper.transform(interpolation, null); //raw expression
        String attrValue = symbolGenerator.next("attrValue"); //holds the raw attribute value
        String attrContent = symbolGenerator.next("attrContent"); //holds the escaped attribute value
        String isTrueVar = symbolGenerator.next("isTrueAttr"); // holds the comparison (attrValue == true)
        String shouldDisplayAttr = symbolGenerator.next("shouldDisplayAttr");
        MarkupContext markupContext = getAttributeMarkupContext(name);
        Expression contentExpression = valueExpression.withNode(new Identifier(attrValue));
        ExpressionNode node = valueExpression.getRoot();
        stream.emit(new VariableBinding.Start(attrValue, node)); //attrContent = <expr>
        stream.emit(new VariableBinding.Start(attrContent, expressionWrapper.adjustToContext(contentExpression, markupContext).getRoot()));
        stream.emit(
                new VariableBinding.Start(
                        shouldDisplayAttr,
                        new BinaryOperation(
                                BinaryOperator.OR,
                                new Identifier(attrContent),
                                new BinaryOperation(BinaryOperator.EQ, new StringConstant("false"), new Identifier(attrValue))
                        )
                )
        );
        stream.emit(new Conditional.Start(shouldDisplayAttr, true)); // if (attrContent)
        emitAttributeStart(name);   //write("attrName");
        invoke.beforeAttributeValue(stream, name, node);
        stream.emit(new VariableBinding.Start(isTrueVar, //isTrueAttr = (attrValue == true)
                new BinaryOperation(BinaryOperator.EQ, new Identifier(attrValue), BooleanConstant.TRUE)));
        stream.emit(new Conditional.Start(isTrueVar, false)); //if (!isTrueAttr)
        emitAttributeValueStart(); // write("='");
        stream.emit(new OutVariable(attrContent)); //write(attrContent)
        emitAttributeEnd(); //write("'");
        stream.emit(Conditional.END); //end if isTrueAttr
        stream.emit(VariableBinding.END); //end scope for isTrueAttr
        invoke.afterAttributeValue(stream, name);
        stream.emit(Conditional.END); //end if attrContent
        stream.emit(VariableBinding.END);
        stream.emit(VariableBinding.END); //end scope for attrContent
        stream.emit(VariableBinding.END); //end scope for attrValue
    }


    private void emitAttributeStart(String name) {
        out(" " + name);
    }

    private void emitAttributeValueStart() {
        out("=\"");
    }

    private void emitAttributeEnd() {
        out("\"");
    }


    public void onCloseTag(String markup) {
        ElementContext context = elementStack.pop();
        PluginInvoke invoke = context.pluginInvoke();
        invoke.afterChildren(stream);
        boolean selfClosingTag = StringUtils.isEmpty(markup);
        invoke.beforeTagClose(stream, selfClosingTag);
        out(markup);
        invoke.afterTagClose(stream, selfClosingTag);
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
        this.stream.signalDone();
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
            outExprNode(expressionWrapper.transform(interpolation, context).getRoot());
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
                    log.warn("Element {} requires that all expressions have an explicit context specified. Expression will be " +
                            "replaced by the empty string", currentTag);
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
                    log.warn("All expressions within the value of attribute {} need to have an explicit context option. The expression will be erased.",
                            attributeName);
                    addedFragment = new Fragment.Text("");
                }
            }
            newInterpolation.addFragment(addedFragment);
        }
        return newInterpolation;
    }


    private void outExprNode(ExpressionNode node) {
        String variable = symbolGenerator.next();
        stream.emit(new VariableBinding.Start(variable, node));
        stream.emit(new OutVariable(variable));
        stream.emit(VariableBinding.END);
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
        stream.emit(new OutText(text));
    }

    private void handlePlugin(String name, String value, ElementContext context) {
        PluginCallInfo callInfo = Syntax.parsePluginAttribute(name);
        if (callInfo != null) {
            Plugin plugin = obtainPlugin(callInfo.getName());
            Expression expr = expressionWrapper.transform(
                    expressionParser.parseInterpolation(value), null);
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
}
