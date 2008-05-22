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

}
