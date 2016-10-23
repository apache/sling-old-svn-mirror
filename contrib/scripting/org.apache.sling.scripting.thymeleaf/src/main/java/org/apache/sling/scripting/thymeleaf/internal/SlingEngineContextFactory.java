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
package org.apache.sling.scripting.thymeleaf.internal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.thymeleaf.SlingContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.IEngineContextFactory;
import org.thymeleaf.engine.TemplateData;

@Component(
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Sling EngineContextFactory for Sling Scripting Thymeleaf",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
public class SlingEngineContextFactory implements IEngineContextFactory {

    @Override
    public IEngineContext createEngineContext(final IEngineConfiguration configuration, final TemplateData templateData, final Map<String, Object> templateResolutionAttributes, final IContext context) {
        if (context instanceof SlingContext) {
            // TODO web context
            final SlingContext slingContext = (SlingContext) context;
            final ResourceResolver resourceResolver = slingContext.getResourceResolver();
            final Locale locale = context.getLocale();
            final Set<String> variableNames = context.getVariableNames();
            final Map<String, Object> variables = new LinkedHashMap<>(variableNames.size() + 1, 1.0f);
            for (final String variableName : variableNames) {
                variables.put(variableName, context.getVariable(variableName));
            }
            return new SlingEngineContext(resourceResolver, configuration, templateData, templateResolutionAttributes, locale, variables);
        } else {
            throw new IllegalStateException("context is not an instance of SlingContext");
        }
    }

}
