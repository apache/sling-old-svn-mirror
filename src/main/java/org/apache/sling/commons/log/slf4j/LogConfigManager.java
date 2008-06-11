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
package org.apache.sling.commons.log.slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.sling.commons.log.LogManager;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class LogConfigManager implements ILoggerFactory {

    public static final String ROOT = "";

    private static LogConfigManager instance = new LogConfigManager();

    private final Map<String, SlingLoggerWriter> writerByPid;

    private final Map<String, SlingLoggerWriter> writerByFileName;

    private final Map<String, SlingLoggerConfig> configByPid;

    private final Map<String, SlingLoggerConfig> configByCategory;

    private final Map<String, SoftReference<SlingLogger>> loggersByCategory;

    private SlingLogger defaultLogger;

    private SlingLoggerWriter defaultWriter;

    private File rootDir;

    public static LogConfigManager getInstance() {
        return instance;
    }

    /**
     * Logs a message an optional stack trace to error output. This method is
     * used by the logging system in case of errors writing to the correct
     * logging output.
     */
    public static void internalFailure(String message, Throwable t) {
        System.err.println(message);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    private LogConfigManager() {
        writerByPid = new HashMap<String, SlingLoggerWriter>();
        writerByFileName = new HashMap<String, SlingLoggerWriter>();
        configByPid = new HashMap<String, SlingLoggerConfig>();
        configByCategory = new HashMap<String, SlingLoggerConfig>();
        loggersByCategory = new HashMap<String, SoftReference<SlingLogger>>();

        defaultWriter = new SlingLoggerWriter(LogManager.PID);
        try {
            defaultWriter.configure(null, 0, "0");
        } catch (IOException ioe) {
            internalFailure("Cannot initialize default SlingLoggerWriter", ioe);
        }

        Set<String> defaultCategories = new HashSet<String>();
        defaultCategories.add(ROOT);

        SlingLoggerConfig defaultConfig = new SlingLoggerConfig(LogManager.PID,
            LogManager.LOG_PATTERN_DEFAULT, defaultCategories,
            SlingLoggerLevel.INFO, defaultWriter);

        defaultLogger = new SlingLogger(ROOT);
        defaultLogger.configure(defaultConfig);

        // register the writer (PID only, without configuration, the
        // writer logs to sdtout and has no file name)
        writerByPid.put(LogManager.PID, defaultWriter);

        // register the config
        configByPid.put(LogManager.PID, defaultConfig);
        configByCategory.put(ROOT, defaultConfig);

        // register the logger with root category
        loggersByCategory.put(ROOT, new SoftReference<SlingLogger>(
            defaultLogger));
    }

    public void setRoot(String root) {
        // the base for relative path names
        rootDir = new File((root == null) ? "" : root).getAbsoluteFile();
    }

    public void close() {
        writerByPid.clear();
        writerByFileName.clear();
        configByPid.clear();
        configByCategory.clear();

        // remove references to the loggers
        for (SoftReference<SlingLogger> logger : loggersByCategory.values()) {
            logger.clear();
        }
        loggersByCategory.clear();

        try {
            defaultWriter.close();
        } catch (IOException ignore) {
            // don't care for this
        }
        // defaultLogger.getConfiguration().close();
    }

    // ---------- ILoggerFactory -----------------------------------------------

    public Logger getLogger(String name) {
        SoftReference<SlingLogger> logger = loggersByCategory.get(name);
        SlingLogger slingLogger = (logger != null) ? logger.get() : null;

        // no logger at all or reference has been collected, create a new one
        if (slingLogger == null) {
            slingLogger = new SlingLogger(name);
            slingLogger.configure(getLoggerConfig(name));
            loggersByCategory.put(name, new SoftReference<SlingLogger>(
                slingLogger));
        }

        return slingLogger;
    }

    // ---------- Configuration support ----------------------------------------

    public void updateLogWriter(String pid, Dictionary configuration)
            throws ConfigurationException {

        if (configuration != null) {
            SlingLoggerWriter slw = writerByPid.get(pid);

            // get the log file parameter and normalize empty string to null
            String logFileName = (String) configuration.get(LogManager.LOG_FILE);
            if (logFileName != null && logFileName.trim().length() == 0) {
                logFileName = null;
            }

            if (logFileName != null) {
                // ensure proper separator in the path
                logFileName = logFileName.replace('/', File.separatorChar);

                // ensure absolute path
                logFileName = getAbsoluteLogFile(logFileName);

                // ensure unique configuration of the log writer
                SlingLoggerWriter existingWriter = writerByFileName.get(logFileName);
                if (existingWriter != null
                    && existingWriter.getConfigurationPID().equals(pid)) {
                    // this file is already configured by another LOG_PID
                    throw new ConfigurationException(LogManager.LOG_FILE,
                        "LogFile " + logFileName
                            + " already configured by configuration "
                            + existingWriter.getConfigurationPID());
                }
            }

            // get number of files and ensure minimum and default
            Object fileNumProp = configuration.get(LogManager.LOG_FILE_NUMBER);
            int fileNum = -1;
            if (fileNumProp instanceof Number) {
                fileNum = ((Number) fileNumProp).intValue();
            } else if (fileNumProp != null) {
                try {
                    fileNum = Integer.parseInt(fileNumProp.toString());
                } catch (NumberFormatException nfe) {
                    // don't care
                }
            }
            if (fileNum <= 0) {
                fileNum = LogManager.LOG_FILE_NUMBER_DEFAULT;
            }

            // get the log file size
            Object fileSizeProp = configuration.get(LogManager.LOG_FILE_SIZE);
            String fileSize = null;
            if (fileSizeProp != null) {
                fileSize = fileSizeProp.toString();
            }
            if (fileSize == null || fileSize.length() == 0) {
                fileSize = LogManager.LOG_FILE_SIZE_DEFAULT;
            }

            try {
                if (slw == null) {
                    slw = new SlingLoggerWriter(pid);
                    slw.configure(logFileName, fileNum, fileSize);
                    writerByPid.put(pid, slw);

                    if (logFileName != null) {
                        writerByFileName.put(logFileName, slw);
                    }
                } else {
                    slw.configure(logFileName, fileNum, fileSize);
                }
            } catch (IOException ioe) {
                internalFailure("Cannot create log file " + logFileName, ioe);
                internalFailure("Logging to the console", null);
                throw new ConfigurationException(LogManager.LOG_FILE,
                    "Cannot create writer for log file " + logFileName);
            }

        } else {

            SlingLoggerWriter logWriter = writerByPid.remove(pid);
            if (logWriter != null) {

                // if the writer is writing to a file, remove the file mapping
                String path = logWriter.getPath();
                if (path != null) {
                    writerByFileName.remove(path);
                }

                // make sure, no configuration is referring to this writer
                // any more
                for (SlingLoggerConfig config : configByPid.values()) {
                    if (config.getLogWriter() == logWriter) {
                        // TODO: log this !
                        config.setLogWriter(defaultWriter);
                    }
                }

                // close the removed log writer
                try {
                    logWriter.close();
                } catch (IOException ioe) {
                    // don't care
                }
            }
        }
    }

    public void updateLoggerConfiguration(String pid, Dictionary configuration)
            throws ConfigurationException {

        if (configuration != null) {

            String pattern = (String) configuration.get(LogManager.LOG_PATTERN);
            String level = (String) configuration.get(LogManager.LOG_LEVEL);
            String file = (String) configuration.get(LogManager.LOG_FILE);
            Set<String> categories = toCategoryList(configuration.get(LogManager.LOG_LOGGERS));

            // verify categories
            if (categories == null) {
                throw new ConfigurationException(LogManager.LOG_LOGGERS,
                    "Missing categories in configuration " + pid);
            }

            // verify no other configuration has any of the categories
            for (String cat : categories) {
                SlingLoggerConfig cfg = configByCategory.get(cat);
                if (cfg != null && !pid.equals(cfg.getConfigPid())) {
                    throw new ConfigurationException(LogManager.LOG_LOGGERS,
                        "Category " + cat
                            + " already defined by configuration " + pid);
                }
            }

            // verify writer
            SlingLoggerWriter writer;
            if (file != null && file.length() > 0) {
                writer = writerByPid.get(file);
                if (writer == null) {
                    writer = writerByFileName.get(file);
                    if (writer == null) {
                        file = getAbsoluteLogFile(file);
                        writer = writerByFileName.get(file);
                        if (writer == null) {
                            writer = defaultWriter;
                        }
                    }
                }
            } else {
                writer = defaultWriter;
            }

            // verify log level
            if (level == null) {
                throw new ConfigurationException(LogManager.LOG_LEVEL,
                    "Value required");
            }
            SlingLoggerLevel logLevel = SlingLoggerLevel.valueOf(level.toUpperCase());

            // verify pattern
            if (pattern == null || pattern.length() == 0) {
                pattern = LogManager.LOG_PATTERN_DEFAULT;
            }

            // create or modify existing configuration object
            SlingLoggerConfig config = configByPid.get(pid);
            if (config == null) {

                // create and store new configuration
                config = new SlingLoggerConfig(pid, pattern, categories,
                    logLevel, writer);
                configByPid.put(pid, config);

            } else {

                // remove category to configuration mappings
                for (Iterator<String> ci = config.getCategories(); ci.hasNext();) {
                    configByCategory.remove(ci.next());
                }

                // reconfigure the configuration
                config.configure(pattern, categories, logLevel, writer);
            }

            // relink categories
            for (String cat : categories) {
                configByCategory.put(cat, config);
            }

        } else {

            // configuration deleted if null

            // remove configuration from pid list
            SlingLoggerConfig config = configByPid.remove(pid);

            // remove all configured categories
            if (config != null) {
                // remove category to configuration mappings
                for (Iterator<String> ci = config.getCategories(); ci.hasNext();) {
                    configByCategory.remove(ci.next());
                }
            }

        }

        // reconfigure existing loggers
        reconfigureLoggers();
    }

    private String getAbsoluteLogFile(String logFileName) {
        File logFile = new File(logFileName);
        if (!logFile.isAbsolute()) {
            logFile = new File(rootDir, logFileName);
            logFileName = logFile.getAbsolutePath();
        }
        return logFileName;
    }

    void reconfigureLoggers() {
        // assign correct logger configs to all existing/known loggers
        for (Iterator<SoftReference<SlingLogger>> si = loggersByCategory.values().iterator(); si.hasNext();) {
            SlingLogger logger = si.next().get();
            if (logger != null) {
                logger.configure(getLoggerConfig(logger.getName()));
            } else {
                // if the logger has been GC-ed, remove the entry from the map
                si.remove();
            }
        }
    }

    SlingLoggerConfig getLoggerConfig(String logger) {
        for (;;) {
            SlingLoggerConfig config = configByCategory.get(logger);
            if (config != null) {
                return config;
            }

            if (logger.length() == 0) {
                break;
            }

            int dot = logger.lastIndexOf('.');
            if (dot < 0) {
                logger = ROOT;
            } else {
                logger = logger.substring(0, dot);
            }
        }

        return defaultLogger.getConfiguration();
    }

    private Set<String> toCategoryList(Object loggers) {
        Set<String> loggerList = new HashSet<String>();
        if (loggers == null) {

            return null;

        } else if (loggers.getClass().isArray()) {

            for (Object loggerObject : (Object[]) loggers) {
                if (loggerObject != null) {
                    splitLoggers(loggerList, loggerObject);
                }
            }

        } else if (loggers instanceof Vector) {

            for (Object loggerObject : (Vector) loggers) {
                if (loggerObject != null) {
                    splitLoggers(loggerList, loggerObject);
                }
            }

        } else {

            splitLoggers(loggerList, loggers);

        }

        return loggerList;
    }

    private void splitLoggers(Set<String> loggerList, Object loggerObject) {
        if (loggerObject != null) {
            String loggers[] = loggerObject.toString().split(",");
            for (String logger : loggers) {
                logger = logger.trim();
                if (logger.length() > 0) {
                    loggerList.add(logger);
                }
            }
        }
    }

}
