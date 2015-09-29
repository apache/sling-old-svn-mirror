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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.scripting.thymeleaf.SlingContext;
import org.osgi.framework.Constants;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IWebVariablesMap;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.resource.CharArrayResource;
import org.thymeleaf.resource.IResource;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.util.Validate;

@Component(
    label = "Apache Sling Scripting Thymeleaf “Sling Resource Resolver”",
    description = "Sling resource resolver for Sling Scripting Thymeleaf",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Sling resource resolver for Sling Scripting Thymeleaf"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = false)
})
public class SlingResourceResolver implements IResourceResolver {

    public SlingResourceResolver() {
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public IResource resolveResource(final IEngineConfiguration engineConfiguration, final IContext context, final String resourceName, final String characterEncoding) {
        Validate.notNull(context, "context cannot be null");
        Validate.notNull(context, "resource name cannot be null");
        if (context instanceof SlingContext) {
            final SlingContext slingContext = (SlingContext) context;
            final ResourceResolver resourceResolver = slingContext.getResourceResolver();
            return resolveResource(resourceResolver, resourceName);
        } else if (context instanceof IWebVariablesMap) { // TODO Thymeleaf #388
            final IWebVariablesMap webVariablesMap = (IWebVariablesMap) context;
            final ResourceResolver resourceResolver = (ResourceResolver) webVariablesMap.getVariable(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER);
            return resolveResource(resourceResolver, resourceName);
        } else {
            final String message = String.format("Cannot handle context: %s", context.getClass().getName());
            throw new TemplateProcessingException(message);
        }
    }

    private IResource resolveResource(final ResourceResolver resourceResolver, final String resourceName) {
        final Resource resource = resourceResolver.getResource(resourceName);
        final InputStream inputStream = resource.adaptTo(InputStream.class);
        try {
            final char[] content = IOUtils.toCharArray(inputStream);
            return new CharArrayResource(resourceName, content);
        } catch (IOException e) {
            final String message = String.format("Cannot read from resource '%s'", resourceName);
            throw new TemplateProcessingException(message, e);
        }
    }

}
