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
package org.apache.sling.scripting.thymeleaf;

import java.nio.charset.StandardCharsets;

import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.templatemode.StandardTemplateModeHandlers;
import org.thymeleaf.templateresolver.ITemplateResolutionValidity;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.NonCacheableTemplateResolutionValidity;
import org.thymeleaf.templateresolver.TemplateResolution;

public class SlingTemplateResolver implements ITemplateResolver {

    final SlingResourceResolver resourceResolver;

    public SlingTemplateResolver(final SlingResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public Integer getOrder() {
        return 0; // TODO make configurable
    }

    @Override
    public TemplateResolution resolveTemplate(TemplateProcessingParameters templateProcessingParameters) {
        final String templateName = templateProcessingParameters.getTemplateName();
        final String resourceName = templateName; // TODO
        final String characterEncoding = StandardCharsets.UTF_8.name();
        final String templateMode = StandardTemplateModeHandlers.LEGACYHTML5.getTemplateModeName(); // TODO make configurable
        final ITemplateResolutionValidity validity = new NonCacheableTemplateResolutionValidity(); // TODO
        return new TemplateResolution(templateName, resourceName, resourceResolver, characterEncoding, templateMode, validity);
    }

    @Override
    public void initialize() {
    }

}
