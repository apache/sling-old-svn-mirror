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
package org.apache.sling.scripting.sightly.impl.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperation;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperator;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BooleanConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.Identifier;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.PropertyAccess;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Conditional;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Loop;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutText;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.OutVariable;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.Patterns;
import org.apache.sling.scripting.sightly.impl.compiler.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.impl.compiler.common.DefaultPluginInvoke;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.CompilerContext;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;
import org.apache.sling.scripting.sightly.impl.filter.ExpressionContext;
import org.apache.sling.scripting.sightly.impl.html.MarkupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for the attribute plugin
 */
@Component
@Service
@Properties({
        @Property(name = "service.description", value = "Sightly Resource Block Plugin"),
        @Property(name = Plugin.SCR_PROP_NAME_BLOCK_NAME, value = "attribute"),
        @Property(name = Plugin.SCR_PROP_NAME_PRIORITY, intValue = 150)
})
public class AttributePlugin extends PluginComponent {

    private static final Logger log = LoggerFactory.getLogger(AttributePlugin.class);

    @Override
    public PluginInvoke invoke(Expression expression, PluginCallInfo callInfo, CompilerContext compilerContext) {
        String attributeName = decodeAttributeName(callInfo);
        if (attributeName != null && MarkupUtils.isSensitiveAttribute(attributeName)) {
            log.warn("Refusing to generate attribute {} for security reasons", attributeName);
            return new DefaultPluginInvoke(); //no-op invocation
        }
        return (attributeName != null)
                ? new SingleAttributeInvoke(attributeName, expression, compilerContext)
                : new MultiAttributeInvoke(expression.getRoot(), compilerContext);
    }

    private String decodeAttributeName(PluginCallInfo info) {
        String[] arguments = info.getArguments();
        if (arguments.length == 0) {
            return null;
        }
        return StringUtils.join(arguments, '-');
    }

    private final class SingleAttributeInvoke extends DefaultPluginInvoke {
        private final String attributeName;
        private final String isTrueValue;
        private final String escapedAttrValue;
        private final String shouldDisplayAttribute;

        private boolean writeAtEnd = true;
        private boolean beforeCall = true;
        private final String attrValue;
        private final ExpressionNode node;
        private final ExpressionNode contentNode;

        private SingleAttributeInvoke(String attributeName, Expression expression, CompilerContext compilerContext) {
            this.attributeName = attributeName;
            this.attrValue = compilerContext.generateVariable("attrValue_" + attributeName);
            this.escapedAttrValue = compilerContext.generateVariable("attrValueEscaped_" + attributeName);
            this.isTrueValue = compilerContext.generateVariable("isTrueValue_" + attributeName);
            this.shouldDisplayAttribute = compilerContext.generateVariable("shouldDisplayAttr_" + attributeName);
            this.node = expression.getRoot();
            if (!expression.containsOption(Syntax.CONTEXT_OPTION)) {
                this.contentNode = escapeNodeWithHint(compilerContext, new Identifier(attrValue), MarkupContext.ATTRIBUTE,
                        new StringConstant(attributeName));
            } else {
                this.contentNode = new Identifier(attrValue);
            }
        }

        @Override
        public void beforeAttribute(PushStream stream, String attributeName) {
            if (attributeName.equals(this.attributeName)) {
                if (beforeCall) {
                    emitStart(stream);
                }
                writeAtEnd = false;
            }
        }

        @Override
        public void beforeAttributeValue(PushStream stream, String attributeName, ExpressionNode attributeValue) {
            if (attributeName.equals(this.attributeName) && beforeCall) {
                emitWrite(stream);
                Patterns.beginStreamIgnore(stream);
            }
        }

        @Override
        public void afterAttributeValue(PushStream stream, String attributeName) {
            if (attributeName.equals(this.attributeName) && beforeCall) {
                Patterns.endStreamIgnore(stream);
            }
        }

        @Override
        public void afterAttribute(PushStream stream, String attributeName) {
            if (attributeName.equals(this.attributeName) && beforeCall) {
                emitEnd(stream);
            }
        }

        @Override
        public void afterAttributes(PushStream stream) {
            if (writeAtEnd) {
                emitStart(stream);
                stream.emit(new OutText(" " + this.attributeName));
                emitWrite(stream);
                emitEnd(stream);
            }
        }

        @Override
        public void onPluginCall(PushStream stream, PluginCallInfo callInfo, Expression expression) {
            if ("attribute".equals(callInfo.getName())) {
                String attributeName = decodeAttributeName(callInfo);
                if (this.attributeName.equals(attributeName)) {
                    beforeCall = false;
                }
            }
        }

        private void emitStart(PushStream stream) {
            stream.emit(new VariableBinding.Start(attrValue, node));
            stream.emit(new VariableBinding.Start(escapedAttrValue, contentNode));
            stream.emit(
                    new VariableBinding.Start(
                            shouldDisplayAttribute,
                            new BinaryOperation(
                                    BinaryOperator.OR,
                                    new Identifier(escapedAttrValue),
                                    new BinaryOperation(BinaryOperator.EQ, new StringConstant("false"), new Identifier(attrValue))
                            )
                    )
            );
            stream.emit(new Conditional.Start(shouldDisplayAttribute, true));
        }

        private void emitWrite(PushStream stream) {
            stream.emit(new VariableBinding.Start(isTrueValue,
                    new BinaryOperation(BinaryOperator.EQ,
                            new Identifier(attrValue),
                            BooleanConstant.TRUE)));
            stream.emit(new Conditional.Start(isTrueValue, false));
            stream.emit(new OutText("=\""));
            stream.emit(new OutVariable(escapedAttrValue));
            stream.emit(new OutText("\""));
            stream.emit(Conditional.END);
            stream.emit(VariableBinding.END);
        }

        private void emitEnd(PushStream stream) {
            stream.emit(Conditional.END);
            stream.emit(VariableBinding.END);
            stream.emit(VariableBinding.END);
            stream.emit(VariableBinding.END);
        }
    }

    private final class MultiAttributeInvoke extends DefaultPluginInvoke {

        private final ExpressionNode attrMap;
        private final String attrMapVar;
        private final CompilerContext compilerContext;
        private boolean beforeCall = true;
        private final Set<String> ignored = new HashSet<String>();

        private MultiAttributeInvoke(ExpressionNode attrMap, CompilerContext context) {
            this.attrMap = attrMap;
            this.compilerContext = context;
            this.attrMapVar = context.generateVariable("attrMap");
        }

        @Override
        public void beforeAttributes(PushStream stream) {
            stream.emit(new VariableBinding.Start(attrMapVar, attrMap));
        }

        @Override
        public void beforeAttribute(PushStream stream, String attributeName) {
            ignored.add(attributeName);
            if (beforeCall) {
                String attrNameVar = compilerContext.generateVariable("attrName_" + attributeName);
                String attrValue = compilerContext.generateVariable("mapContains_" + attributeName);
                stream.emit(new VariableBinding.Start(attrNameVar, new StringConstant(attributeName)));
                stream.emit(new VariableBinding.Start(attrValue, attributeValueNode(new StringConstant(attributeName))));
                writeAttribute(stream, attrNameVar, attrValue);
                stream.emit(new Conditional.Start(attrValue, false));
            }
        }

        @Override
        public void afterAttribute(PushStream stream, String attributeName) {
            if (beforeCall) {
                stream.emit(Conditional.END);
                stream.emit(VariableBinding.END);
                stream.emit(VariableBinding.END);
            }
        }

        @Override
        public void onPluginCall(PushStream stream, PluginCallInfo callInfo, Expression expression) {
            if ("attribute".equals(callInfo.getName())) {
                String attrName = decodeAttributeName(callInfo);
                if (attrName == null) {
                    beforeCall = false;
                } else {
                    if (!beforeCall) {
                        ignored.add(attrName);
                    }
                }
            }
        }

        @Override
        public void afterAttributes(PushStream stream) {
            HashMap<String, ExpressionNode> ignoredLiteralMap = new HashMap<String, ExpressionNode>();
            for (String attr : ignored) {
                ignoredLiteralMap.put(attr, new BooleanConstant(true));
            }
            MapLiteral ignoredLiteral = new MapLiteral(ignoredLiteralMap);
            String ignoredVar = compilerContext.generateVariable("ignoredAttributes");
            stream.emit(new VariableBinding.Start(ignoredVar, ignoredLiteral));
            String attrNameVar = compilerContext.generateVariable("attrName");
            String attrNameEscaped = compilerContext.generateVariable("attrNameEscaped");
            String attrIndex = compilerContext.generateVariable("attrIndex");
            stream.emit(new Loop.Start(attrMapVar, attrNameVar, attrIndex));
            stream.emit(new VariableBinding.Start(attrNameEscaped,
                    escapeNode(new Identifier(attrNameVar), MarkupContext.ATTRIBUTE_NAME, null)));
            stream.emit(new Conditional.Start(attrNameEscaped, true));
            String isIgnoredAttr = compilerContext.generateVariable("isIgnoredAttr");
            stream.emit(
                    new VariableBinding.Start(isIgnoredAttr, new PropertyAccess(new Identifier(ignoredVar), new Identifier(attrNameVar))));
            stream.emit(new Conditional.Start(isIgnoredAttr, false));
            String attrContent = compilerContext.generateVariable("attrContent");
            stream.emit(new VariableBinding.Start(attrContent, attributeValueNode(new Identifier(attrNameVar))));
            writeAttribute(stream, attrNameEscaped, attrContent);
            stream.emit(VariableBinding.END); //end of attrContent
            stream.emit(Conditional.END);
            stream.emit(VariableBinding.END);
            stream.emit(Conditional.END);
            stream.emit(VariableBinding.END);
            stream.emit(Loop.END);
            stream.emit(VariableBinding.END);
            stream.emit(VariableBinding.END);
        }

        private void writeAttribute(PushStream stream, String attrNameVar, String attrContentVar) {
            String escapedContent = compilerContext.generateVariable("attrContentEscaped");
            String shouldDisplayAttribute = compilerContext.generateVariable("shouldDisplayAttr");
            stream.emit(new VariableBinding.Start(escapedContent,
                    escapedExpression(new Identifier(attrContentVar), new Identifier(attrNameVar))));
            stream.emit(
                    new VariableBinding.Start(
                            shouldDisplayAttribute,
                            new BinaryOperation(
                                    BinaryOperator.OR,
                                    new Identifier(escapedContent),
                                    new BinaryOperation(BinaryOperator.EQ, new StringConstant("false"), new Identifier(attrContentVar))
                            )
                    )
            );
            stream.emit(new Conditional.Start(shouldDisplayAttribute, true));
            stream.emit(new OutText(" "));   //write("attrName");
            writeAttributeName(stream, attrNameVar);
            writeAttributeValue(stream, escapedContent, attrContentVar);
            stream.emit(Conditional.END);
            stream.emit(VariableBinding.END);
            stream.emit(VariableBinding.END);
        }

        private void writeAttributeName(PushStream stream, String attrNameVar) {
            stream.emit(new OutVariable(attrNameVar));
        }

        private void writeAttributeValue(PushStream stream, String escapedContent, String attrContentVar) {

            String isTrueVar = compilerContext.generateVariable("isTrueAttr"); // holds the comparison (attrValue == true)
            stream.emit(new VariableBinding.Start(isTrueVar, //isTrueAttr = (attrContent == true)
                    new BinaryOperation(BinaryOperator.EQ, new Identifier(attrContentVar), BooleanConstant.TRUE)));
            stream.emit(new Conditional.Start(isTrueVar, false)); //if (!isTrueAttr)
            stream.emit(new OutText("=\""));

            stream.emit(new OutVariable(escapedContent)); //write(escapedContent)

            stream.emit(new OutText("\""));
            stream.emit(Conditional.END); //end if isTrueAttr
            stream.emit(VariableBinding.END); //end scope for isTrueAttr
        }

        private ExpressionNode attributeValueNode(ExpressionNode attributeNameNode) {
            return new PropertyAccess(new Identifier(attrMapVar), attributeNameNode);
        }

        private ExpressionNode escapedExpression(ExpressionNode original, ExpressionNode hint) {
            return escapeNode(original, MarkupContext.ATTRIBUTE, hint);
        }

        private ExpressionNode escapeNode(ExpressionNode node, MarkupContext markupContext, ExpressionNode hint) {
            return escapeNodeWithHint(compilerContext, node, markupContext, hint);
        }
    }

    private static ExpressionNode escapeNodeWithHint(CompilerContext compilerContext, ExpressionNode node, MarkupContext markupContext,
                                                     ExpressionNode hint) {
        if (hint != null) {
            //todo: this is not the indicated way to escape via XSS. Correct after modifying the compiler context API
            return new RuntimeCall("xss", node, new StringConstant(markupContext.getName()), hint);
        }
        return compilerContext.adjustToContext(new Expression(node), markupContext, ExpressionContext.ATTRIBUTE).getRoot();
    }
}
