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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
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
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.cache.ICacheManager;
import org.thymeleaf.context.IEngineContextFactory;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.linkbuilder.ILinkBuilder;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.messageresolver.IMessageResolver;
import org.thymeleaf.messageresolver.StandardMessageResolver;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templateparser.markup.decoupled.IDecoupledTemplateLogicResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Component(
    service = ScriptEngineFactory.class,
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Scripting engine for Thymeleaf templates",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = ThymeleafScriptEngineFactoryConfiguration.class
)
public final class ThymeleafScriptEngineFactory extends AbstractScriptEngineFactory {

    @Reference(
        cardinality = ReferenceCardinality.AT_LEAST_ONE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "addTemplateResolver",
        unbind = "removeTemplateResolver"
    )
    private List<ITemplateResolver> templateResolvers;

    @Reference(
        cardinality = ReferenceCardinality.AT_LEAST_ONE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "addMessageResolver",
        unbind = "removeMessageResolver"
    )
    private List<IMessageResolver> messageResolvers;

    @Reference(
        cardinality = ReferenceCardinality.AT_LEAST_ONE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "addDialect",
        unbind = "removeDialect"
    )
    private List<IDialect> dialects;

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "addLinkBuilder",
        unbind = "removeLinkBuilder"
    )
    private List<ILinkBuilder> linkBuilders;

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        bind = "setDecoupledTemplateLogicResolver",
        unbind = "unsetDecoupledTemplateLogicResolver"
    )
    private volatile IDecoupledTemplateLogicResolver decoupledTemplateLogicResolver;

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        bind = "setCacheManager",
        unbind = "unsetCacheManager"
    )
    private volatile ICacheManager cacheManager;

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        bind = "setEngineContextFactory",
        unbind = "unsetEngineContextFactory"
    )
    private volatile IEngineContextFactory engineContextFactory;

    private ThymeleafScriptEngineFactoryConfiguration configuration;

    private BundleContext bundleContext;

    private TemplateEngine templateEngine;

    private ServiceRegistration<ITemplateEngine> serviceRegistration;

    private final Object lock = new Object();

    private final Logger logger = LoggerFactory.getLogger(ThymeleafScriptEngineFactory.class);

    public ThymeleafScriptEngineFactory() {
    }

    protected void addTemplateResolver(final ITemplateResolver templateResolver) {
        synchronized (lock) {
            logger.debug("adding template resolver '{}'", templateResolver.getName());
            if (templateEngine == null || templateEngine.isInitialized()) {
                serviceTemplateEngine();
            } else {
                templateEngine.addTemplateResolver(templateResolver);
            }
        }
    }

    protected void removeTemplateResolver(final ITemplateResolver templateResolver) {
        synchronized (lock) {
            logger.debug("removing template resolver '{}'", templateResolver.getName());
            serviceTemplateEngine();
        }
    }

    protected void addMessageResolver(final IMessageResolver messageResolver) {
        synchronized (lock) {
            logger.debug("adding message resolver '{}'", messageResolver.getName());
            if (templateEngine == null || templateEngine.isInitialized()) {
                serviceTemplateEngine();
            } else {
                templateEngine.addMessageResolver(messageResolver);
            }
        }
    }

    protected void removeMessageResolver(final IMessageResolver messageResolver) {
        synchronized (lock) {
            logger.debug("removing message resolver '{}'", messageResolver.getName());
            serviceTemplateEngine();
        }
    }

    protected void addDialect(final IDialect dialect) {
        synchronized (lock) {
            logger.debug("adding dialect '{}'", dialect.getName());
            if (templateEngine == null || templateEngine.isInitialized()) {
                serviceTemplateEngine();
            } else {
                templateEngine.addDialect(dialect);
            }
        }
    }

    protected void removeDialect(final IDialect dialect) {
        synchronized (lock) {
            logger.debug("removing dialect '{}'", dialect.getName());
            serviceTemplateEngine();
        }
    }

    protected void addLinkBuilder(final ILinkBuilder linkBuilder) {
        synchronized (lock) {
            logger.debug("adding link builder '{}'", linkBuilder.getName());
            if (templateEngine == null || templateEngine.isInitialized()) {
                serviceTemplateEngine();
            } else {
                templateEngine.addLinkBuilder(linkBuilder);
            }
        }
    }

    protected void removeLinkBuilder(final ILinkBuilder linkBuilder) {
        synchronized (lock) {
            logger.debug("removing link builder '{}'", linkBuilder.getName());
            serviceTemplateEngine();
        }
    }

    protected void setDecoupledTemplateLogicResolver(final IDecoupledTemplateLogicResolver decoupledTemplateLogicResolver) {
        synchronized (lock) {
            logger.debug("setting decoupled template logic resolver '{}'", decoupledTemplateLogicResolver.getClass().getName());
            if (templateEngine == null || templateEngine.isInitialized()) {
                serviceTemplateEngine();
            } else {
                templateEngine.setDecoupledTemplateLogicResolver(decoupledTemplateLogicResolver);
            }
        }
    }

    protected void unsetDecoupledTemplateLogicResolver(final IDecoupledTemplateLogicResolver decoupledTemplateLogicResolver) {
        synchronized (lock) {
            logger.debug("unsetting decoupled template logic resolver '{}'", decoupledTemplateLogicResolver.getClass().getName());
            serviceTemplateEngine();
        }
    }

    protected void setCacheManager(final ICacheManager cacheManager) {
        synchronized (lock) {
            logger.debug("setting cache manager '{}'", cacheManager.getClass().getName());
            if (templateEngine == null || templateEngine.isInitialized()) {
                serviceTemplateEngine();
            } else {
                templateEngine.setCacheManager(cacheManager);
            }
        }
    }

    protected void unsetCacheManager(final ICacheManager cacheManager) {
        synchronized (lock) {
            logger.debug("unsetting cache manager '{}'", cacheManager.getClass().getName());
            serviceTemplateEngine();
        }
    }

    protected void setEngineContextFactory(final IEngineContextFactory engineContextFactory) {
        synchronized (lock) {
            logger.debug("setting engine context factory '{}'", engineContextFactory.getClass().getName());
            if (templateEngine == null || templateEngine.isInitialized()) {
                serviceTemplateEngine();
            } else {
                templateEngine.setEngineContextFactory(engineContextFactory);
            }
        }
    }

    protected void unsetEngineContextFactory(final IEngineContextFactory engineContextFactory) {
        synchronized (lock) {
            logger.debug("unsetting engine context factory '{}'", engineContextFactory.getClass().getName());
            serviceTemplateEngine();
        }
    }

    @Activate
    private void activate(final ThymeleafScriptEngineFactoryConfiguration configuration, final BundleContext bundleContext) {
        logger.debug("activate");
        this.configuration = configuration;
        this.bundleContext = bundleContext;
        configure(configuration);
        setupTemplateEngine();
        registerTemplateEngine();
    }

    @Modified
    private void modified(final ThymeleafScriptEngineFactoryConfiguration configuration) {
        logger.debug("modified");
        this.configuration = configuration;
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivate");
        unregisterTemplateEngine();
        templateEngine = null;
        bundleContext = null;
    }

    private void configure(final ThymeleafScriptEngineFactoryConfiguration configuration) {
        setExtensions(configuration.extensions());
        setMimeTypes(configuration.mimeTypes());
        setNames(configuration.names());
    }

    @Override
    public String getLanguageName() {
        return "Thymeleaf";
    }

    @Override
    public String getLanguageVersion() {
        try {
            final Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/org/thymeleaf/thymeleaf.properties"));
            return properties.getProperty("version");
        } catch (Exception e) {
            logger.error("error reading version from thymeleaf.properties", e);
            return ""; // null breaks output of web console
        }
    }

    @Override
    public ScriptEngine getScriptEngine() {
        logger.debug("get script engine for Thymeleaf");
        return new ThymeleafScriptEngine(this);
    }

    private void serviceTemplateEngine() {
        unregisterTemplateEngine();
        setupTemplateEngine();
        registerTemplateEngine();
    }

    private void setupTemplateEngine() {
        logger.info("setting up new template engine");
        templateEngine = null;
        // setup template engine
        final TemplateEngine templateEngine = new TemplateEngine();
        // Template Resolvers
        if (this.templateResolvers != null) {
            final Set<ITemplateResolver> templateResolvers = new HashSet<>(this.templateResolvers);
            templateEngine.setTemplateResolvers(templateResolvers);
        }
        // Message Resolvers
        if (this.messageResolvers != null) {
            final Set<IMessageResolver> messageResolvers = new HashSet<>(this.messageResolvers);
            templateEngine.setMessageResolvers(messageResolvers);
        }
        if (configuration.useStandardMessageResolver()) {
            final IMessageResolver standardMessageResolver = new StandardMessageResolver();
            templateEngine.addMessageResolver(standardMessageResolver);
        }
        // Link Builders
        if (this.linkBuilders != null) {
            final Set<ILinkBuilder> linkBuilders = new HashSet<>(this.linkBuilders);
            templateEngine.setLinkBuilders(linkBuilders);
        }
        if (configuration.useStandardLinkBuilder()) {
            final ILinkBuilder standardLinkBuilder = new StandardLinkBuilder();
            templateEngine.addLinkBuilder(standardLinkBuilder);
        }
        // Dialects
        if (this.dialects != null) {
            final Set<IDialect> dialects = new HashSet<>(this.dialects);
            templateEngine.setDialects(dialects);
        }
        if (configuration.useStandardDialect()) {
            final IDialect standardDialect = new StandardDialect();
            templateEngine.addDialect(standardDialect);
        }
        // Decoupled Template Logic Resolver
        if (!configuration.useStandardDecoupledTemplateLogicResolver()) {
            templateEngine.setDecoupledTemplateLogicResolver(decoupledTemplateLogicResolver);
        }
        // Cache Manager
        if (!configuration.useStandardCacheManager()) {
            templateEngine.setCacheManager(cacheManager);
        }
        // Engine Context Factory
        if (!configuration.useStandardEngineContextFactory()) {
            templateEngine.setEngineContextFactory(engineContextFactory);
        }
        //
        this.templateEngine = templateEngine;
    }

    private void registerTemplateEngine() {
        if (templateEngine == null || templateEngine.getTemplateResolvers().size() == 0 || templateEngine.getMessageResolvers().size() == 0 || templateEngine.getDialects().size() == 0) {
            return;
        }
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Thymeleaf TemplateEngine");
        properties.put(Constants.SERVICE_VENDOR, "Thymeleaf");
        logger.info("registering {} as service {} with properties {}", templateEngine, ITemplateEngine.class.getName(), properties);
        serviceRegistration = bundleContext.registerService(ITemplateEngine.class, templateEngine, properties);
    }

    private void unregisterTemplateEngine() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    TemplateEngine getTemplateEngine() {
        synchronized (lock) {
            if (templateEngine == null) {
                serviceTemplateEngine();
            }
            return templateEngine;
        }
    }

}
