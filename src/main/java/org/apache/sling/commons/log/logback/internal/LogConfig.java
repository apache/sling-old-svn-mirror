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
package org.apache.sling.commons.log.logback.internal;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;

public class LogConfig {
    private static final String[] LEGACY_MARKERS = {
        "{0}", "{1}", "{2}", "{3}", "{4}", "{5}"
    };

    private final String configPid;

    private final Set<String> categories;

    private final Level logLevel;

    private final String pattern;

    private final String logWriterName;

    private final LogWriterProvider logWriterProvider;

    private final LoggerContext loggerContext;

    private final boolean isAdditiv;

    LogConfig(LogWriterProvider logWriterProvider, final String pattern, Set<String> categories, Level logLevel,
            String logWriterName, final boolean isAdditiv, String configPid, LoggerContext loggerContext) {
        this.logWriterProvider = logWriterProvider;
        this.configPid = configPid;
        this.pattern = pattern;
        this.categories = Collections.unmodifiableSet(categories);
        this.logLevel = logLevel;
        this.logWriterName = logWriterName;
        this.loggerContext = loggerContext;
        this.isAdditiv = isAdditiv;
    }

    public String getConfigPid() {
        return configPid;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public String getLogWriterName() {
        return logWriterName;
    }

    public boolean isAppenderDefined() {
        return logWriterName != null;
    }

    public boolean isAdditive() {
        return this.isAdditiv;
    }

    public LogWriter getLogWriter() {
        return logWriterProvider.getLogWriter(getLogWriterName());
    }

    public PatternLayout createLayout() {
        // The java.util.MessageFormat pattern to use for formatting log
        // messages with the root logger.
        // This is a java.util.MessageFormat pattern supporting up to six
        // arguments:
        // {0} The timestamp of type java.util.Date,
        // {1} the log marker,
        // {2} the name of the current thread,
        // {3} the name of the logger,
        // {4} the debug level and
        // {5} the actual debug message
        Pattern date = Pattern.compile("\\{0,date,(.+?)\\}");
        Matcher m = date.matcher(pattern);
        String logBackPattern = pattern;

        if (m.matches()) {
            // If legacy pattern then transform the date format
            logBackPattern = m.replaceAll("%d'{'$1'}'");
        }

        boolean legacyPattern = false;
        for (String marker : LEGACY_MARKERS) {
            if (logBackPattern.contains(marker)) {
                legacyPattern = true;
                break;
            }
        }

        if (legacyPattern) {
            // Default {0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5}
            // Convert patterns to %d{dd.MM.yyyy HH:mm:ss.SSS} *%level*
            // [%thread] %logger %msg%n
            logBackPattern = MessageFormat.format(logBackPattern, "zero", "%marker", "%thread", "%logger", "%level",
                "%message") + "%n";
        }

        PatternLayout pl = new PatternLayout();
        pl.setPattern(logBackPattern);
        pl.setOutputPatternAsHeader(false);
        pl.setContext(loggerContext);
        pl.start();
        return pl;
    }

    @Override
    public String toString() {
        return "LogConfig{" + "configPid='" + configPid + '\'' + ", categories=" + categories + ", logLevel="
            + logLevel + ", logWriterName='" + logWriterName + '\'' + '}';
    }

    public static interface LogWriterProvider {
        LogWriter getLogWriter(String writerName);
    }

}
