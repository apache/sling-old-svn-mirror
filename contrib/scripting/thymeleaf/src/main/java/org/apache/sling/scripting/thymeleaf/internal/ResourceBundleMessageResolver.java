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

import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;

@Component(
    label = "Apache Sling Scripting Thymeleaf “Resource Bundle Message Resolver”",
    description = "resource bundle message resolver for Sling Scripting Thymeleaf",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "resource bundle message resolver for Sling Scripting Thymeleaf")
})
public class ResourceBundleMessageResolver implements IMessageResolver {

    @Reference
    private ResourceBundleProvider resourceBundleProvider;

    private Integer order;

    public static final int DEFAULT_ORDER = 0;

    @Property(intValue = DEFAULT_ORDER)
    public static final String ORDER_PARAMETER = "org.apache.sling.scripting.thymeleaf.internal.ResourceBundleMessageResolver.order";

    public static final Object[] EMPTY_MESSAGE_PARAMETERS = new Object[0];

    private final Logger logger = LoggerFactory.getLogger(ResourceBundleMessageResolver.class);

    public ResourceBundleMessageResolver() {
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

    private synchronized void configure(final ComponentContext componentContext) {
        final Dictionary properties = componentContext.getProperties();
        order = PropertiesUtil.toInteger(properties.get(ORDER_PARAMETER), DEFAULT_ORDER);
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
    public String resolveMessage(final ITemplateContext templateContext, final Class<?> origin, final String key, final Object[] messageParameters) {
        logger.debug("template context: {}, origin: {}, key: {}, message parameters: {}", templateContext, origin, key, messageParameters);
        // TODO can origin be useful with Sling i18n?
        final Locale locale = templateContext.getLocale();
        final ResourceBundle resourceBundle = resourceBundleProvider.getResourceBundle(locale);
        final String string = resourceBundle.getString(key);
        final MessageFormat messageFormat = new MessageFormat(string, locale);
        final String message = messageFormat.format((messageParameters != null ? messageParameters : EMPTY_MESSAGE_PARAMETERS));
        logger.debug("message: '{}'", message);
        return message;
    }

    @Override
    public String createAbsentMessageRepresentation(final ITemplateContext templateContext, final Class<?> origin, final String key, final Object[] messageParameters) {
        return key; // TODO make configurable
    }

}
