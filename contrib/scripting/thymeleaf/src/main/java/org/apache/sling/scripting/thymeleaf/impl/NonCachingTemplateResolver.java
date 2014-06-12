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
package org.apache.sling.scripting.thymeleaf.impl;

import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.thymeleaf.SlingTemplateModeHandler;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.templateresolver.ITemplateResolutionValidity;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.NonCacheableTemplateResolutionValidity;
import org.thymeleaf.templateresolver.TemplateResolution;

@Component(
    label = "Apache Sling Scripting Thymeleaf “Non-Caching Template Resolver”",
    description = "non-caching template resolver for Sling Scripting Thymeleaf",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "non-caching template resolver for Sling Scripting Thymeleaf")
})
public class NonCachingTemplateResolver implements ITemplateResolver {

    @Reference
    private IResourceResolver resourceResolver;

    @Reference(referenceInterface = SlingTemplateModeHandler.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    final private Set<SlingTemplateModeHandler> templateModeHandlers = new LinkedHashSet<SlingTemplateModeHandler>();

    private Integer order;

    public static final int DEFAULT_ORDER = 0;

    @Property(intValue = DEFAULT_ORDER)
    public static final String ORDER_PARAMETER = "org.apache.sling.scripting.thymeleaf.impl.NonCachingTemplateResolver.order";

    private String encoding;

    public static final String DEFAULT_ENCODING = "UTF-8";

    @Property(value = DEFAULT_ENCODING)
    public static final String ENCODING_PARAMETER = "org.apache.sling.scripting.thymeleaf.impl.NonCachingTemplateResolver.encoding";

    private final Logger logger = LoggerFactory.getLogger(NonCachingTemplateResolver.class);

    public NonCachingTemplateResolver() {
    }

    @Activate
    private void activate(final ComponentContext componentContext) {
        logger.debug("activate");
        configure(componentContext);
    }

    @Modified
    private void modified(final ComponentContext componentContext) {
        logger.debug("modified");
        configure(componentContext);
    }

    @Deactivate
    private void deactivate(final ComponentContext componentContext) {
        logger.debug("deactivate");
    }

    protected void bindTemplateModeHandlers(final SlingTemplateModeHandler templateModeHandler) {
        logger.debug("binding template mode handler '{}'", templateModeHandler.getTemplateModeName());
        templateModeHandlers.add(templateModeHandler);
    }

    protected void unbindTemplateModeHandlers(final SlingTemplateModeHandler templateModeHandler) {
        logger.debug("unbinding template mode handler '{}'", templateModeHandler.getTemplateModeName());
        templateModeHandlers.remove(templateModeHandler);
    }

    private synchronized void configure(final ComponentContext componentContext) {
        final Dictionary properties = componentContext.getProperties();
        order = PropertiesUtil.toInteger(properties.get(ORDER_PARAMETER), DEFAULT_ORDER);
        encoding = PropertiesUtil.toString(properties.get(ENCODING_PARAMETER), DEFAULT_ENCODING);
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public Integer getOrder() {
        return order;
    }

    @Override
    public TemplateResolution resolveTemplate(TemplateProcessingParameters templateProcessingParameters) {
        final String templateName = templateProcessingParameters.getTemplateName();
        final String resourceName = templateName; // TODO
        final String characterEncoding = encoding;
        final String templateMode = computeTemplateMode(templateName);
        final ITemplateResolutionValidity validity = new NonCacheableTemplateResolutionValidity();
        return new TemplateResolution(templateName, resourceName, resourceResolver, characterEncoding, templateMode, validity);
    }

    @Override
    public void initialize() {
    }

    protected String computeTemplateMode(final String templateName) {
        for (final SlingTemplateModeHandler templateModeHandler : templateModeHandlers) {
            final String templateMode = templateModeHandler.getTemplateModeName();
            logger.debug("template mode handler '{}' with patterns {}", templateMode, templateModeHandler.getPatternSpec().getPatterns());
            if (templateModeHandler.getPatternSpec().matches(templateName)) {
                logger.debug("using template mode '{}' for template '{}'", templateMode, templateName);
                return templateMode;
            }
        }
        return null;
    }

}
