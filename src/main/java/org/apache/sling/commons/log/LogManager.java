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
package org.apache.sling.commons.log;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.commons.log.slf4j.SlingLogFileWriter;
import org.apache.sling.commons.log.slf4j.SlingLogWriter;
import org.apache.sling.commons.log.slf4j.SlingLogger;
import org.apache.sling.commons.log.slf4j.SlingLoggerFactory;
import org.apache.sling.commons.log.slf4j.SlingLoggerLevel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.slf4j.LoggerFactory;

/**
 * The <code>LogManager</code> manages the loggers used by the LogService and
 * the rest of the system.
 */
public class LogManager implements ManagedService {

    /**
     * Initial configuration property specifying whether logging should be
     * initialized here or not (value is
     * "org.apache.sling.commons.log.intialize"). If this property is missing or
     * set to <code>true</code>, this class will reset and configure logging.
     * Otherwise, logging is neither reset nor configured by this class.
     */
    public static final String LOG_INITIALIZE = "org.apache.sling.commons.log.intialize";

    public static final String LOG_LEVEL = "org.apache.sling.commons.log.level";

    public static final String LOG_FILE = "org.apache.sling.commons.log.file";

    public static final String LOG_FILE_NUMBER = "org.apache.sling.commons.log.file.number";

    public static final String LOG_FILE_SIZE = "org.apache.sling.commons.log.file.size";

    public static final String LOG_PATTERN = "org.apache.sling.commons.log.pattern";

    public static final String LOG_CONFIG_URL = "org.apache.sling.commons.log.url";

    public static final String LOG_PATTERN_DEFAULT = "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5}";

    public static final int LOG_FILE_NUMBER_DEFAULT = 5;

    public static final String LOG_FILE_SIZE_DEFAULT = "10M";

    /**
     * default log category - set during init()
     */
    private org.slf4j.Logger log;

    private File rootDir;

    private ServiceRegistration loggingConfigurable;

    LogManager(final BundleContext context) {

        // the base for relative path names
        String root = context.getProperty("sling.home");
        rootDir = new File((root == null) ? "" : root).getAbsoluteFile();

        // set initial default configuration
        configureLogging(new ConfigProperties() {
            public String getProperty(String name) {
                return context.getProperty(name);
            }
        });

        // get our own logger
        log = LoggerFactory.getLogger(LogServiceFactory.class);
        log.info("LogManager: Logging set up from context");

        // register for official configuration now
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, LogManager.class.getName());
        props.put(Constants.SERVICE_DESCRIPTION,
            "LogManager Configuration Admin support");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        loggingConfigurable = context.registerService(
            ManagedService.class.getName(), this, props);
    }

    void shutdown() {
        if (loggingConfigurable != null) {
            loggingConfigurable.unregister();
            loggingConfigurable = null;
        }

        // shutdown the log manager
        SlingLoggerFactory loggerFactory = SlingLoggerFactory.getInstance();
        loggerFactory.close();
    }

    // ---------- ManagedService interface -------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    @SuppressWarnings("unchecked")
    public void updated(final Dictionary properties) { // unchecked
        if (properties != null) {
            configureLogging(new ConfigProperties() {
                public String getProperty(String name) {
                    final Object obj = properties.get(name);
                    return (obj == null) ? null : obj.toString();
                }
            });
        }
    }

    // --------- log management ------------------------------------------------

    /**
     * Start this service. Nothing to be done here.
     */
    // public void reconfigure(URL configLocation) throws Exception {
    // // reset logging first
    // LoggerContext loggerContext = (LoggerContext)
    // LoggerFactory.getILoggerFactory();
    // loggerContext.shutdownAndReset();
    //
    // // get the configurator
    // JoranConfigurator configuration = new JoranConfigurator();
    // configuration.setContext(loggerContext);
    //
    // // check for log configuration URL and try that first
    // try {
    // configuration.doConfigure(configLocation);
    // } catch (Throwable t) {
    // this.log.error("reconfigure: Cannot configure from {}",
    // configLocation);
    // // well then, fall back to simple configuration
    // }
    //
    // this.log.info("reconfigure: Logging reconfigured from ", configLocation);
    // }
    // void reconfigure(String data) {
    // LoggerContext loggerContext = (LoggerContext)
    // LoggerFactory.getILoggerFactory();
    // loggerContext.shutdownAndReset();
    //
    // // get the configurator
    // JoranConfigurator configuration = new JoranConfigurator();
    // configuration.setContext(loggerContext);
    //
    // InputSource source = new InputSource();
    // source.setCharacterStream(new StringReader(data));
    // }
    public String[] getLoggers() {
        Set<String> loggerSet = new TreeSet<String>();

        SlingLoggerFactory loggerFactory = SlingLoggerFactory.getInstance();
        List<SlingLogger> loggers = loggerFactory.getLoggerList();
        for (SlingLogger logger : loggers) {
            loggerSet.add(logger.getName() + ", level=" + logger.getLogLevel());
        }

        return loggerSet.toArray(new String[loggerSet.size()]);
    }

    public String setLogLevel(String logger, String levelName) {
        // ignore if logger is not set
        if (logger == null || logger.length() == 0) {
            return "Logger name required to set logging level";
        }

        SlingLoggerFactory loggerFactory = SlingLoggerFactory.getInstance();
        SlingLogger log = loggerFactory.getSlingLogger(logger);
        if (log != null) {
            log.setLogLevel(levelName);

            return "Set level '" + levelName + "' on logger '" + log.getName()
                + "'";
        }

        // Fallback
        return "Logger '" + logger + "' cannot be retrieved";
    }

    /**
     * Configures logging from the given properties. This is intended as an
     * initial configuration. <p/> Sets up the initial logging properties for
     * the logging support until the real logging configuration file can be read
     * from the ContentBus.
     * 
     * @param properties The <code>Properties</code> containing the initial
     *            configuration.
     * @throws NullPointerException if <code>properties</code> is
     *             <code>null</code>.
     */
    protected void configureLogging(ConfigProperties context) {

        // check whether we should configure logging at all
        String initialize = context.getProperty(LOG_INITIALIZE);
        if (initialize != null && !"true".equalsIgnoreCase(initialize)) {
            // not initializing logging now
            return;
        }

        // check for log configuration URL and try that first
        // String logConfig = context.getProperty(LOG_CONFIG_URL);
        // if (logConfig != null && logConfig.length() > 0) {
        // try {
        // URL logConfigURL = new URL(logConfig);
        // configuration.doConfigure(logConfigURL);
        // return;
        // } catch (Throwable t) {
        // // well then, fall back to simple configuration
        // }
        // }

        // if a log file is defined, use the file appender
        String logFileName = context.getProperty(LOG_FILE);
        SlingLogWriter output;
        if (logFileName != null && logFileName.length() > 0) {

            // ensure proper separator in the path
            logFileName = logFileName.replace('/', File.separatorChar);

            // ensure absolute path
            File logFile = new File(logFileName);
            if (!logFile.isAbsolute()) {
                logFile = new File(rootDir, logFileName);
                logFileName = logFile.getAbsolutePath();
            }

            // check parent directory
            File logDir = logFile.getParentFile();
            if (logDir != null) {
                logDir.mkdirs();
            }

            // get number of files and ensure minimum and default
            String fileNumProp = context.getProperty(LOG_FILE_NUMBER);
            int fileNum = -1;
            if (fileNumProp != null) {
                try {
                    fileNum = Integer.parseInt(fileNumProp.toString());
                } catch (NumberFormatException nfe) {
                    // don't care
                }
            }
            if (fileNum <= 0) {
                fileNum = LOG_FILE_NUMBER_DEFAULT;
            }

            // get the log file size
            String fileSize = context.getProperty(LOG_FILE_SIZE);
            if (fileSize == null || fileSize.length() == 0) {
                fileSize = LOG_FILE_SIZE_DEFAULT;
            }

            try {
                output = new SlingLogFileWriter(logFileName, fileNum, fileSize);
            } catch (IOException ioe) {
                SlingLoggerFactory.internalFailure("Cannot creat log file "
                    + logFileName, ioe);
                SlingLoggerFactory.internalFailure("Logging to the console",
                    null);
                output = new SlingLogWriter();
            }

        } else {

            // fall back to console if no log file defined
            output = new SlingLogWriter();

        }

        // check for the log level setting in the web app properties
        String logLevel = context.getProperty(LOG_LEVEL);
        if (logLevel == null || logLevel.length() == 0) {
            logLevel = SlingLoggerLevel.INFO.toString();
        } else {
            logLevel = logLevel.toUpperCase();
        }

        // set the log appender message pattern
        String messageFormatString = context.getProperty(LOG_PATTERN);
        if (messageFormatString == null || messageFormatString.length() == 0) {
            messageFormatString = LOG_PATTERN_DEFAULT;
        }
        MessageFormat messageFormat = new MessageFormat(messageFormatString);

        // configure the logger factory now from the setup
        SlingLoggerFactory loggerFactory = SlingLoggerFactory.getInstance();
        loggerFactory.configure(logLevel, output, messageFormat);
    }

    protected static interface ConfigProperties {
        String getProperty(String name);
    }

}
