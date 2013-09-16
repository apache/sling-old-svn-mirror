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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;

/**
 * The <code>LogWriter</code> class encapsulates the OSGi configuration for a
 * log writer and provides methods to access these to create an Appender.
 */
public class LogWriter {

    /**
     * Special fileName for which Console Appender would be created
     */
    public static final String FILE_NAME_CONSOLE = "CONSOLE";

    private static final long FACTOR_KB = 1024;

    private static final long FACTOR_MB = 1024 * FACTOR_KB;

    private static final long FACTOR_GB = 1024 * FACTOR_MB;

    /**
     * Regular expression matching a maximum file size specification. This
     * pattern case-insensitively matches a number and an optional factor
     * specifier of the forms k, kb, m, mb, g, or gb.
     */
    private static final Pattern SIZE_SPEC = Pattern.compile("([\\d]+)([kmg]b?)?", Pattern.CASE_INSENSITIVE);

    /**
     * The PID of the configuration from which this instance has been
     * configured. If this is <code>null</code> this instance is an implicitly
     * created instance which is not tied to any configuration.
     */
    private final String configurationPID;

    private final String fileName;

    private final int logNumber;

    private final String logRotation;

    private final String appenderName;

    public LogWriter(String configurationPID, String appenderName, int logNumber, String logRotation, String fileName) {
        this.appenderName = appenderName;
        if (fileName == null || fileName.length() == 0) {
            fileName = FILE_NAME_CONSOLE;
        }

        if (logNumber < 0) {
            logNumber = LogConfigManager.LOG_FILE_NUMBER_DEFAULT;
        }

        if (logRotation == null || logRotation.length() == 0) {
            logRotation = LogConfigManager.LOG_FILE_SIZE_DEFAULT;
        }

        this.configurationPID = configurationPID;
        this.fileName = fileName;
        this.logNumber = logNumber;
        this.logRotation = logRotation;
    }

    public LogWriter(String appenderName,String fileName, int logNumber, String logRotation) {
        this(null, appenderName, logNumber, logRotation, fileName);
    }

    public String getConfigurationPID() {
        return configurationPID;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAppenderName() {
        return appenderName;
    }

    public int getLogNumber() {
        return logNumber;
    }

    public String getLogRotation() {
        return logRotation;
    }

    public boolean isImplicit() {
        return configurationPID == null;
    }

    public Appender<ILoggingEvent> createAppender(final Context context, final Encoder<ILoggingEvent> encoder) {

        OutputStreamAppender<ILoggingEvent> appender;
        if (FILE_NAME_CONSOLE.equals(fileName)) {
            appender = new ConsoleAppender<ILoggingEvent>();
            appender.setName(FILE_NAME_CONSOLE);
        } else {
            SlingRollingFileAppender<ILoggingEvent> rollingAppender = new SlingRollingFileAppender<ILoggingEvent>();
            rollingAppender.setAppend(true);
            rollingAppender.setFile(getFileName());

            Matcher sizeMatcher = SIZE_SPEC.matcher(getLogRotation());
            if (sizeMatcher.matches()) {
                // group 1 is the base size and is an integer number
                final long baseSize = Long.parseLong(sizeMatcher.group(1));

                // this will take the final size value
                final long maxSize;

                // group 2 is optional and is the size spec. If not null it is
                // at least one character long and the first character is enough
                // for use to know (the second is of no use here)
                final String factorString = sizeMatcher.group(2);
                if (factorString == null) {
                    // no factor define, hence no multiplication
                    maxSize = baseSize;
                } else {
                    switch (factorString.charAt(0)) {
                        case 'k':
                        case 'K':
                            maxSize = baseSize * FACTOR_KB;
                            break;
                        case 'm':
                        case 'M':
                            maxSize = baseSize * FACTOR_MB;
                            break;
                        case 'g':
                        case 'G':
                            maxSize = baseSize * FACTOR_GB;
                            break;
                        default:
                            // we don't really expect this according to the
                            // pattern
                            maxSize = baseSize;
                    }
                }

                SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<ILoggingEvent>();
                triggeringPolicy.setMaxFileSize(String.valueOf(maxSize));
                triggeringPolicy.setContext(context);
                triggeringPolicy.start();
                rollingAppender.setTriggeringPolicy(triggeringPolicy);

                FixedWindowRollingPolicy pol = new FixedWindowRollingPolicy();
                pol.setMinIndex(1);
                pol.setMaxIndex(getLogNumber());
                pol.setFileNamePattern(getFileName() + "%i");
                pol.setContext(context);
                pol.setParent(rollingAppender);
                pol.start();
                rollingAppender.setRollingPolicy(pol);
            } else {
                TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<ILoggingEvent>();
                policy.setFileNamePattern(createFileNamePattern(getFileName(), getLogRotation()));
                policy.setMaxHistory(getLogNumber());
                policy.setContext(context);
                policy.setParent(rollingAppender);
                policy.start();
                rollingAppender.setTriggeringPolicy(policy);
            }

            rollingAppender.setLogWriter(this);
            rollingAppender.setName(getAppenderName());

            appender = rollingAppender;
        }

        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.start();
        return appender;
    }

    public static String createFileNamePattern(String fileName, String pattern) {
        // Default file name pattern "'.'yyyy-MM-dd"
        // http://sling.apache.org/site/logging.html#Logging-ScheduledRotation
        if (pattern.startsWith("'.'")) {
            pattern = pattern.substring(3); // 3 = '.' length
            pattern = ".%d{" + pattern + "}";
        }

        // Legacy pattern which does not start with '.' Just wrap them with %d{}
        if (!pattern.contains("%d{")) {
            pattern = "%d{" + pattern + "}";
        }
        return fileName + pattern;

    }

    @Override
    public String toString() {
        return "LogWriter{" + "configurationPID='" + configurationPID + '\'' + ", fileName='" + fileName + '\''
            + ", logNumber=" + logNumber + ", logRotation='" + logRotation + '\'' + '}';
    }
}
