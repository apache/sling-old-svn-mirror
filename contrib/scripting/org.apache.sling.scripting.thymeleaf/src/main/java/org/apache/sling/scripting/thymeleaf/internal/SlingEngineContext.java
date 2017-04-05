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

import java.util.Locale;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.thymeleaf.SlingContext;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.EngineContext;
import org.thymeleaf.engine.TemplateData;

public class SlingEngineContext extends EngineContext implements SlingContext {

    private final ResourceResolver resourceResolver;

    public SlingEngineContext(final ResourceResolver resourceResolver, final IEngineConfiguration configuration, final TemplateData templateData, final Map<String, Object> templateResolutionAttributes, final Locale locale, final Map<String, Object> variables) {
        super(configuration, templateData, templateResolutionAttributes, locale, variables);
        this.resourceResolver = resourceResolver;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

}
