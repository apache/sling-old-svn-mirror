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

package org.apache.sling.distribution.log.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.log.DistributionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * Default implementation of a {@link DistributionLog}
 */
public class DefaultDistributionLog implements DistributionLog {

    private final DistributionComponentKind kind;
    private final String name;
    private final LinkedList<String> lines = new LinkedList<String>();
    private final Logger logger;
    private final LogLevel logLevel;

    public DefaultDistributionLog(DistributionComponentKind kind, String name, Class clazz, LogLevel logLevel) {

        this.kind = kind;
        this.name = name;
        this.logLevel = logLevel;
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public String getName() {
        return name;
    }

    public DistributionComponentKind getKind() {
        return kind;
    }

    public List<String> getLines() {
        synchronized (lines) {
            return new ArrayList<String>(lines);
        }
    }

    private void internalLog(LogLevel level, String fmt, Object... objects) {
        try {
            FormattingTuple fmtp = MessageFormatter.arrayFormat(fmt, objects);
            internalLog(level, fmtp.getMessage());
        } catch (Throwable e) {
            logger.error("cannot add entry log", e);
        }
    }

    private void internalLog(LogLevel level, String message) {
        if (level.cardinal < logLevel.cardinal) {
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");
        Calendar cal = Calendar.getInstance();


        String log = dateFormat.format(cal.getTime()) +
                " - " +
                level.name() +
                " - " +
                message;
        synchronized (lines) {
            lines.add(log);
            int maxLines = 1000;
            while (lines.size() > maxLines) {
                lines.removeFirst();
            }
        }
    }

    public void error(String fmt, Object... objects) {
        String specificFmt = getSpecificString(fmt);
        logger.error(specificFmt, objects);
        internalLog(LogLevel.ERROR, fmt, objects);
    }

    public void info(String fmt, Object... objects) {
        String specificFmt = getSpecificString(fmt);
        logger.info(specificFmt, objects);
        internalLog(LogLevel.INFO, fmt, objects);
    }

    public void info(boolean silent, String fmt, Object... objects) {
        if (silent) {
            debug(fmt, objects);
        } else {
            info(fmt, objects);
        }
    }

    public void debug(String fmt, Object... objects) {
        String specificFmt = getSpecificString(fmt);
        logger.debug(specificFmt, objects);
        internalLog(LogLevel.DEBUG, fmt, objects);
    }

    public void warn(String fmt, Object... objects) {
        String specificFmt = getSpecificString(fmt);
        logger.warn(specificFmt, objects);
        internalLog(LogLevel.WARN, fmt, objects);
    }


    private String getSpecificString(String fmt) {
        return "[" + kind.getName() + "][" + name + "] " + fmt;
    }


    /**
     * Log level
     */
    public enum LogLevel {

        DEBUG(0),

        INFO(1),

        WARN(2),

        ERROR(3);

        public final int cardinal;

        LogLevel(int cardinal) {
            this.cardinal = cardinal;
        }
    }
}
