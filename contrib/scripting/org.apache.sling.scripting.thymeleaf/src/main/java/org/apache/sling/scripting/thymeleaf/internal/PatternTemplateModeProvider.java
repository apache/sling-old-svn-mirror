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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
import org.thymeleaf.util.PatternSpec;

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

    private final PatternSpec htmlPatternSpec = new PatternSpec();

    private final PatternSpec xmlPatternSpec = new PatternSpec();

    private final PatternSpec textPatternSpec = new PatternSpec();

    private final PatternSpec javascriptPatternSpec = new PatternSpec();

    private final PatternSpec cssPatternSpec = new PatternSpec();

    private final PatternSpec rawPatternSpec = new PatternSpec();

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
        setPatterns(configuration.htmlPatterns(), htmlPatternSpec);
        logger.debug("configured HTML patterns: {}", htmlPatternSpec.getPatterns());
        // XML
        setPatterns(configuration.xmlPatterns(), xmlPatternSpec);
        logger.debug("configured XML patterns: {}", xmlPatternSpec.getPatterns());
        // TEXT
        setPatterns(configuration.textPatterns(), textPatternSpec);
        logger.debug("configured TEXT patterns: {}", textPatternSpec.getPatterns());
        // JAVASCRIPT
        setPatterns(configuration.javascriptPatterns(), javascriptPatternSpec);
        logger.debug("configured JAVASCRIPT patterns: {}", javascriptPatternSpec.getPatterns());
        // CSS
        setPatterns(configuration.cssPatterns(), cssPatternSpec);
        logger.debug("configured CSS patterns: {}", cssPatternSpec.getPatterns());
        // RAW
        setPatterns(configuration.rawPatterns(), rawPatternSpec);
        logger.debug("configured RAW patterns: {}", rawPatternSpec.getPatterns());
    }

    private void setPatterns(final String[] strings, final PatternSpec patternSpec) {
        final Set<String> set = new HashSet<String>();
        if (strings != null) {
            Collections.addAll(set, strings);
        }
        patternSpec.setPatterns(set);
    }

    @Override
    public TemplateMode provideTemplateMode(final Resource resource) {
        final String path = resource.getPath();
        if (htmlPatternSpec.matches(path)) {
            return TemplateMode.HTML;
        }
        if (xmlPatternSpec.matches(path)) {
            return TemplateMode.XML;
        }
        if (textPatternSpec.matches(path)) {
            return TemplateMode.TEXT;
        }
        if (javascriptPatternSpec.matches(path)) {
            return TemplateMode.JAVASCRIPT;
        }
        if (cssPatternSpec.matches(path)) {
            return TemplateMode.CSS;
        }
        if (rawPatternSpec.matches(path)) {
            return TemplateMode.RAW;
        }
        return null;
    }

}
