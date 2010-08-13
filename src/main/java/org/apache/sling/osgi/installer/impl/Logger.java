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
package org.apache.sling.osgi.installer.impl;

import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * Logger support.
 * If a log service is available, log messages go
 * to the log service - if no log service is
 * available, log messages are ignored.
 */
public class Logger {

    /**
     * Log a debug message.
     */
    public static void logDebug(final String message) {
        log(LogService.LOG_DEBUG, message, null);
    }

    /**
     * Log a debug message with exception.
     */
    public static void logDebug(final String message, final Throwable t) {
        log(LogService.LOG_DEBUG, message, t);
    }

    /**
     * Log a info message.
     */
    public static void logInfo(final String message) {
        log(LogService.LOG_INFO, message, null);

    }

    /**
     * Log a info message with exception.
     */
    public static void logInfo(final String message, final Throwable t) {
        log(LogService.LOG_INFO, message, t);

    }

    /**
     * Log a warning message.
     */
    public static void logWarn(final String message) {
        log(LogService.LOG_WARNING, message, null);

    }

    /**
     * Log a warning message with exception.
     */
    public static void logWarn(final String message, final Throwable t) {
        log(LogService.LOG_WARNING, message, t);
    }

    /**
     * Internal method for logging.
     * This method checks if the LogService is available and only then logs
     */
    private static void log(final int level, final String message, final Throwable t) {
        final ServiceTracker tracker = LOGGER_TRACKER;
        if ( tracker != null ) {
            final LogService ls = (LogService) tracker.getService();
            if ( ls != null ) {
                if ( t != null ) {
                    ls.log(level, message, t);
                } else {
                    ls.log(level, message);
                }
            }
        }
    }

    private static ServiceTracker LOGGER_TRACKER;

    /** Set the logger tracker. */
    static void setTracker(final ServiceTracker tracker) {
        LOGGER_TRACKER = tracker;
    }
}

