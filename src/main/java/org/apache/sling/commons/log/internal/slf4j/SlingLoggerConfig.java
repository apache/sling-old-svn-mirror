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

import java.io.IOException;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Marker;

/**
 * The <code>SlingLoggerConfig</code> conveys the logger configuration in
 * terms of writer used and log level set. The respective instances of this
 * class are also used for the actual message formatting and writing.
 */
class SlingLoggerConfig {

    private String configPid;

    private Set<String> categories;

    private SlingLoggerLevel level;

    private MessageFormat format;

    private SlingLoggerWriter writer;

    SlingLoggerConfig(String pid, String pattern, Set<String> categories,
            SlingLoggerLevel level, SlingLoggerWriter writer) {
        this.configPid = pid;
        configure(pattern, categories, level, writer);
    }

    void configure(String pattern, Set<String> categories,
            SlingLoggerLevel level, SlingLoggerWriter writer) {
        this.format = new MessageFormat(pattern);
        this.categories = new HashSet<String>(categories);
        this.level = level;
        this.writer = writer;
    }

    String getConfigPid() {
        return configPid;
    }

    boolean hasCategory(String category) {
        return categories.contains(category);
    }

    Set<String> getCategories() {
        return categories;
    }

    SlingLoggerWriter getLogWriter() {
        return writer;
    }

    void setLogWriter(SlingLoggerWriter writer) {
        this.writer = writer;
    }

    SlingLoggerLevel getLogLevel() {
        return level;
    }

    boolean isLevel(SlingLoggerLevel reference) {
        return level.compareTo(reference) <= 0;
    }

    void setLogLevel(SlingLoggerLevel level) {
        this.level = level;
    }

    void formatMessage(StringBuffer buffer, Marker marker, String name,
            SlingLoggerLevel level, String msg, String fqcn) {
        // create the formatted log line; use a local copy because the field
        // may be exchanged while we are trying to use it
        MessageFormat myFormat = format;
        synchronized (myFormat) {
            myFormat.format(new Object[] { new Date(), marker,
                Thread.currentThread().getName(), name, level.toString(), msg,
                fqcn }, buffer, new FieldPosition(0));
        }
    }

    void printMessage(String message, boolean needsEOL) {
        // use a local copy because the field may be exchanged while we are
        // trying to use it
        SlingLoggerWriter myOutput = writer;
        synchronized (myOutput) {
            try {
                myOutput.write(message);

                // write line termination or flush, whatever is needed
                if (needsEOL) {
                    myOutput.writeln();
                } else {
                    myOutput.flush();
                }

            } catch (IOException ioe) {
                LogConfigManager.internalFailure("Failed logging message: "
                    + message, ioe);
            }
        }
    }

}
