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

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class TestLogConfig {

    @Test
    public void testLayout() {
        String pattern = "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5}";
        String convertedPattern = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %message%n";
        LogConfig logConfig = createConfig(pattern);
        assertEquals(convertedPattern, logConfig.createLayout().getPattern());
    }

    @Test
    public void testLayoutWithNewPattern() {
        String convertedPattern = "%d{dd.MM.yyyy HH:mm:ss.SSS} *%level* [%thread] %logger %message%n";
        LogConfig logConfig = createConfig(convertedPattern);
        // Test that valid LogBack pattern are not tampered
        assertEquals(convertedPattern, logConfig.createLayout().getPattern());
    }

    private LogConfig createConfig(String pattern) {
        return new LogConfig(new DummyLogWriterProvider(), pattern, Collections.<String> emptySet(), Level.DEBUG,
            "test", false, null, (LoggerContext) LoggerFactory.getILoggerFactory());
    }

    private static class DummyLogWriterProvider implements LogConfig.LogWriterProvider {

        @Override
        public LogWriter getLogWriter(String writerName) {
            return null;
        }
    }
}
