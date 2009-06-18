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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

class SlingLogger implements LocationAwareLogger {

    private final String name;

    private SlingLoggerConfig config;

    SlingLogger(String name) {
        this.name = name;
    }

    void setLoggerConfig(SlingLoggerConfig config) {
        this.config = config;
    }

    // ---------- Actual Loger Entry writing -----------------------------------

    private void log(Marker marker, SlingLoggerLevel level, String msg,
            Throwable t) {
        log(marker, null, level, msg, t);
    }

    private void log(Marker marker, String fqcn, SlingLoggerLevel level,
            String msg, Throwable t) {
        StringWriter writer = new StringWriter();

        // create the formatted log line; use a local copy because the field
        // may be exchanged while we are trying to use it
        config.formatMessage(writer.getBuffer(), marker, getName(), level, msg, fqcn);

        // marker indicating whether a line terminator is to be written after
        // the message: If a throwable is given, the stacktrace generator
        // writes a final line terminator and hence we do need to do it
        // ourselves
        boolean needsEOL = true;

        // append stacktrace if throwable is not null
        if (t != null) {
            writer.write(' ');
            PrintWriter pw = new PrintWriter(writer);
            t.printStackTrace(pw);
            pw.flush();

            // just flush the output, no EOL needed
            needsEOL = false;
        }

        // use a local copy because the field may be exchanged while we are
        // trying to use it
        config.printMessage(writer.toString(), needsEOL);
    }

    // ---------- Logger interface ---------------------------------------------

    public String getName() {
        return name;
    }

    public void trace(String msg) {
        if (isTraceEnabled()) {
            log(null, SlingLoggerLevel.TRACE, msg, null);
        }
    }

    public void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            log(null, SlingLoggerLevel.TRACE, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void trace(String format, Object[] argArray) {
        if (isTraceEnabled()) {
            log(null, SlingLoggerLevel.TRACE, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            log(null, SlingLoggerLevel.TRACE, msg, t);
        }
    }

    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            log(null, SlingLoggerLevel.TRACE, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void trace(Marker marker, String msg) {
        if (isTraceEnabled(marker)) {
            log(marker, SlingLoggerLevel.TRACE, msg, null);
        }
    }

    public void trace(Marker marker, String format, Object arg) {
        if (isTraceEnabled(marker)) {
            log(marker, SlingLoggerLevel.TRACE, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void trace(Marker marker, String format, Object[] argArray) {
        if (isTraceEnabled(marker)) {
            log(marker, SlingLoggerLevel.TRACE, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void trace(Marker marker, String msg, Throwable t) {
        if (isTraceEnabled(marker)) {
            log(marker, SlingLoggerLevel.TRACE, msg, t);
        }
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (isTraceEnabled(marker)) {
            log(marker, SlingLoggerLevel.TRACE, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void debug(String msg) {
        if (isDebugEnabled()) {
            log(null, SlingLoggerLevel.DEBUG, msg, null);
        }
    }

    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            log(null, SlingLoggerLevel.DEBUG, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void debug(String format, Object[] argArray) {
        if (isDebugEnabled()) {
            log(null, SlingLoggerLevel.DEBUG, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            log(null, SlingLoggerLevel.DEBUG, msg, t);
        }
    }

    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            log(null, SlingLoggerLevel.DEBUG, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void debug(Marker marker, String msg) {
        if (isDebugEnabled(marker)) {
            log(marker, SlingLoggerLevel.DEBUG, msg, null);
        }
    }

    public void debug(Marker marker, String format, Object arg) {
        if (isDebugEnabled(marker)) {
            log(marker, SlingLoggerLevel.DEBUG, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void debug(Marker marker, String format, Object[] argArray) {
        if (isDebugEnabled(marker)) {
            log(marker, SlingLoggerLevel.DEBUG, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void debug(Marker marker, String msg, Throwable t) {
        if (isDebugEnabled(marker)) {
            log(marker, SlingLoggerLevel.DEBUG, msg, t);
        }
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (isDebugEnabled(marker)) {
            log(marker, SlingLoggerLevel.DEBUG, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void info(String msg) {
        if (isInfoEnabled()) {
            log(null, SlingLoggerLevel.INFO, msg, null);
        }
    }

    public void info(String format, Object arg) {
        if (isInfoEnabled()) {
            log(null, SlingLoggerLevel.INFO, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void info(String format, Object[] argArray) {
        if (isInfoEnabled()) {
            log(null, SlingLoggerLevel.INFO, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            log(null, SlingLoggerLevel.INFO, msg, t);
        }
    }

    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            log(null, SlingLoggerLevel.INFO, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void info(Marker marker, String msg) {
        if (isInfoEnabled(marker)) {
            log(marker, SlingLoggerLevel.INFO, msg, null);
        }
    }

    public void info(Marker marker, String format, Object arg) {
        if (isInfoEnabled(marker)) {
            log(marker, SlingLoggerLevel.INFO, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void info(Marker marker, String format, Object[] argArray) {
        if (isInfoEnabled(marker)) {
            log(marker, SlingLoggerLevel.INFO, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void info(Marker marker, String msg, Throwable t) {
        if (isInfoEnabled(marker)) {
            log(marker, SlingLoggerLevel.INFO, msg, t);
        }
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (isInfoEnabled(marker)) {
            log(marker, SlingLoggerLevel.INFO, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void warn(String msg) {
        if (isWarnEnabled()) {
            log(null, SlingLoggerLevel.WARN, msg, null);
        }
    }

    public void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            log(null, SlingLoggerLevel.WARN, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void warn(String format, Object[] argArray) {
        if (isWarnEnabled()) {
            log(null, SlingLoggerLevel.WARN, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            log(null, SlingLoggerLevel.WARN, msg, t);
        }
    }

    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            log(null, SlingLoggerLevel.WARN, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void warn(Marker marker, String msg) {
        if (isWarnEnabled(marker)) {
            log(marker, SlingLoggerLevel.WARN, msg, null);
        }
    }

    public void warn(Marker marker, String format, Object arg) {
        if (isWarnEnabled(marker)) {
            log(marker, SlingLoggerLevel.WARN, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void warn(Marker marker, String format, Object[] argArray) {
        if (isWarnEnabled(marker)) {
            log(marker, SlingLoggerLevel.WARN, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void warn(Marker marker, String msg, Throwable t) {
        if (isWarnEnabled(marker)) {
            log(marker, SlingLoggerLevel.WARN, msg, t);
        }
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (isWarnEnabled(marker)) {
            log(marker, SlingLoggerLevel.WARN, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void error(String msg) {
        if (isErrorEnabled()) {
            log(null, SlingLoggerLevel.ERROR, msg, null);
        }
    }

    public void error(String format, Object arg) {
        if (isErrorEnabled()) {
            log(null, SlingLoggerLevel.ERROR, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void error(String format, Object[] argArray) {
        if (isErrorEnabled()) {
            log(null, SlingLoggerLevel.ERROR, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            log(null, SlingLoggerLevel.ERROR, msg, t);
        }
    }

    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            log(null, SlingLoggerLevel.ERROR, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public void error(Marker marker, String msg) {
        if (isErrorEnabled(marker)) {
            log(marker, SlingLoggerLevel.ERROR, msg, null);
        }
    }

    public void error(Marker marker, String format, Object arg) {
        if (isErrorEnabled(marker)) {
            log(marker, SlingLoggerLevel.ERROR, MessageFormatter.format(format,
                arg), null);
        }
    }

    public void error(Marker marker, String format, Object[] argArray) {
        if (isErrorEnabled(marker)) {
            log(marker, SlingLoggerLevel.ERROR, MessageFormatter.arrayFormat(
                format, argArray), null);
        }
    }

    public void error(Marker marker, String msg, Throwable t) {
        if (isErrorEnabled(marker)) {
            log(marker, SlingLoggerLevel.ERROR, msg, t);
        }
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (isErrorEnabled(marker)) {
            log(marker, SlingLoggerLevel.ERROR, MessageFormatter.format(format,
                arg1, arg2), null);
        }
    }

    public boolean isTraceEnabled() {
        return config.isLevel(SlingLoggerLevel.TRACE);
    }

    public boolean isTraceEnabled(Marker marker) {
        return config.isLevel(SlingLoggerLevel.TRACE);
    }

    public boolean isDebugEnabled() {
        return config.isLevel(SlingLoggerLevel.DEBUG);
    }

    public boolean isDebugEnabled(Marker marker) {
        return config.isLevel(SlingLoggerLevel.DEBUG);
    }

    public boolean isInfoEnabled() {
        return config.isLevel(SlingLoggerLevel.INFO);
    }

    public boolean isInfoEnabled(Marker marker) {
        return config.isLevel(SlingLoggerLevel.INFO);
    }

    public boolean isWarnEnabled() {
        return config.isLevel(SlingLoggerLevel.WARN);
    }

    public boolean isWarnEnabled(Marker marker) {
        return config.isLevel(SlingLoggerLevel.WARN);
    }

    public boolean isErrorEnabled() {
        return config.isLevel(SlingLoggerLevel.ERROR);
    }

    public boolean isErrorEnabled(Marker marker) {
        return config.isLevel(SlingLoggerLevel.ERROR);
    }

    public void log(Marker marker, String fqcn, int level, String message,
            Throwable t) {
        SlingLoggerLevel slingLevel = SlingLoggerLevel.fromSlf4jLevel(level);
        if (config.isLevel(slingLevel)) {
            log(marker, fqcn, slingLevel, message, t);
        }
    }
}
