/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.log.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.commons.log.internal.config.ConfigurationServiceFactory;
import org.apache.sling.commons.log.internal.slf4j.LogConfigManager;
import org.apache.sling.commons.log.internal.slf4j.SlingConfigurationPrinter;
import org.apache.sling.commons.log.internal.slf4j.SlingLogPanel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LogManager</code> manages the loggers used by the LogService and
 * the rest of the system.
 */
public class LogManager {

    public static final String LOG_LEVEL = "org.apache.sling.commons.log.level";

    public static final String LOG_FILE = "org.apache.sling.commons.log.file";

    public static final String LOG_FILE_NUMBER = "org.apache.sling.commons.log.file.number";

    public static final String LOG_FILE_SIZE = "org.apache.sling.commons.log.file.size";

    public static final String LOG_PATTERN = "org.apache.sling.commons.log.pattern";

    public static final String LOG_PATTERN_DEFAULT = "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5}";

    public static final String LOG_LOGGERS = "org.apache.sling.commons.log.names";

    public static final String LOG_LEVEL_DEFAULT = "INFO";

    public static final int LOG_FILE_NUMBER_DEFAULT = 5;

    public static final String LOG_FILE_SIZE_DEFAULT = "'.'yyyy-MM-dd";

    public static final String PID = "org.apache.sling.commons.log.LogManager";

    public static final String FACTORY_PID_WRITERS = PID + ".factory.writer";

    public static final String FACTORY_PID_CONFIGS = PID + ".factory.config";

    private final LogConfigManager logConfigManager;

    /**
     * default log category - set during init()
     */
    private Logger log;

    private ServiceRegistration loggingConfigurable;

    private ServiceRegistration writerConfigurer;

    private ServiceRegistration configConfigurer;

    LogManager(final BundleContext context) {

        // set the root folder for relative log file names
        logConfigManager = LogConfigManager.getInstance();
        logConfigManager.setRoot(context.getProperty("sling.home"));
        logConfigManager.setDefaultConfiguration(getBundleConfiguration(context));

        // get our own logger
        log = LoggerFactory.getLogger(LogServiceFactory.class);
        log.info("LogManager: Logging set up from context");

        // prepare registration properties (will be reused)
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        // register for official (global) configuration now
        props.put(Constants.SERVICE_PID, PID);
        props.put(Constants.SERVICE_DESCRIPTION,
            "LogManager Configuration Admin support");
        loggingConfigurable = context.registerService(
            "org.osgi.service.cm.ManagedService",
            new ConfigurationServiceFactory(logConfigManager,
                "org.apache.sling.commons.log.internal.config.GlobalConfigurator"),
            props);

        // register for log writer configuration
        ConfigurationServiceFactory msf = new ConfigurationServiceFactory(
            logConfigManager,
            "org.apache.sling.commons.log.internal.config.LogWriterManagedServiceFactory");
        props.put(Constants.SERVICE_PID, FACTORY_PID_WRITERS);
        props.put(Constants.SERVICE_DESCRIPTION, "LogWriter configurator");
        writerConfigurer = context.registerService(
            "org.osgi.service.cm.ManagedServiceFactory", msf, props);

        // register for log configuration
        msf = new ConfigurationServiceFactory(
            logConfigManager,
            "org.apache.sling.commons.log.internal.config.LoggerManagedServiceFactory");
        props.put(Constants.SERVICE_PID, FACTORY_PID_CONFIGS);
        props.put(Constants.SERVICE_DESCRIPTION, "Logger configurator");
        configConfigurer = context.registerService(
            "org.osgi.service.cm.ManagedServiceFactory", msf, props);

        // setup the web console plugin panel. This may fail loading
        // the panel class if the Servlet API is not wired
        try {
            SlingLogPanel.registerPanel(context);
        } catch (Throwable ignore) {
        }
        // setup the web console configuration printer.
        SlingConfigurationPrinter.registerPrinter(context);
    }

    void shutdown() {

        // tear down the web console plugin panel (if created at all). This
        // may fail loading the panel class if the Servlet API is not wired
        try {
             SlingLogPanel.unregisterPanel();
        } catch (Throwable ignore) {
        }
        // tear down the web console configuration printer (if created at all).
        SlingConfigurationPrinter.unregisterPrinter();

        if (loggingConfigurable != null) {
            loggingConfigurable.unregister();
            loggingConfigurable = null;
        }

        if (writerConfigurer != null) {
            writerConfigurer.unregister();
            writerConfigurer = null;
        }

        if (configConfigurer != null) {
            configConfigurer.unregister();
            configConfigurer = null;
        }

        // shutdown the log manager
        logConfigManager.close();
    }

    // ---------- ManagedService interface -------------------------------------

    private Dictionary<String, String> getBundleConfiguration(
            BundleContext bundleContext) {
        Dictionary<String, String> config = new Hashtable<String, String>();

        final String[] props = { LOG_LEVEL, LOG_LEVEL, LOG_FILE,
            LOG_FILE_NUMBER, LOG_FILE_SIZE, LOG_PATTERN };
        for (String prop : props) {
            String value = bundleContext.getProperty(prop);
            if (value != null) {
                config.put(prop, value);
            }
        }

        // ensure sensible default values for required configuration field(s)
        if (config.get(LOG_LEVEL) == null) {
            config.put(LOG_LEVEL, LOG_LEVEL_DEFAULT);
        }

        return config;
    }
}
