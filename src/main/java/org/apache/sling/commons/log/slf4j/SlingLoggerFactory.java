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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.commons.log.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public final class SlingLoggerFactory implements ILoggerFactory {

    private static final SlingLoggerFactory instance = new SlingLoggerFactory();

    private final Map<String, SlingLogger> loggers = new HashMap<String, SlingLogger>();

    /**
     * The logger level currently configured to this factory. Before
     * configuration, this is set to {@link SlingLoggerLevel#INFO}.
     */
    private SlingLoggerLevel level = SlingLoggerLevel.INFO;

    /**
     * The currently active log output. Before configuration, logging output is
     * set to go to the console.
     */
    private SlingLogWriter output = new SlingLogWriter();

    /**
     * The currently active message format to generate the log message entries.
     * Before configuration this is set to
     * {@value LogManager#LOG_PATTERN_DEFAULT}.
     */
    private MessageFormat messageFormat = new MessageFormat(
        LogManager.LOG_PATTERN_DEFAULT);

    /**
     * Returns the singleton instance of this class.
     */
    public static SlingLoggerFactory getInstance() {
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

    // private constructor to prevent instantiation. This is a singleton class
    private SlingLoggerFactory() {
    }

    // ---------- ILoggerFactory interface -------------------------------------

    /**
     * Returns a logger for the given name. If such a logger already exists the
     * same logger is returned. Otherwise a new instance is created and
     * configured with the current logging level, output and message format.
     * 
     * @param name The name of the logger to return
     */
    public Logger getLogger(String name) {
        synchronized (loggers) {
            SlingLogger logger = loggers.get(name);
            if (logger == null) {
                logger = createLogger(name);
                loggers.put(name, logger);
            }
            return logger;
        }
    }

    // ---------- Sling specific ILoggerFactory stuff --------------------------

    /**
     * Configures this factory and all existing loggers with the new log level,
     * output and message format.
     * 
     * @param logLevel The log level to be set. If this is not a valid
     *            {@link SlingLoggerLevel} value, the default <code>INFO</code>
     *            is assumed.
     * @param output The new ouptut channel
     * @param messageFormat The new message format
     */
    public void configure(String logLevel, SlingLogWriter output,
            MessageFormat messageFormat) {

        // close the old output if existing
        if (this.output != null) {
            try {
                this.output.close();
            } catch (IOException ioe) {
                internalFailure("Problem closing old output", ioe);
            }
        }

        try {
            this.level = SlingLoggerLevel.valueOf(logLevel);
        } catch (Exception e) {
            this.level = SlingLoggerLevel.INFO;
        }

        this.output = output;
        this.messageFormat = messageFormat;

        synchronized (loggers) {
            for (SlingLogger logger : loggers.values()) {
                logger.configure(level, output, messageFormat);
            }
        }
    }

    public void close() {
        if (output != null) {
            try {
                output.close();
            } catch (IOException ioe) {
                internalFailure("Problem closing the output", ioe);
            }
        }
    }

    public List<SlingLogger> getLoggerList() {
        synchronized (loggers) {
            return new ArrayList<SlingLogger>(loggers.values());
        }
    }

    public SlingLogger getSlingLogger(String name) {
        synchronized (loggers) {
            return loggers.get(name);
        }
    }

    private SlingLogger createLogger(String name) {
        SlingLogger logger = new SlingLogger(name);
        logger.configure(level, output, messageFormat);
        return logger;
    }

}
