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
package org.apache.sling.scripting.freemarker.internal;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import freemarker.template.Configuration;
import freemarker.template.TemplateModel;
import org.apache.sling.commons.osgi.SortingServiceTracker;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = ScriptEngineFactory.class,
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Scripting engine for FreeMarker templates",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = FreemarkerScriptEngineFactoryConfiguration.class
)
public class FreemarkerScriptEngineFactory extends AbstractScriptEngineFactory {

    /** The name of the FreeMarker language (value is "FreeMarker"). */
    private static final String FREEMARKER_NAME = "FreeMarker";

    private BundleContext bundleContext;

    private SortingServiceTracker<TemplateModel> templateModelTracker;

    private final Logger logger = LoggerFactory.getLogger(FreemarkerScriptEngineFactory.class);

    public FreemarkerScriptEngineFactory() {
    }

    @Activate
    private void activate(final FreemarkerScriptEngineFactoryConfiguration configuration, final BundleContext bundleContext) {
        logger.debug("activate");
        configure(configuration);
        this.bundleContext = bundleContext;
        templateModelTracker = new SortingServiceTracker<>(bundleContext, TemplateModel.class.getName());
        templateModelTracker.open();
    }

    @Modified
    private void modified(final FreemarkerScriptEngineFactoryConfiguration configuration) {
        logger.debug("modified");
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivate");
        templateModelTracker.close();
        templateModelTracker = null;
        bundleContext = null;
    }

    private void configure(final FreemarkerScriptEngineFactoryConfiguration configuration) {
        setExtensions(configuration.extensions());
        setMimeTypes(configuration.mimeTypes());
        setNames(configuration.names());
    }

    public ScriptEngine getScriptEngine() {
        return new FreemarkerScriptEngine(this);
    }

    public String getLanguageName() {
        return FREEMARKER_NAME;
    }

    public String getLanguageVersion() {
        return Configuration.getVersion().toString();
    }

    Map<String, TemplateModel> getTemplateModels() {
        final Map<String, TemplateModel> models = new HashMap<>();
        for (final ServiceReference<TemplateModel> serviceReference : templateModelTracker.getSortedServiceReferences()) {
            models.put(serviceReference.getProperty("name").toString(), bundleContext.getService(serviceReference));
        }
        return models;
    }

}
