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
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.context.IContext;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.util.PatternSpec;

@Component(
    label = "Apache Sling Scripting Thymeleaf “Non-Caching Template Resolver”",
    description = "non-caching template resolver for Sling Scripting Thymeleaf",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(
        name = Constants.SERVICE_VENDOR,
        value = "The Apache Software Foundation"
    ),
    @Property(
        name = Constants.SERVICE_DESCRIPTION,
        value = "non-caching template resolver for Sling Scripting Thymeleaf"
    )
})
public class NonCachingTemplateResolver implements ITemplateResolver {

    @Reference
    private IResourceResolver resourceResolver;

    private Integer order;

    public static final int DEFAULT_ORDER = 0;

    @Property(intValue = DEFAULT_ORDER)
    public static final String ORDER_PARAMETER = "org.apache.sling.scripting.thymeleaf.internal.NonCachingTemplateResolver.order";

    private String encoding;

    public static final String DEFAULT_ENCODING = "UTF-8";

    @Property(value = DEFAULT_ENCODING)
    public static final String ENCODING_PARAMETER = "org.apache.sling.scripting.thymeleaf.internal.NonCachingTemplateResolver.encoding";

    private final PatternSpec htmlPatternSpec = new PatternSpec();

    @Property(unbounded = PropertyUnbounded.ARRAY)
    public static final String HTML_PATTERNS_PARAMETER = "org.apache.sling.scripting.thymeleaf.internal.NonCachingTemplateResolver.htmlPatterns";

    private final PatternSpec xmlPatternSpec = new PatternSpec();

    @Property(unbounded = PropertyUnbounded.ARRAY)
    public static final String XML_PATTERNS_PARAMETER = "org.apache.sling.scripting.thymeleaf.internal.NonCachingTemplateResolver.xmlPatterns";

    private final PatternSpec textPatternSpec = new PatternSpec();

    @Property(unbounded = PropertyUnbounded.ARRAY)
    public static final String TEXT_PATTERNS_PARAMETER = "org.apache.sling.scripting.thymeleaf.internal.NonCachingTemplateResolver.textPatterns";

    private final PatternSpec javascriptPatternSpec = new PatternSpec();

    @Property(unbounded = PropertyUnbounded.ARRAY)
    public static final String JAVASCRIPT_PATTERNS_PARAMETER = "org.apache.sling.scripting.thymeleaf.internal.NonCachingTemplateResolver.javascriptPatterns";

    private final PatternSpec cssPatternSpec = new PatternSpec();

    @Property(unbounded = PropertyUnbounded.ARRAY)
    public static final String CSS_PATTERNS_PARAMETER = "org.apache.sling.scripting.thymeleaf.internal.NonCachingTemplateResolver.cssPatterns";

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

    private void configure(final ComponentContext componentContext) {
        final Dictionary properties = componentContext.getProperties();
        order = PropertiesUtil.toInteger(properties.get(ORDER_PARAMETER), DEFAULT_ORDER);
        encoding = PropertiesUtil.toString(properties.get(ENCODING_PARAMETER), DEFAULT_ENCODING);
        // HTML
        final String[] htmlPatterns = PropertiesUtil.toStringArray(properties.get(HTML_PATTERNS_PARAMETER), new String[]{});
        setPatterns(htmlPatterns, htmlPatternSpec);
        // XML
        final String[] xmlPatterns = PropertiesUtil.toStringArray(properties.get(XML_PATTERNS_PARAMETER), new String[]{});
        setPatterns(xmlPatterns, htmlPatternSpec);
        // TEXT
        final String[] textPatterns = PropertiesUtil.toStringArray(properties.get(TEXT_PATTERNS_PARAMETER), new String[]{});
        setPatterns(textPatterns, textPatternSpec);
        // JAVASCRIPT
        final String[] javascriptPatterns = PropertiesUtil.toStringArray(properties.get(JAVASCRIPT_PATTERNS_PARAMETER), new String[]{});
        setPatterns(javascriptPatterns, javascriptPatternSpec);
        // CSS
        final String[] cssPatterns = PropertiesUtil.toStringArray(properties.get(CSS_PATTERNS_PARAMETER), new String[]{});
        setPatterns(cssPatterns, cssPatternSpec);
    }

    private void setPatterns(final String[] strings, final PatternSpec patternSpec) {
        final Set<String> set = new HashSet<String>();
        Collections.addAll(set, strings);
        patternSpec.setPatterns(set);
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
    public TemplateResolution resolveTemplate(final IEngineConfiguration configuration, final IContext context, final String templateName) {
        final String resourceName = templateName;
        final String characterEncoding = encoding;
        final TemplateMode templateMode = computeTemplateMode(templateName);
        final ICacheEntryValidity cacheEntryValidity = new NonCacheableCacheEntryValidity();
        if (templateMode != null) {
            logger.debug("using template mode '{}' for template '{}'", templateMode, templateName);
            return new TemplateResolution(templateName, resourceName, resourceResolver, characterEncoding, templateMode, cacheEntryValidity);
        } else {
            logger.warn("no template mode for template '{}'", templateName);
            return null; // will fail at caller
        }
    }

    private TemplateMode computeTemplateMode(final String templateName) {
        if (htmlPatternSpec.matches(templateName)) {
            return TemplateMode.HTML;
        }
        if (xmlPatternSpec.matches(templateName)) {
            return TemplateMode.XML;
        }
        if (textPatternSpec.matches(templateName)) {
            return TemplateMode.TEXT;
        }
        if (javascriptPatternSpec.matches(templateName)) {
            return TemplateMode.JAVASCRIPT;
        }
        if (cssPatternSpec.matches(templateName)) {
            return TemplateMode.CSS;
        }
        return null;
    }

}
