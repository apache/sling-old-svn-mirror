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
package org.apache.sling.osgi.log;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;

/**
 * The <code>LogbackManager</code> manages the loggers used by the LogService
 * and the rest of the system.
 */
public class LogbackManager implements ManagedService {

    /**
     * Initial configuration property specifying whether Logback should be
     * initialized here or not (value is "org.apache.sling.osgi.log.intialize").
     * If this property is missing or set to <code>true</code>, this class
     * will reset and configure Logback. Otherwise, Logback is neither reset nor
     * configured by this class.
     * <p>
     * This property may be used to prevent resetting Logback which might be
     * configured by a container and reused by the Framework.
     */
    public static final String LOG_INITIALIZE = "org.apache.sling.osgi.log.intialize";

    public static final String LOG_LEVEL = "org.apache.sling.osgi.log.level";

    public static final String LOG_FILE = "org.apache.sling.osgi.log.file";

    public static final String LOG_FILE_NUMBER = "org.apache.sling.osgi.log.file.number";

    public static final String LOG_FILE_SIZE = "org.apache.sling.osgi.log.file.size";

    public static final String LOG_PATTERN = "org.apache.sling.osgi.log.pattern";

    public static final String LOG_CONFIG_URL = "org.apache.sling.osgi.log.url";

    public static final String LOG_PATTERN_DEFAULT = "%d{dd.MM.yyyy HH:mm:ss} *%-5p* %c{1}: %m%n";

    public static final int LOG_FILE_NUMBER_DEFAULT = 5;

    public static final String LOG_FILE_SIZE_DEFAULT = "10MB";

    /**
     * default log category - set during init()
     */
    private org.slf4j.Logger log;

    private File rootDir;

    private ServiceRegistration logbackConfigurable;

    /* package */LogbackManager(final BundleContext context) {

        // the base for relative path names
        String root = context.getProperty("sling.home");
        this.rootDir = new File((root == null) ? "" : root).getAbsoluteFile();

        // set initial default configuration
        this.configureLogback(new ConfigProperties() {
            public String getProperty(String name) {
                return context.getProperty(name);
            }
        });

        // get our own logger
        this.log = LoggerFactory.getLogger(LogServiceFactory.class);

        this.log.info("LogbackManager: Logging set up from context");

        // register for official configuration now
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, LogbackManager.class.getName());
        props.put(Constants.SERVICE_DESCRIPTION,
            "LogbackManager Configuration Admin support");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        this.logbackConfigurable = context.registerService(
            ManagedService.class.getName(), this, props);
    }

    /* package */void shutdown() {
        if (this.logbackConfigurable != null) {
            this.logbackConfigurable.unregister();
            this.logbackConfigurable = null;
        }

        // shutdown the log manager
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.shutdownAndReset();
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
            this.configureLogback(new ConfigProperties() {
                public String getProperty(String name) {
                    return (String) properties.get(name);
                }
            });
        }
    }

    // --------- log management ------------------------------------------------

    /**
     * Start this service. Nothing to be done here.
     */
    public void reconfigure(URL configLocation) throws Exception {
        // reset logging first
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.shutdownAndReset();

        // get the configurator
        JoranConfigurator configuration = new JoranConfigurator();
        configuration.setContext(loggerContext);

        // check for log configuration URL and try that first
        try {
            configuration.doConfigure(configLocation);
        } catch (Throwable t) {
            this.log.error("reconfigure: Cannot configure from {}",
                configLocation);
            // well then, fall back to simple configuration
        }

        this.log.info("reconfigure: Logging reconfigured from ", configLocation);
    }

    void reconfigure(String data) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.shutdownAndReset();

        // get the configurator
        JoranConfigurator configuration = new JoranConfigurator();
        configuration.setContext(loggerContext);

        InputSource source = new InputSource();
        source.setCharacterStream(new StringReader(data));
    }

    public String[] getLoggers() {
        Set<String> loggerSet = new TreeSet<String>();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Logger> loggers = loggerContext.getLoggerList();
        for (Logger logger : loggers) {
            loggerSet.add(logger.getName() + ", level=" + logger.getLevel());
        }

        // prepend the root logger
        Logger root = loggerContext.getLogger(LoggerContext.ROOT_NAME);
        loggerSet.add("__root__, level=" + root.getLevel());

        return loggerSet.toArray(new String[loggerSet.size()]);
    }

    public String setLogLevel(String logger, String levelName) {
        // ignore if logger is not set
        if (logger == null || logger.length() == 0) {
            return "Logger name required to set logging level";
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger log = loggerContext.getLogger(logger);
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
     * Configures Logback from the given properties. This is intended as an
     * initial configuration. <p/> Sets up the initial Logback properties for
     * the logging support until the real logging configuration file can be read
     * from the ContentBus.
     *
     * @param properties The <code>Properties</code> containing the initial
     *            configuration.
     * @throws NullPointerException if <code>properties</code> is
     *             <code>null</code>.
     */
    protected void configureLogback(ConfigProperties context) {

        // check whether we should configure logging at all
        String initialize = context.getProperty(LOG_INITIALIZE);
        if (initialize != null && !"true".equalsIgnoreCase(initialize)) {
            // not initializing logging now
            return;
        }

        // reset logging first
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.shutdownAndReset();

        // get the configurator
        JoranConfigurator configuration = new JoranConfigurator();
        configuration.setContext(loggerContext);

        // check for log configuration URL and try that first
        String logConfig = context.getProperty(LOG_CONFIG_URL);
        if (logConfig != null && logConfig.length() > 0) {
            try {
                URL logConfigURL = new URL(logConfig);
                configuration.doConfigure(logConfigURL);
                return;
            } catch (Throwable t) {
                // well then, fall back to simple configuration
            }
        }

        // set the log appender message pattern
        String pattern = context.getProperty(LOG_PATTERN);
        if (pattern == null || pattern.length() == 0) {
            pattern = LOG_PATTERN_DEFAULT;
        }
        PatternLayout pl = new PatternLayout();
        pl.setPattern(pattern);
        pl.setContext(loggerContext);
        pl.start();

        // if a log file is defined, use the file appender
        String logFileName = context.getProperty(LOG_FILE);
        Appender<LoggingEvent> appender;
        if (logFileName != null && logFileName.length() > 0) {

            // ensure proper separator in the path
            logFileName = logFileName.replace('/', File.separatorChar);

            // ensure absolute path
            File logFile = new File(logFileName);
            if (!logFile.isAbsolute()) {
                logFile = new File(this.rootDir, logFileName);
                logFileName = logFile.getAbsolutePath();
            }

            // check parent directory
            File logDir = logFile.getParentFile();
            if (logDir != null) {
                logDir.mkdirs();
            }

            // get number of files and ensure minimum and default
            Object fileNumObj = context.getProperty(LOG_FILE_NUMBER);
            int fileNum = -1;
            if (fileNumObj instanceof Number) {
                fileNum = ((Number) fileNumObj).intValue();
            } else if (fileNumObj != null) {
                try {
                    fileNum = Integer.parseInt(fileNumObj.toString());
                } catch (NumberFormatException nfe) {
                    // don't care
                }
            }
            if (fileNum <= 0) {
                fileNum = LOG_FILE_NUMBER_DEFAULT;
            }

            // keep the number old log files
            FixedWindowRollingPolicy rolling = new FixedWindowRollingPolicy();
            rolling.setFileNamePattern(logFileName + ".%i");
            rolling.setMinIndex(0);
            rolling.setMaxIndex(fileNum - 1);
            rolling.setContext(loggerContext);

            // get the log file size
            Object fileSizeObj = context.getProperty(LOG_FILE_SIZE);
            String fileSize = (fileSizeObj != null) ? fileSizeObj.toString() : null;
            if (fileSize == null || fileSize.length() == 0) {
                fileSize = LOG_FILE_SIZE_DEFAULT;
            }

            // switch log file after 1MB
            SizeBasedTriggeringPolicy trigger = new SizeBasedTriggeringPolicy();
            trigger.setMaxFileSize(fileSize);
            trigger.setContext(loggerContext);

            // define the default appender
            RollingFileAppender<LoggingEvent> fa = new RollingFileAppender<LoggingEvent>();
            fa.setFile(logFileName);
            fa.setAppend(true);
            fa.setRollingPolicy(rolling);
            fa.setTriggeringPolicy(trigger);

            // link the roller to the file appender
            rolling.setParent(fa);
            rolling.start();
            trigger.start();

            appender = fa;

        } else {

            // fall back to console if no log file defined
            appender = new ConsoleAppender<LoggingEvent>();

        }

        // finalize the appender setup
        appender.setContext(loggerContext);
        appender.setLayout(pl);
        appender.start();

        // check for the log level setting in the web app properties
        String logLevel = context.getProperty(LOG_LEVEL);

        // add the appender for the root logger
        Logger rootLogger = loggerContext.getLogger(LoggerContext.ROOT_NAME);
        rootLogger.addAppender(appender);
        rootLogger.setLevel(Level.toLevel(logLevel, Level.WARN));

        // do not go up the line if root is the prefixed-logger, otherwise if
        // root is the real root, this has no effect
        rootLogger.setAdditive(false);
    }

    protected static interface ConfigProperties {
        String getProperty(String name);
    }
}
