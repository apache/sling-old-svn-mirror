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
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.scripting.thymeleaf.AbsentMessageRepresentationProvider;
import org.apache.sling.scripting.thymeleaf.internal.ResourceBundleMessageResolverConfiguration.AbsentMessageRepresentationType;
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
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;

@Component(
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=ResourceBundle MessageResolver for Sling Scripting Thymeleaf",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = ResourceBundleMessageResolverConfiguration.class
)
public class ResourceBundleMessageResolver implements IMessageResolver {

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        bind = "setResourceBundleProvider",
        unbind = "unsetResourceBundleProvider"
    )
    private volatile ResourceBundleProvider resourceBundleProvider;

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        bind = "setAbsentMessageRepresentationProvider",
        unbind = "unsetAbsentMessageRepresentationProvider"
    )
    private volatile AbsentMessageRepresentationProvider absentMessageRepresentationProvider;

    private Integer order;

    private AbsentMessageRepresentationType absentMessageRepresentationType;

    public static final Object[] EMPTY_MESSAGE_PARAMETERS = new Object[0];

    private final Logger logger = LoggerFactory.getLogger(ResourceBundleMessageResolver.class);

    public ResourceBundleMessageResolver() {
    }

    public void setResourceBundleProvider(final ResourceBundleProvider resourceBundleProvider) {
        logger.info("setting resource bundle provider: {}", resourceBundleProvider);
    }

    public void unsetResourceBundleProvider(final ResourceBundleProvider resourceBundleProvider) {
        logger.info("unsetting resource bundle provider: {}", resourceBundleProvider);
    }

    public void setAbsentMessageRepresentationProvider(final AbsentMessageRepresentationProvider absentMessageRepresentationProvider) {
        logger.info("setting absent message representation provider: {}", absentMessageRepresentationProvider);
    }

    public void unsetAbsentMessageRepresentationProvider(final AbsentMessageRepresentationProvider absentMessageRepresentationProvider) {
        logger.info("unsetting absent message representation provider: {}", absentMessageRepresentationProvider);
    }

    @Activate
    private void activate(final ResourceBundleMessageResolverConfiguration configuration) {
        logger.debug("activate");
        configure(configuration);
    }

    @Modified
    private void modified(final ResourceBundleMessageResolverConfiguration configuration) {
        logger.debug("modified");
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivate");
    }

    private void configure(final ResourceBundleMessageResolverConfiguration configuration) {
        order = configuration.order();
        absentMessageRepresentationType = configuration.absentMessageRepresentationType();
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
        logger.debug("resolving message for '{}' ({}) with message parameters {}", key, origin, messageParameters);
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
        logger.debug("creating absent message representation for '{}' ({}) with message parameters {}", key, origin, messageParameters);
        String message = null;
        final AbsentMessageRepresentationProvider absentMessageRepresentationProvider = this.absentMessageRepresentationProvider;
        if (absentMessageRepresentationProvider == null) {
            switch (absentMessageRepresentationType) {
                case EMPTY:
                    message = "";
                    break;
                case BLANK:
                    message = " ";
                    break;
                case KEY:
                    message = key;
                    break;
            }
        } else {
            message = absentMessageRepresentationProvider.provideAbsentMessageRepresentation(templateContext, origin, key, messageParameters);
        }
        logger.debug("message: '{}'", message);
        return message;
    }

}
