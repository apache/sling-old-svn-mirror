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

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.cache.ICacheManager;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.messageresolver.IMessageResolver;
import org.thymeleaf.standard.StandardDialect;
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
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        bind = "setCacheManager",
        unbind = "unsetCacheManager"
    )
    private volatile ICacheManager cacheManager;

    private TemplateEngine templateEngine;

    private final Object lock = new Object();

    private final Logger logger = LoggerFactory.getLogger(ThymeleafScriptEngineFactory.class);

    public ThymeleafScriptEngineFactory() {
    }

    protected void addTemplateResolver(final ITemplateResolver templateResolver) {
        synchronized (lock) {
            logger.debug("adding template resolver '{}'", templateResolver.getName());
            templateEngine = null;
        }
    }

    protected void removeTemplateResolver(final ITemplateResolver templateResolver) {
        synchronized (lock) {
            logger.debug("removing template resolver '{}'", templateResolver.getName());
            templateEngine = null;
        }
    }

    protected void addMessageResolver(final IMessageResolver messageResolver) {
        synchronized (lock) {
            logger.debug("adding message resolver '{}'", messageResolver.getName());
            templateEngine = null;
        }
    }

    protected void removeMessageResolver(final IMessageResolver messageResolver) {
        synchronized (lock) {
            logger.debug("removing message resolver '{}'", messageResolver.getName());
            templateEngine = null;
        }
    }

    protected void addDialect(final IDialect dialect) {
        synchronized (lock) {
            logger.debug("adding dialect '{}'", dialect.getName());
            templateEngine = null;
        }
    }

    protected void removeDialect(final IDialect dialect) {
        synchronized (lock) {
            logger.debug("removing dialect '{}'", dialect.getName());
            templateEngine = null;
        }
    }

    protected void setCacheManager(final ICacheManager cacheManager) {
        synchronized (lock) {
            logger.debug("setting cache manager '{}'", cacheManager.getClass().getName());
            templateEngine = null;
        }
    }

    protected void unsetCacheManager(final ICacheManager cacheManager) {
        synchronized (lock) {
            logger.debug("unsetting cache manager '{}'", cacheManager.getClass().getName());
            templateEngine = null;
        }
    }

    @Activate
    private void activate(final ThymeleafScriptEngineFactoryConfiguration configuration) {
        logger.debug("activate");
        configure(configuration);
    }

    @Modified
    private void modified(final ThymeleafScriptEngineFactoryConfiguration configuration) {
        logger.debug("modified");
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivate");
        templateEngine = null;
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

    TemplateEngine getTemplateEngine() {
        synchronized (lock) {
            if (this.templateEngine == null) {
                logger.info("setting up new template engine");
                templateEngine = new TemplateEngine();
                final Set<ITemplateResolver> templateResolvers = new HashSet<>(this.templateResolvers);
                templateEngine.setTemplateResolvers(templateResolvers);
                final Set<IMessageResolver> messageResolvers = new HashSet<>(this.messageResolvers);
                templateEngine.setMessageResolvers(messageResolvers);
                final Set<IDialect> dialects = new HashSet<>(this.dialects);
                templateEngine.setDialects(dialects);
                final IDialect standardDialect = new StandardDialect();
                templateEngine.addDialect(standardDialect);
                templateEngine.setCacheManager(cacheManager);
            }
            return templateEngine;
        }
    }

}
