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

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.thymeleaf.TemplateModeProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.ITemplateResource;

@Component(
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Non-caching template resolver for Sling Scripting Thymeleaf",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = NonCachingTemplateResolverConfiguration.class
)
public class NonCachingTemplateResolver implements ITemplateResolver {

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        bind = "setTemplateModeProvider",
        unbind = "unsetTemplateModeProvider"
    )
    private volatile TemplateModeProvider templateModeProvider;

    private Integer order;

    private final Logger logger = LoggerFactory.getLogger(NonCachingTemplateResolver.class);

    public NonCachingTemplateResolver() {
    }

    public void setTemplateModeProvider(final TemplateModeProvider templateModeProvider) {
        logger.debug("setting template mode provider: {}", templateModeProvider);
    }

    public void unsetTemplateModeProvider(final TemplateModeProvider templateModeProvider) {
        logger.debug("unsetting template mode provider: {}", templateModeProvider);
    }

    @Activate
    private void activate(final NonCachingTemplateResolverConfiguration configuration) {
        logger.debug("activate");
        configure(configuration);
    }

    @Modified
    private void modified(final NonCachingTemplateResolverConfiguration configuration) {
        logger.debug("modified");
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivate");
    }

    private void configure(final NonCachingTemplateResolverConfiguration configuration) {
        order = configuration.order();
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
    public TemplateResolution resolveTemplate(final IEngineConfiguration engineConfiguration, final IContext context, final String ownerTemplate, final String template, final Map<String, Object> templateResolutionAttributes) {
        logger.debug("resolving template '{}'", template);
        final ResourceResolver resourceResolver = (ResourceResolver) context.getVariable(SlingBindings.RESOLVER);
        final Resource resource = resourceResolver.getResource(template);
        final ITemplateResource templateResource = new SlingTemplateResource(resource);
        final TemplateMode templateMode = templateModeProvider.provideTemplateMode(resource);
        logger.debug("using template mode {} for template '{}'", templateMode, template);
        return new TemplateResolution(templateResource, templateMode, NonCacheableCacheEntryValidity.INSTANCE);
    }

}
