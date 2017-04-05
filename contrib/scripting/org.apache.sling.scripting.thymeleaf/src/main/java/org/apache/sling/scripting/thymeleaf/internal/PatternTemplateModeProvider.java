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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.thymeleaf.TemplateModeProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.templatemode.TemplateMode;

@Component(
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Pattern TemplateModeProvider for Sling Scripting Thymeleaf",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = PatternTemplateModeProviderConfiguration.class
)
public class PatternTemplateModeProvider implements TemplateModeProvider {

    private Pattern htmlPattern;

    private Pattern xmlPattern;

    private Pattern textPattern;

    private Pattern javascriptPattern;

    private Pattern cssPattern;

    private Pattern rawPattern;

    private final Logger logger = LoggerFactory.getLogger(PatternTemplateModeProvider.class);

    public PatternTemplateModeProvider() {
    }

    @Activate
    private void activate(final PatternTemplateModeProviderConfiguration configuration) {
        logger.debug("activating");
        configure(configuration);
    }

    @Modified
    private void modified(final PatternTemplateModeProviderConfiguration configuration) {
        logger.debug("modifying");
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivating");
    }

    private void configure(final PatternTemplateModeProviderConfiguration configuration) {
        // HTML
        htmlPattern = StringUtils.isNotBlank(configuration.htmlPattern()) ? Pattern.compile(configuration.htmlPattern()) : null;
        logger.debug("configured HTML pattern: {}", htmlPattern);
        // XML
        xmlPattern = StringUtils.isNotBlank(configuration.xmlPattern()) ? Pattern.compile(configuration.xmlPattern()) : null;
        logger.debug("configured XML pattern: {}", xmlPattern);
        // TEXT
        textPattern = StringUtils.isNotBlank(configuration.textPattern()) ? Pattern.compile(configuration.textPattern()) : null;
        logger.debug("configured TEXT pattern: {}", textPattern);
        // JAVASCRIPT
        javascriptPattern = StringUtils.isNotBlank(configuration.javascriptPattern()) ? Pattern.compile(configuration.javascriptPattern()) : null;
        logger.debug("configured JAVASCRIPT pattern: {}", javascriptPattern);
        // CSS
        cssPattern = StringUtils.isNotBlank(configuration.cssPattern()) ? Pattern.compile(configuration.cssPattern()) : null;
        logger.debug("configured CSS pattern: {}", cssPattern);
        // RAW
        rawPattern = StringUtils.isNotBlank(configuration.rawPattern()) ? Pattern.compile(configuration.rawPattern()) : null;
        logger.debug("configured RAW pattern: {}", rawPattern);
    }

    @Override
    public TemplateMode provideTemplateMode(final Resource resource) {
        final String path = resource.getPath();
        if (htmlPattern != null && htmlPattern.matcher(path).matches()) {
            return TemplateMode.HTML;
        }
        if (xmlPattern != null && xmlPattern.matcher(path).matches()) {
            return TemplateMode.XML;
        }
        if (textPattern != null && textPattern.matcher(path).matches()) {
            return TemplateMode.TEXT;
        }
        if (javascriptPattern != null && javascriptPattern.matcher(path).matches()) {
            return TemplateMode.JAVASCRIPT;
        }
        if (cssPattern != null && cssPattern.matcher(path).matches()) {
            return TemplateMode.CSS;
        }
        if (rawPattern != null && rawPattern.matcher(path).matches()) {
            return TemplateMode.RAW;
        }
        return null;
    }

}
