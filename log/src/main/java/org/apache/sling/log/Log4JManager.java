/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.log;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggerRepository;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

/**
 * The <code>Log4JManager</code> manages the loggers used by the LogService
 * and the rest of the system.
 */
public class Log4JManager {

    /**
     * Initial configuration property specifying whether LOG4J should be
     * initialized here or not (value is "org.apache.sling.log.intialize"). If
     * this property is missing or set to <code>true</code>, this class will
     * reset and configure LOG4J. Otherwise, LOG4J is neither reset nor
     * configured by this class.
     * <p>
     * This property may be used to prevent resetting LOG4J which might be
     * configured by a container and reused by the Framework.
     */
    public static final String LOG_INITIALIZE = "org.apache.sling.log.intialize";

    public static final String LOG_LEVEL = "org.apache.sling.log.level";

    public static final String LOG_FILE = "org.apache.sling.log.file";

    public static final String LOG_PATTERN = "org.apache.sling.log.pattern";

    public static final String LOG_CONFIG_URL = "org.apache.sling.log.url";

    public static final String LOG_PATTERN_DEFAULT = "%d{dd.MM.yyyy HH:mm:ss} *%-5p* %c{1}: %m%n";

    /**
     * default log category - set during init()
     */
    private org.slf4j.Logger log;

    /* package */Log4JManager(BundleContext context) {
        // init basic logging
        if (log == null) {

            // set initial default configuration
            configureLog4J(context);

            // get our own logger
            log = LoggerFactory.getLogger(LogServiceFactory.class);

            log.info("Log4JManager: Logging set up from context");
        } else {

            log.info("Log4JManager: Logging has already been configured");

        }
    }

    /* package */void shutdown() {
        // shutdown the log manager
        LogManager.shutdown();
    }

    // --------- log management ------------------------------------------------

    /**
     * Start this service. Nothing to be done here.
     */
    public void reconfigure(URL configLocation) throws Exception {
        LoggerRepository repo = LogManager.getLoggerRepository();
        repo.resetConfiguration();
        OptionConverter.selectAndConfigure(configLocation, null, repo);
        log.info("reconfigure: Logging reconfigured from {0}", configLocation);
    }

    public String[] getLoggers() {
        Set loggerSet = new TreeSet();

        Enumeration loggers = LogManager.getCurrentLoggers();
        while (loggers.hasMoreElements()) {
            Logger logger = (Logger) loggers.nextElement();
            loggerSet.add(logger.getName() + ", level=" + logger.getLevel());
        }

        // prepend the root logger
        Logger root = Logger.getRootLogger();
        loggerSet.add("__root__, level=" + root.getLevel());

        return (String[]) loggerSet.toArray(new String[loggerSet.size()]);
    }

    public String setLogLevel(String logger, String levelName) {
        // ignore if logger is not set
        if (logger == null || logger.length() == 0) {
            return "Logger name required to set logging level";
        }

        Logger log = Logger.getLogger(logger);
        if (log != null) {
            Level level = Level.toLevel(levelName, log.getLevel());
            log.setLevel(level);

            return "Set level '" + level + "' on logger '" + log.getName()
                + "'";
        }

        // Fallback
        return "Logger '" + logger + "' cannot be retrieved";
    }

    /**
     * Configures Log4J from the given properties. This is intended as an
     * initial configuration. <p/> Sets up the initial Log4J properties for the
     * logging support until the real logging configuration file can be read
     * from the ContentBus.
     * 
     * @param properties The <code>Properties</code> containing the initial
     *            configuration.
     * @throws NullPointerException if <code>properties</code> is
     *             <code>null</code>.
     */
    private static void configureLog4J(BundleContext context) {

        // check whether we should configure logging at all
        String initialize = context.getProperty(LOG_INITIALIZE);
        if (initialize != null && !"true".equalsIgnoreCase(initialize)) {
            // not initializing logging now
            return;
        }

        // reset logging first
        try {
            LogManager.resetConfiguration();
        } catch (Throwable t) {
            // might happen due to missing configuration file
        }

        // check for log configuration URL and try that first
        String logConfig = context.getProperty(LOG_CONFIG_URL);
        if (logConfig != null) {
            try {
                URL logConfigURL = new URL(logConfig);
                OptionConverter.selectAndConfigure(logConfigURL, null,
                    LogManager.getLoggerRepository());
                return;
            } catch (Throwable t) {
                // well then, fall back to simple configuration
            }
        }

        // check for the log level setting in the web app properties
        String logLevel = context.getProperty(LOG_LEVEL);

        // get initial log file name substituting variables
        String logFileName = context.getProperty(LOG_FILE);

        // if a log file is defined, use the file appender
        WriterAppender appender;
        if (logFileName != null && logFileName.length() > 0) {

            // ensure proper separator in the path
            logFileName = logFileName.replace('/', File.separatorChar);

            // ensure absolute path
            File logFile = new File(logFileName);

            // check parent directory
            File logDir = logFile.getParentFile();
            if (logDir != null) {
                logDir.mkdirs();
            }

            // define the default appender
            FileAppender fa = new FileAppender();
            fa.setFile(logFileName);
            fa.setAppend(true);
            appender = fa;

        } else {

            // fall back to console if no log file defined
            appender = new ConsoleAppender();

        }

        // set the log appender message pattern
        String pattern = context.getProperty(LOG_PATTERN);
        if (pattern == null || pattern.length() == 0) {
            pattern = LOG_PATTERN_DEFAULT;
        }
        PatternLayout pl = new PatternLayout(pattern);
        appender.setLayout(pl);

        // activate option settings of the appender
        appender.activateOptions();

        // add the appender for the root logger
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.addAppender(appender);
        rootLogger.setLevel(Level.toLevel(logLevel, Level.WARN));

        // do not go up the line if root is the prefixed-logger, otherwise if
        // root is the real root, this has no effect
        rootLogger.setAdditivity(false);
    }
}
