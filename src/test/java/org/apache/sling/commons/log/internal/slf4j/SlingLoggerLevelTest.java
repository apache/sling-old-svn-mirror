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

import junit.framework.TestCase;

public class SlingLoggerLevelTest extends TestCase {

    public void test_fromSlf4jLevel() {
        assertEquals(SlingLoggerLevel.TRACE, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.TRACE_INT-1));
        assertEquals(SlingLoggerLevel.TRACE, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.TRACE_INT));
        assertEquals(SlingLoggerLevel.TRACE, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.TRACE_INT+1));

        assertEquals(SlingLoggerLevel.TRACE, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.DEBUG_INT-1));
        assertEquals(SlingLoggerLevel.DEBUG, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.DEBUG_INT));
        assertEquals(SlingLoggerLevel.DEBUG, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.DEBUG_INT+1));

        assertEquals(SlingLoggerLevel.DEBUG, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.INFO_INT-1));
        assertEquals(SlingLoggerLevel.INFO, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.INFO_INT));
        assertEquals(SlingLoggerLevel.INFO, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.INFO_INT+1));

        assertEquals(SlingLoggerLevel.INFO, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.WARN_INT-1));
        assertEquals(SlingLoggerLevel.WARN, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.WARN_INT));
        assertEquals(SlingLoggerLevel.WARN, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.WARN_INT+1));

        assertEquals(SlingLoggerLevel.WARN, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.ERROR_INT-1));
        assertEquals(SlingLoggerLevel.ERROR, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.ERROR_INT));
        assertEquals(SlingLoggerLevel.ERROR, SlingLoggerLevel.fromSlf4jLevel(LocationAwareLogger.ERROR_INT+1));
    }

    public void test_fromString() {
        assertNull(SlingLoggerLevel.fromString(null));

        assertNull(SlingLoggerLevel.fromString("traze"));
        assertNull(SlingLoggerLevel.fromString("-1"));
        assertNull(SlingLoggerLevel.fromString("7"));

        assertEquals(SlingLoggerLevel.TRACE, SlingLoggerLevel.fromString("trace"));
        assertEquals(SlingLoggerLevel.DEBUG, SlingLoggerLevel.fromString("debug"));
        assertEquals(SlingLoggerLevel.INFO, SlingLoggerLevel.fromString("info"));
        assertEquals(SlingLoggerLevel.WARN, SlingLoggerLevel.fromString("warn"));
        assertEquals(SlingLoggerLevel.ERROR, SlingLoggerLevel.fromString("error"));

        assertEquals(SlingLoggerLevel.TRACE, SlingLoggerLevel.fromString("TRACE"));
        assertEquals(SlingLoggerLevel.DEBUG, SlingLoggerLevel.fromString("DEBUG"));
        assertEquals(SlingLoggerLevel.INFO, SlingLoggerLevel.fromString("INFO"));
        assertEquals(SlingLoggerLevel.WARN, SlingLoggerLevel.fromString("WARN"));
        assertEquals(SlingLoggerLevel.ERROR, SlingLoggerLevel.fromString("ERROR"));

        assertEquals(SlingLoggerLevel.TRACE, SlingLoggerLevel.fromString("TrAcE"));
        assertEquals(SlingLoggerLevel.DEBUG, SlingLoggerLevel.fromString("dEbUg"));
        assertEquals(SlingLoggerLevel.INFO, SlingLoggerLevel.fromString("Info"));
        assertEquals(SlingLoggerLevel.WARN, SlingLoggerLevel.fromString("Warn"));
        assertEquals(SlingLoggerLevel.ERROR, SlingLoggerLevel.fromString("ErroR"));

        assertEquals(SlingLoggerLevel.TRACE, SlingLoggerLevel.fromString("5"));
        assertEquals(SlingLoggerLevel.DEBUG, SlingLoggerLevel.fromString("4"));
        assertEquals(SlingLoggerLevel.INFO, SlingLoggerLevel.fromString("3"));
        assertEquals(SlingLoggerLevel.WARN, SlingLoggerLevel.fromString("2"));
        assertEquals(SlingLoggerLevel.ERROR, SlingLoggerLevel.fromString("1"));
        assertEquals(SlingLoggerLevel.ERROR, SlingLoggerLevel.fromString("0"));
    }
}