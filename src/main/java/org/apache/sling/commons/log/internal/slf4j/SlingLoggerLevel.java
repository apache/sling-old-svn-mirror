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

import org.slf4j.spi.LocationAwareLogger;

public enum SlingLoggerLevel {

    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    /**
     * Translates SLF4J logging levels into {@link SlingLoggerLevel}
     *
     * @param level The SLF4J logging level
     * @return The matching {@link SlingLoggerLevel}
     */
    public static SlingLoggerLevel fromSlf4jLevel(int level) {
        SlingLoggerLevel slingLevel;

        if (level < LocationAwareLogger.DEBUG_INT) {
            slingLevel = SlingLoggerLevel.TRACE;
        } else if (level < LocationAwareLogger.INFO_INT) {
            slingLevel = SlingLoggerLevel.DEBUG;
        } else if (level < LocationAwareLogger.WARN_INT) {
            slingLevel = SlingLoggerLevel.INFO;
        } else if (level < LocationAwareLogger.ERROR_INT) {
            slingLevel = SlingLoggerLevel.WARN;
        } else {
            slingLevel = SlingLoggerLevel.ERROR;
        }

        return slingLevel;
    }

    /**
     * Converts the given {@code levelString} to a {@link SlingLoggerLevel}
     * instance or {@code null} of not possible. If the value converted to an
     * upper case string is one of the {@link SlingLoggerLevel} constant names,
     * the respective constant is returned. Otherwise the string is converted to
     * an int (if possible) and the following mapping is applied:
     * <table>
     * <tr>
     * <th>level</th>
     * <th>SlingLoggerLevel</th>
     * </tr>
     * <tr>
     * <td>0 or 1</td>
     * <td>{@link #ERROR}</td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>{@link #WARN}</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>{@link #INFO}</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>{@link #DEBUG}</td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td>{@link #TRACE}</td>
     * </tr>
     * </table>
     * <p>
     * If the string value is not one of the constant names, or cannot be
     * converted to a number or the number is not one of the supported values,
     * {@code null} is returned.
     *
     * @param levelString The string value to convert into a
     *            {@code SlingLoggerLevel}
     * @return The {@link SlingLoggerLevel} instance or {@code null} if the
     *         string value cannot be converted.
     */
    public static SlingLoggerLevel fromString(final String levelString) {
        if (levelString != null && levelString.length() > 0) {
            try {
                return SlingLoggerLevel.valueOf(levelString.toUpperCase());
            } catch (IllegalArgumentException iae) {
                // ignore
            }

            try {
                final int level = Integer.parseInt(levelString);
                switch (level) {
                    case 0:
                    case 1:
                        return ERROR;
                    case 2:
                        return WARN;
                    case 3:
                        return INFO;
                    case 4:
                        return DEBUG;
                    case 5:
                        return TRACE;
                }
            } catch (NumberFormatException nfe) {
                // ignore
            }
        }

        return null;
    }
}
