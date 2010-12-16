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
package org.apache.sling.commons.log.internal.slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.commons.log.internal.LogManager;
import org.apache.sling.commons.log.internal.config.ConfigurationException;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class LogConfigManager implements ILoggerFactory {

    public static final String ROOT = "";

    // the singleton instance of this class
    private static LogConfigManager instance = new LogConfigManager();

    // map of log writers indexed by configuration PID
    private final Map<String, SlingLoggerWriter> writerByPid;

    // map of log writers indexed by (absolute) file name. This map does
    // not contain writers writing to standard out
    private final Map<String, SlingLoggerWriter> writerByFileName;

    // map of log configurations by configuration PID
    private final Map<String, SlingLoggerConfig> configByPid;

    // map of log configurations by the categories they are configured with
    private final Map<String, SlingLoggerConfig> configByCategory;

    // map of all loggers supplied by getLogger(String) by their names. Each
    // entry is in fact a SoftReference to the actual logger, such that the
    // loggers may be cleaned up if no used any more.
    // There is no ReferenceQueue handling currently for removed loggers
    private final Map<String, SoftReference<SlingLogger>> loggersByCategory;

    // the default logger configuration set up by the constructor and managed
    // by the global logger configuration
    private final SlingLoggerConfig defaultLoggerConfig;

    // the default writer configuration set up by the constructor and managed
    // by the global logger configuration
    private final SlingLoggerWriter defaultWriter;

    // the root folder to make relative writer paths absolute
    private File rootDir;

    // global default configuration (from BundleContext properties)
    private Dictionary<String, String> defaultConfiguration;

    /**
     * Returns the single instance of this log configuration instance.
     */
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

    /**
     * Sets up this log configuration manager by creating the default writers
     * and logger configuration
     */
    private LogConfigManager() {
        writerByPid = new ConcurrentHashMap<String, SlingLoggerWriter>();
        writerByFileName = new ConcurrentHashMap<String, SlingLoggerWriter>();
        configByPid = new ConcurrentHashMap<String, SlingLoggerConfig>();
        configByCategory = new ConcurrentHashMap<String, SlingLoggerConfig>();
        loggersByCategory = new ConcurrentHashMap<String, SoftReference<SlingLogger>>();

        // configure the default writer to write to stdout (for now)
        // and register for PID only
        defaultWriter = new SlingLoggerWriter(LogManager.PID);
        try {
            defaultWriter.configure(null, 0, "0");
        } catch (IOException ioe) {
            internalFailure("Cannot initialize default SlingLoggerWriter", ioe);
        }
        writerByPid.put(LogManager.PID, defaultWriter);

        // set up the default configuration using the default logger
        // writing at INFO level to start with
        Set<String> defaultCategories = new HashSet<String>();
        defaultCategories.add(ROOT);
        defaultLoggerConfig = new SlingLoggerConfig(LogManager.PID,
            LogManager.LOG_PATTERN_DEFAULT, defaultCategories,
            SlingLoggerLevel.INFO, defaultWriter);
        configByPid.put(LogManager.PID, defaultLoggerConfig);
        configByCategory.put(ROOT, defaultLoggerConfig);
    }

    /**
     * Sets the root (folder) to be used to make relative paths absolute.
     */
    public void setRoot(String root) {
        rootDir = new File((root == null) ? "" : root).getAbsoluteFile();
    }

    /**
     * Sets and applies the default configuration used by the
     * {@link #updateGlobalConfiguration(Dictionary)} method if no configuration
     * is supplied.
     */
    public void setDefaultConfiguration(
            Dictionary<String, String> defaultConfiguration) {
        this.defaultConfiguration = defaultConfiguration;
        try {
            updateGlobalConfiguration(defaultConfiguration);
        } catch (ConfigurationException ce) {
            internalFailure(ce.getMessage(), ce);
        }
    }

    /**
     * Shuts this configuration manager down by dropping all references to
     * existing configurations, dropping all stored loggers and closing all log
     * writers.
     * <p>
     * After this methods is called, this instance should not be used again.
     */
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

        // shutdown the default writer
        try {
            defaultWriter.close();
        } catch (IOException ignore) {
            // don't care for this
        }
    }

    // ---------- ILoggerFactory

    /**
     * Returns the name logger. If no logger for the name already exists, it is
     * created and configured on the fly and returned. If a logger of the same
     * name already exists, that logger is returned.
     */
    public Logger getLogger(String name) {
        SoftReference<SlingLogger> logger = loggersByCategory.get(name);
        SlingLogger slingLogger = (logger != null) ? logger.get() : null;

        // no logger at all or reference has been collected, create a new one
        if (slingLogger == null) {
            slingLogger = new SlingLogger(name);
            slingLogger.setLoggerConfig(getLoggerConfig(name));
            loggersByCategory.put(name, new SoftReference<SlingLogger>(
                slingLogger));
        }

        return slingLogger;
    }

    // ---------- SlingLogPanel support

    /**
     * Return configured {@link SlingLoggerConfig} instances as an iterator.
     */
    Iterator<SlingLoggerConfig> getSlingLoggerConfigs() {
        return configByPid.values().iterator();
    }

    /**
     * Return configured and implicit {@link SlingLoggerWriter} instances as
     * an iterator.
     */
    Iterator<SlingLoggerWriter> getSlingLoggerWriters() {
        return internalGetSlingLoggerWriters().iterator();
    }

    /**
     * Returns the number of logger configurations active in the system
     */
    int getNumSlingLoggerConfigs() {
        return configByPid.size();
    }

    /**
     * Returns the number of logger writers active in the system
     */
    int getNumSlingLogWriters() {
        return internalGetSlingLoggerWriters().size();
    }

    /**
     * Returns the number of currently user logger categories
     */
    int getNumLoggers() {
        return loggersByCategory.size();
    }

    /**
     * Internal method returns the collection of explicitly configured and
     * implicitly defined logger writers.
     */
    private Collection<SlingLoggerWriter> internalGetSlingLoggerWriters() {
        // configured writers
        Collection<SlingLoggerWriter> writers = new HashSet<SlingLoggerWriter>(
            writerByPid.values());

        // add implicit writers
        for (SlingLoggerWriter slw : writerByFileName.values()) {
            if (slw.getConfigurationPID() == null) {
                writers.add(slw);
            }
        }

        return writers;
    }

    // ---------- Configuration support

    public void updateGlobalConfiguration(
            Dictionary<String, String> configuration)
            throws ConfigurationException {
        // fallback to start default settings when the config is deleted
        if (configuration == null) {
            configuration = defaultConfiguration;
        }

        // set the logger name to a special value to indicate the global
        // (ROOT) logger setting (SLING-529)
        configuration.put(LogManager.LOG_LOGGERS, LogConfigManager.ROOT);

        // update the default log writer and logger configuration
        updateLogWriter(LogManager.PID, configuration);
        updateLoggerConfiguration(LogManager.PID, configuration);
    }

    /**
     * Updates or removes the log writer configuration identified by the
     * <code>pid</code>. In case of log writer removal, any logger
     * configuration referring to the removed log writer is modified to now log
     * to the default log writer.
     * <p>
     * The configuration object is expected to contain the following properties:
     * <dl>
     * <dt>{@link LogManager#LOG_FILE}</dt>
     * <dd>The relative of absolute path/name of the file to log to. If this
     * property is missing or an empty string, the writer writes to standard
     * output</dd>
     * <dt>{@link LogManager#LOG_FILE_SIZE}</dt>
     * <dd>The maximum size of the log file to write before rotating the log
     * file. This property must be a number of be convertible to a number. The
     * actual value may also be suffixed by a size indicator <code>k</code>,
     * <code>kb</code>, <code>m</code>, <code>mb</code>, <code>g</code>
     * or <code>gb</code> representing the respective factors of kilo, mega
     * and giga.If this property is missing or cannot be converted to a number,
     * the default value {@link LogManager#LOG_FILE_SIZE_DEFAULT} is assumed. If
     * the writer writes standard output this property is ignored.</dd>
     * <dt>{@link LogManager#LOG_FILE_NUMBER}</dt>
     * <dd>The maximum number of rotated log files to keep. This property must
     * be a number of be convertible to a number. If this property is missing or
     * cannot be converted to a number, the default value
     * {@link LogManager#LOG_FILE_NUMBER_DEFAULT} is assumed. If the writer
     * writes standard output this property is ignored.</dd>
     * </dl>
     *
     * @param pid The identifier of the log writer to update or remove
     * @param configuration New configuration setting for the log writer or
     *            <code>null</code> to indicate to remove the log writer.
     * @throws ConfigurationException If another log writer already exists for
     *             the same file as configured for the given log writer or if
     *             configuring the log writer fails.
     */
    public void updateLogWriter(String pid, Dictionary<?, ?> configuration)
            throws ConfigurationException {

        if (configuration != null) {
            SlingLoggerWriter slw = writerByPid.get(pid);

            // get the log file parameter and normalize empty string to null
            String logFileName = (String) configuration.get(LogManager.LOG_FILE);
            if (logFileName != null && logFileName.trim().length() == 0) {
                logFileName = null;
            }

            // if we have a file name, make it absolute and correct for our
            // environment and verify there is no other writer already existing
            // for the same file
            if (logFileName != null) {

                // ensure absolute path
                logFileName = getAbsoluteLogFile(logFileName);

                // ensure unique configuration of the log writer
                SlingLoggerWriter existingWriter = writerByFileName.get(logFileName);
                if (existingWriter != null) {
                    if (slw == null) {

                        // this is an implicit writer being configured now
                        slw = existingWriter;
                        slw.setConfigurationPID(pid);
                        writerByPid.put(pid, slw);

                    } else if (!existingWriter.getConfigurationPID().equals(pid)) {

                        // this file is already configured by another LOG_PID
                        throw new ConfigurationException(LogManager.LOG_FILE,
                            "LogFile " + logFileName
                                + " already configured by configuration "
                                + existingWriter.getConfigurationPID());
                    }
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

            // get the log file size
            Object fileSizeProp = configuration.get(LogManager.LOG_FILE_SIZE);
            String fileSize = null;
            if (fileSizeProp != null) {
                fileSize = fileSizeProp.toString();
            }

            if (configureLogWriter(slw, pid, logFileName, fileNum, fileSize) == null) {
                throw new ConfigurationException(LogManager.LOG_FILE,
                    "Cannot create writer for log file " + logFileName);
            }

        } else {

            final SlingLoggerWriter logWriter = writerByPid.remove(pid);
            if (logWriter != null) {

                // make the writer implicit
                logWriter.setConfigurationPID(null);

                // close if unused, otherwise reconfigure to default values
                closeIfUnused(logWriter, true);
            }
        }
    }

    /**
     * Updates or removes the logger configuration indicated by the given
     * <code>pid</code>. If the case of modified categories or removal of the
     * logger configuration, existing loggers will be modified to reflect the
     * correct logger configurations available.
     * <p>
     * The configuration object is expected to contain the following properties:
     * <dl>
     * <dt>{@link LogManager#LOG_PATTERN}</dt>
     * <dd>The <code>MessageFormat</code> pattern to apply to format the log
     * message before writing it to the log writer. If this property is missing
     * or the empty string the default pattern
     * {@link LogManager#LOG_PATTERN_DEFAULT} is used.</dd>
     * <dt>{@link LogManager#LOG_LEVEL}</dt>
     * <dd>The log level to use for log message limitation. The supported
     * values are <code>trace</code>, <code>debug</code>,
     * <code>info</code>, <code>warn</code> and <code>error</code>. Case
     * does not matter. If this property is missing a
     * <code>ConfigurationException</code> is thrown and this logger
     * configuration is not used.</dd>
     * <dt>{@link LogManager#LOG_LOGGERS}</dt>
     * <dd>The logger names to which this configuration applies. As logger
     * names form a hierarchy like Java packages, the listed names also apply to
     * "child names" unless more specific configuration applies for such
     * children. This property may be a single string, an array of strings or a
     * collection of strings. Each string may itself be a comma-separated list of
     * logger names. If this property is missing a
     * <code>ConfigurationException</code> is thrown.</dd>
     * <dt>{@link LogManager#LOG_FILE}</dt>
     * <dd>The name of the log writer to use. This may be the name of a log
     * file configured for any log writer or it may be the configuration PID of
     * such a writer. If this property is missing or empty or does not refer to
     * an existing log writer configuration, the default log writer is used.</dd>
     *
     * @param pid The name of the configuration to update or remove.
     * @param configuration The configuration object.
     * @throws ConfigurationException If the log level and logger names
     *             properties are not configured for the given configuration.
     */
    public void updateLoggerConfiguration(String pid,
            Dictionary<?, ?> configuration) throws ConfigurationException {

        // assume we have to reconfigure the loggers
        boolean reconfigureLoggers = true;

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
                            writer = configureLogWriter(null, null, file, -1, null);
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
                final Set<String> oldCategories = config.getCategories();

                // check whether the log writer is to be changed
                final SlingLoggerWriter oldWriter = config.getLogWriter();

                // reconfigure the configuration
                config.configure(pattern, categories, logLevel, writer);

                if (categories.equals(oldCategories)) {

                    // no need to change category registrations, clear them
                    // also no need to reconfigure the loggers
                    categories.clear();
                    reconfigureLoggers = false;

                } else {

                    // remove the old categories if different from the new ones
                    configByCategory.keySet().removeAll(oldCategories);

                }

                // close the old log writer if replaced and not used any more
                if (oldWriter != writer) {
                    closeIfUnused(oldWriter, false);
                }
            }

            // relink categories
            for (String cat : categories) {
                configByCategory.put(cat, config);
            }

        } else {

            // configuration deleted if null

            // remove configuration from pid list
            SlingLoggerConfig config = configByPid.remove(pid);

            if (config != null) {
                // remove all configured categories
                configByCategory.keySet().removeAll(config.getCategories());

                // close the writer if unused (and unconfigured)
                closeIfUnused(config.getLogWriter(), false);
            }

        }

        // reconfigure existing loggers
        if (reconfigureLoggers) {
            reconfigureLoggers();
        }
    }

    // ---------- Internal helpers ---------------------------------------------

    /**
     * Returns the <code>logFileName</code> argument converted into an
     * absolute path name. If <code>logFileName</code> is already absolute it
     * is returned unmodified. Otherwise it is made absolute by resolving it
     * relative to the root directory set on this instance by the
     * {@link #setRoot(String)} method.
     *
     * @throws NullPointerException if <code>logFileName</code> is
     *             <code>null</code>.
     */
    private String getAbsoluteLogFile(String logFileName) {
        // ensure proper separator in the path (esp. for systems, which do
        // not use "slash" as a separator, e.g Windows)
        logFileName = logFileName.replace('/', File.separatorChar);

        // create a file instance and check whether this is absolute. If not
        // create a new absolute file instance with the root dir and get
        // the absolute path name from that
        File logFile = new File(logFileName);
        if (!logFile.isAbsolute()) {
            logFile = new File(rootDir, logFileName);
            logFileName = logFile.getAbsolutePath();
        }

        // return the correct log file name
        return logFileName;
    }

    /**
     * Reconfigures all loggers such that each logger is supplied with the
     * {@link SlingLoggerConfig} most appropriate to its name. If a registered
     * logger is not used any more, it is removed from the list.
     */
    private void reconfigureLoggers() {
        // assign correct logger configs to all existing/known loggers
        for (Iterator<SoftReference<SlingLogger>> si = loggersByCategory.values().iterator(); si.hasNext();) {
            SlingLogger logger = si.next().get();
            if (logger != null) {
                logger.setLoggerConfig(getLoggerConfig(logger.getName()));
            } else {
                // if the logger has been GC-ed, remove the entry from the map
                si.remove();
            }
        }
    }

    /**
     * Returns a {@link SlingLoggerConfig} instance applicable to the given
     * <code>logger</code> name. This is the instance applicable to a longest
     * match log. If no such instance exists, the default logger configuration
     * is returned.
     */
    private SlingLoggerConfig getLoggerConfig(String logger) {
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

        return defaultLoggerConfig;
    }

    /**
     * Decomposes the <code>loggers</code> configuration object into a set of
     * logger names. The <code>loggers</code> object may be a single string,
     * an array of strings or a collection of strings. Each string may in turn be a
     * comma-separated list of strings. Each entry makes up an entry in the
     * resulting set.
     *
     * @param loggers The configuration object to be decomposed. If this is
     *            <code>null</code>, <code>null</code> is returned
     *            immediately
     * @return The set of logger names provided by the <code>loggers</code>
     *         object or <code>null</code> if the <code>loggers</code>
     *         object itself is <code>null</code>.
     */
    private Set<String> toCategoryList(Object loggers) {

        // quick exit if there is no configuration
        if (loggers == null) {
            return null;
        }

        // prepare set of names (used already in case loggers == ROOT)
        Set<String> loggerNames = new HashSet<String>();

        // in case of the special setting ROOT, return a set of just the
        // root logger name (SLING-529)
        if (loggers == ROOT) {
            loggerNames.add(ROOT);
            return loggerNames;
        }

        // convert the loggers object to an array
        Object[] loggersArray;
        if (loggers.getClass().isArray()) {
            loggersArray = (Object[]) loggers;
        } else if (loggers instanceof Collection<?>) {
            loggersArray = ((Collection<?>) loggers).toArray();
        } else {
            loggersArray = new Object[] { loggers };
        }

        // conver the array of potentially comma-separated logger names
        // into the set of logger names
        for (Object loggerObject : loggersArray) {
            if (loggerObject != null) {
                String[] splitLoggers = loggerObject.toString().split(",");
                for (String logger : splitLoggers) {
                    logger = logger.trim();
                    if (logger.length() > 0) {
                        loggerNames.add(logger);
                    }
                }
            }
        }

        // return those names
        return loggerNames;
    }

    /**
     * Configures and returns a {@link SlingLoggerWriter}. If the
     * <code>pid</code> is not <code>null</code> the writer is also added to the
     * by pid map. If the <code>fileName</code> is not <code>null</code> the
     * writer is also added to the file name map.
     *
     * @param writer The {@link SlingLoggerWriter} to configure. If this is
     *            <code>null</code> a new instance is created.
     * @param pid The configuration PID to set on the <code>writer</code>. This
     *            may be <code>null</code> to indicate that the logger writer is
     *            not configured by any configuration.
     * @param fileName The name of the file to log to.
     * @param fileNum The number of log files to keep (if rotating by size) or
     *            -1 to assume the default (
     *            {@link java.util.logging.LogManager#LOG_FILE_NUMBER_DEFAULT}).
     * @param threshold The log rotation threashold (size or data/time format
     *            pattern or <code>null</code> to assume the default (
     *            {@link java.util.logging.LogManager#LOG_FILE_SIZE_DEFAULT}).
     * @return The {@link SlingLoggerWriter} or <code>null</code> if an error
     *         occurrs configuring the writer.
     */
    private SlingLoggerWriter configureLogWriter(SlingLoggerWriter writer,
            String pid, String fileName, int fileNum, String threshold) {

        // create the writer instance if it is new
        if (writer == null) {
            writer = new SlingLoggerWriter(pid);
        } else {
            writer.setConfigurationPID(pid);
        }

        if (fileNum < 0) {
            fileNum = LogManager.LOG_FILE_NUMBER_DEFAULT;
        }

        if (threshold == null || threshold.length() == 0) {
            threshold = LogManager.LOG_FILE_SIZE_DEFAULT;
        }

        try {
            writer.configure(fileName, fileNum, threshold);
        } catch (IOException ioe) {
            internalFailure("Cannot create log file " + fileName, ioe);
            internalFailure("Logging to the console", null);
            return null;
        }

        // add to maps
        if (pid != null) {
            writerByPid.put(pid, writer);
        }
        if (fileName != null) {
            writerByFileName.put(fileName, writer);
        }

        // everything set and done
        return writer;
    }

    /**
     * Closes or resets the given <code>logWriter</code> if it is not referred
     * to by any logger config or writer configuration.
     *
     * @param logWriter The {@link SlingLoggerWriter} to close or (optionally)
     *            reconfigure.
     * @param reset Whether the log writer should be reset to default values if
     *            it is still referred to by any logger configuration.
     */
    private void closeIfUnused(SlingLoggerWriter logWriter, boolean reset) {

        // The log writer is based on configuration, don't touch
        if (logWriter.getConfigurationPID() != null) {
            return;
        }

        // "Implicit" writer : check for references
        for (SlingLoggerConfig config : configByPid.values()) {
            if (config.getLogWriter() == logWriter) {

                // optionally reconfigure to default values
                if (reset) {
                    try {
                        logWriter.configure(logWriter.getPath(),
                            LogManager.LOG_FILE_NUMBER_DEFAULT,
                            LogManager.LOG_FILE_SIZE_DEFAULT);
                    } catch (IOException ioe) {
                        internalFailure(
                            "Cannot reset the log writer to default configuration",
                            ioe);
                    }
                }

                // done here...
                return;
            }
        }
        // invariant: writer is not used and not configured any more

        // remove from the writer file name map
        String path = logWriter.getPath();
        if (path != null) {
            writerByFileName.remove(path);
        }

        // close it to clean up
        try {
            logWriter.close();
        } catch (IOException ioe) {
            // don't care
        }
    }
}
