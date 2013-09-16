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

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.apache.commons.io.FileUtils;
import org.apache.sling.commons.log.logback.internal.util.SlingRollingFileAppender;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestLogWriter {

    @Test
    public void specialHandlingForConsole() {
        LogWriter lw = new LogWriter(null,null, 5, null);
        assertTrue(createappender(lw) instanceof ConsoleAppender);

        lw = new LogWriter(LogWriter.FILE_NAME_CONSOLE,LogWriter.FILE_NAME_CONSOLE, 5, null);
        assertTrue(createappender(lw) instanceof ConsoleAppender);
    }

    @Test
    public void testSizeBasedLegacyPattern() {
        LogWriter lw = new LogWriter("foo","target/foo", 5, "4k");
        Appender<ILoggingEvent> a = createappender(lw);

        assertInstanceOf(a, SlingRollingFileAppender.class);
        SlingRollingFileAppender sr = (SlingRollingFileAppender) a;

        assertInstanceOf(sr.getTriggeringPolicy(), SizeBasedTriggeringPolicy.class);
        assertInstanceOf(sr.getRollingPolicy(), FixedWindowRollingPolicy.class);

        SizeBasedTriggeringPolicy sbtp = (SizeBasedTriggeringPolicy) sr.getTriggeringPolicy();
        FixedWindowRollingPolicy fwRp = (FixedWindowRollingPolicy) sr.getRollingPolicy();
        assertEquals(5, fwRp.getMaxIndex());
        assertEquals(String.valueOf(4 * FileUtils.ONE_KB), sbtp.getMaxFileSize());
    }

    @Test
    public void testRotationBasedLegacyPattern() {
        LogWriter lw = new LogWriter("foo","target/foo", 5, "'.'yyyy-MM");
        Appender<ILoggingEvent> a = createappender(lw);

        assertInstanceOf(a, SlingRollingFileAppender.class);
        SlingRollingFileAppender sr = (SlingRollingFileAppender) a;

        assertInstanceOf(sr.getTriggeringPolicy(), TimeBasedRollingPolicy.class);

        TimeBasedRollingPolicy tbrp = (TimeBasedRollingPolicy) sr.getTriggeringPolicy();
        assertEquals(5, tbrp.getMaxHistory());
        assertEquals("target/foo.%d{yyyy-MM}", tbrp.getFileNamePattern());
    }

    private static Appender<ILoggingEvent> createappender(LogWriter lw) {
        Encoder<ILoggingEvent> encoder = new PatternLayoutEncoder();
        return lw.createAppender((Context) LoggerFactory.getILoggerFactory(), encoder);
    }

    private static void assertInstanceOf(Object o, Class expected) {
        if (expected.isInstance(o)) {
            return;
        }
        fail(String.format("Object of type [%s] is not instanceof [%s]", o.getClass(), expected));
    }
}
