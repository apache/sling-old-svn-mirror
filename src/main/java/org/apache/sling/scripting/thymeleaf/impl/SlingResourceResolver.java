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

import java.io.InputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.thymeleaf.SlingContext;
import org.osgi.framework.Constants;
import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.context.IContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
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
    public InputStream getResourceAsStream(final TemplateProcessingParameters templateProcessingParameters, final String resourceName) {

        Validate.notNull(templateProcessingParameters, "Template Processing Parameters cannot be null");

        final IContext context = templateProcessingParameters.getContext();
        if (context instanceof SlingContext) {
            final SlingContext slingContext = (SlingContext) context;
            final ResourceResolver resourceResolver = slingContext.getResourceResolver();
            final Resource resource = resourceResolver.getResource(resourceName);
            return resource.adaptTo(InputStream.class);
        } else {
            throw new TemplateProcessingException("Cannot handle context: " + context.getClass().getName());
        }
    }

}
