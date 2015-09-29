/*
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
 */
package org.apache.sling.scripting.thymeleaf.internal.processor;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateProcessingContext;
import org.thymeleaf.dialect.IProcessorDialect;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

public abstract class SlingLocalVariableAttributeTagProcessor extends SlingHtmlAttributeTagProcessor {

    public static final String LOCAL_VARIABLE_PREFIX = "sling";

    public SlingLocalVariableAttributeTagProcessor(final IProcessorDialect processorDialect, final String dialectPrefix, final String attributeName, final int precedence) {
        super(processorDialect, dialectPrefix, attributeName, precedence, true);
    }

    protected abstract String getLocalVariableName();

    @Override
    protected void doProcess(final ITemplateProcessingContext templateProcessingContext, final IProcessableElementTag processableElementTag, final AttributeName attributeName, final String attributeValue, final String attributeTemplateName, final int attributeLine, final int attributeCol, final IElementTagStructureHandler structureHandler) {
        final IEngineConfiguration configuration = templateProcessingContext.getConfiguration();
        final IStandardExpressionParser expressionParser = StandardExpressions.getExpressionParser(configuration);
        final IStandardExpression expression = expressionParser.parseExpression(templateProcessingContext, attributeValue);
        final Object result = expression.execute(templateProcessingContext);
        structureHandler.setLocalVariable(getLocalVariableName(), result);
    }

}
