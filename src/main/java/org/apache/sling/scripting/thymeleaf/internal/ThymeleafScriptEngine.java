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

import java.io.Reader;
import java.io.Writer;
import java.util.Locale;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.IContext;

public final class ThymeleafScriptEngine extends AbstractSlingScriptEngine {

    private final ThymeleafScriptEngineFactory thymeleafScriptEngineFactory;

    private final Logger logger = LoggerFactory.getLogger(ThymeleafScriptEngine.class);

    public ThymeleafScriptEngine(final ThymeleafScriptEngineFactory thymeleafScriptEngineFactory) {
        super(thymeleafScriptEngineFactory);
        this.thymeleafScriptEngineFactory = thymeleafScriptEngineFactory;
    }

    @Override
    public Object eval(final Reader reader, final ScriptContext scriptContext) throws ScriptException {
        final Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        final SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);

        if (helper == null) {
            throw new ScriptException("SlingScriptHelper missing from bindings");
        }

        ResourceResolver resourceResolver = (ResourceResolver) scriptContext.getAttribute(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER, SlingScriptConstants.SLING_SCOPE);
        if (resourceResolver == null) {
            resourceResolver = helper.getScript().getScriptResource().getResourceResolver();
        }
        final Locale locale = helper.getResponse().getLocale();

        final String scriptName = helper.getScript().getScriptResource().getPath();
        final Writer writer = scriptContext.getWriter();

        bindings.put(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER, resourceResolver); // TODO #388

        try {
            final IContext context = new DefaultSlingContext(resourceResolver, locale, bindings);
            thymeleafScriptEngineFactory.getTemplateEngine().process(scriptName, context, writer);
        } catch (Exception e) {
            logger.error("Failure rendering Thymeleaf template '{}': {}", scriptName, e.getMessage());
            throw new ScriptException(e);
        }

        return null;
    }

}
