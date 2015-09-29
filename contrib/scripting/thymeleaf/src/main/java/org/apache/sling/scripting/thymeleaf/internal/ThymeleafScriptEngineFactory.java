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

import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.script.ScriptEngine;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.Thymeleaf;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.messageresolver.IMessageResolver;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Component(
    label = "Apache Sling Scripting Thymeleaf “Script Engine Factory”",
    description = "scripting engine for Thymeleaf templates",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "scripting engine for Thymeleaf templates"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = false)
})
public final class ThymeleafScriptEngineFactory extends AbstractScriptEngineFactory {

    @Reference(referenceInterface = ITemplateResolver.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Set<ITemplateResolver> templateResolvers = new LinkedHashSet<ITemplateResolver>();

    @Reference(referenceInterface = IMessageResolver.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Set<IMessageResolver> messageResolvers = new LinkedHashSet<IMessageResolver>();

    @Reference(referenceInterface = IDialect.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Set<IDialect> dialects = new LinkedHashSet<IDialect>();

    private TemplateEngine templateEngine;

    public static final String DEFAULT_EXTENSION = "html";

    @Property(value = {DEFAULT_EXTENSION}, unbounded = PropertyUnbounded.ARRAY)
    public static final String EXTENSIONS_PARAMETER = "org.apache.sling.scripting.thymeleaf.extensions";

    public static final String DEFAULT_MIMETYPE = "text/html";

    @Property(value = {DEFAULT_MIMETYPE}, unbounded = PropertyUnbounded.ARRAY)
    public static final String MIMETYPES_PARAMETER = "org.apache.sling.scripting.thymeleaf.mimetypes";

    public static final String DEFAULT_NAME = "thymeleaf";

    @Property(value = {DEFAULT_NAME}, unbounded = PropertyUnbounded.ARRAY)
    public static final String NAMES_PARAMETER = "org.apache.sling.scripting.thymeleaf.names";

    private final Logger logger = LoggerFactory.getLogger(ThymeleafScriptEngineFactory.class);

    public ThymeleafScriptEngineFactory() {
    }

    @Activate
    private void activate(final ComponentContext componentContext) {
        logger.debug("activate");
        configure(componentContext);
        configureTemplateEngine();
    }

    @Modified
    private void modified(final ComponentContext componentContext) {
        logger.debug("modified");
        configure(componentContext);
        configureTemplateEngine();
    }

    @Deactivate
    private void deactivate(final ComponentContext componentContext) {
        logger.debug("deactivate");
        templateEngine = null;
    }

    protected synchronized void bindTemplateResolvers(final ITemplateResolver templateResolver) {
        logger.debug("binding template resolver '{}'", templateResolver.getName());
        templateResolvers.add(templateResolver);
        configureTemplateEngine();
    }

    protected synchronized void unbindTemplateResolvers(final ITemplateResolver templateResolver) {
        logger.debug("unbinding template resolver '{}'", templateResolver.getName());
        templateResolvers.remove(templateResolver);
        configureTemplateEngine();
    }

    protected synchronized void bindMessageResolvers(final IMessageResolver messageResolver) {
        logger.debug("binding message resolver '{}'", messageResolver.getName());
        messageResolvers.add(messageResolver);
        configureTemplateEngine();
    }

    protected synchronized void unbindMessageResolvers(final IMessageResolver messageResolver) {
        logger.debug("unbinding message resolver '{}'", messageResolver.getName());
        messageResolvers.remove(messageResolver);
        configureTemplateEngine();
    }

    protected synchronized void bindDialects(final IDialect dialect) {
        logger.debug("binding dialect '{}'", dialect.getName());
        dialects.add(dialect);
        configureTemplateEngine();
    }

    protected synchronized void unbindDialects(final IDialect dialect) {
        logger.debug("unbinding dialect '{}'", dialect.getName());
        dialects.remove(dialect);
        configureTemplateEngine();
    }

    private synchronized void configure(final ComponentContext componentContext) {
        final Dictionary properties = componentContext.getProperties();

        final String[] extensions = PropertiesUtil.toStringArray(properties.get(EXTENSIONS_PARAMETER), new String[]{DEFAULT_EXTENSION});
        setExtensions(extensions);

        final String[] mimeTypes = PropertiesUtil.toStringArray(properties.get(MIMETYPES_PARAMETER), new String[]{DEFAULT_MIMETYPE});
        setMimeTypes(mimeTypes);

        final String[] names = PropertiesUtil.toStringArray(properties.get(NAMES_PARAMETER), new String[]{DEFAULT_NAME});
        setNames(names);
    }

    // the configuration of the Thymeleaf TemplateEngine is static and we need to recreate on modification
    private synchronized void configureTemplateEngine() {
        logger.info("configure template engine");
        final TemplateEngine templateEngine = new TemplateEngine();
        if (templateResolvers.size() > 0) {
            templateEngine.setTemplateResolvers(templateResolvers);
        }
        if (messageResolvers.size() > 0) {
            templateEngine.setMessageResolvers(messageResolvers);
        }
        if (dialects.size() > 0) {
            templateEngine.setDialects(dialects);
            final IDialect standardDialect = new StandardDialect();
            templateEngine.addDialect(standardDialect);
        }
        // TODO
        // final ICacheManager cacheManager = null;
        // templateEngine.setCacheManager(cacheManager);
        this.templateEngine = templateEngine;
    }

    @Override
    public String getLanguageName() {
        return "Thymeleaf";
    }

    @Override
    public String getLanguageVersion() {
        return Thymeleaf.VERSION;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        logger.debug("get script engine for Thymeleaf");
        return new ThymeleafScriptEngine(this);
    }

    TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

}
