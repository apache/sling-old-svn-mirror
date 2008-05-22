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

import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;

import junit.framework.TestCase;

public class SlingLoggerTest extends TestCase {

    private static final String LOGGER_NAME = "test.log";

    private SlingLogWriter output = new SlingLogWriter() {
        public void writeln() throws IOException {
            // just flush, no end of line
            flush();
        }
    };

    private MessageFormat messageOnly = new MessageFormat("{5}");

    private SlingLogger logger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        logger = new SlingLogger(LOGGER_NAME);
        logger.configure(SlingLoggerLevel.DEBUG, output, messageOnly);
    }

    @Override
    protected void tearDown() throws Exception {
        output.close();
        super.tearDown();
    }

    public void testSetLogLevel() {

        // prevent real output, set output delegatee to null
        try {
            output.getDelegatee().close();
        } catch (IOException ignore) {
        }
        output.setDelegatee(null);

        // initial assertion
        logger.setLogLevel(SlingLoggerLevel.DEBUG);
        assertEquals(SlingLoggerLevel.DEBUG, logger.getLogLevel());

        // valid as is
        logger.setLogLevel("INFO");
        assertEquals(SlingLoggerLevel.INFO, logger.getLogLevel());

        // valid internal conversion to upper case
        logger.setLogLevel("warn");
        assertEquals(SlingLoggerLevel.WARN, logger.getLogLevel());
        logger.setLogLevel("ErrOr");
        assertEquals(SlingLoggerLevel.ERROR, logger.getLogLevel());

        // reset log level to debug
        logger.setLogLevel(SlingLoggerLevel.DEBUG);
        assertEquals(SlingLoggerLevel.DEBUG, logger.getLogLevel());

        // invalid, last level is still set
        logger.setLogLevel((String) null);
        assertEquals(SlingLoggerLevel.DEBUG, logger.getLogLevel());

        // invalid, last level is still set
        logger.setLogLevel("");
        assertEquals(SlingLoggerLevel.DEBUG, logger.getLogLevel());

        // invalid, last level is still set
        logger.setLogLevel("gurk");
        assertEquals(SlingLoggerLevel.DEBUG, logger.getLogLevel());
    }

    public void testCheckLogLevelTrace() {
        // initial assertion
        logger.setLogLevel(SlingLoggerLevel.TRACE);
        assertEquals(SlingLoggerLevel.TRACE, logger.getLogLevel());

        // ensure logging disabled
        // none for trace

        // ensure logging enabled
        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
    }

    public void testCheckLogLevelDebug() {
        // initial assertion
        logger.setLogLevel(SlingLoggerLevel.DEBUG);
        assertEquals(SlingLoggerLevel.DEBUG, logger.getLogLevel());

        // ensure logging disabled
        assertFalse(logger.isTraceEnabled());

        // ensure logging enabled
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
    }

    public void testCheckLogLevelInfo() {
        // initial assertion
        logger.setLogLevel(SlingLoggerLevel.INFO);
        assertEquals(SlingLoggerLevel.INFO, logger.getLogLevel());

        // ensure logging disabled
        assertFalse(logger.isTraceEnabled());
        assertFalse(logger.isDebugEnabled());

        // ensure logging enabled
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
    }

    public void testCheckLogLevelWarn() {
        // initial assertion
        logger.setLogLevel(SlingLoggerLevel.WARN);
        assertEquals(SlingLoggerLevel.WARN, logger.getLogLevel());

        // ensure logging disabled
        assertFalse(logger.isTraceEnabled());
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());

        // ensure logging enabled
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
    }

    public void testCheckLogLevelError() {
        // initial assertion
        logger.setLogLevel(SlingLoggerLevel.ERROR);
        assertEquals(SlingLoggerLevel.ERROR, logger.getLogLevel());

        // ensure logging disabled
        assertFalse(logger.isTraceEnabled());
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());
        assertFalse(logger.isWarnEnabled());

        // ensure logging enabled
        assertTrue(logger.isErrorEnabled());
    }

    public void testFormat() {
        StringWriter w = new StringWriter();
        output.setDelegatee(w);

        // a single message
        logger.configure(SlingLoggerLevel.DEBUG, output, messageOnly);

        String message = "This is a message";
        logger.warn(message);
        assertEquals(message, w.toString());

        // reset output buffer and format with logger name and message
        w.getBuffer().delete(0, w.getBuffer().length());
        logger.configure(SlingLoggerLevel.DEBUG, output, new MessageFormat(
            "{3}|{5}"));

        logger.warn(message);
        assertEquals(logger.getName() + "|" + message, w.toString());

        // reset output buffer and format with logger name, level, thread and
        // message
        w.getBuffer().delete(0, w.getBuffer().length());
        logger.configure(SlingLoggerLevel.DEBUG, output, new MessageFormat(
            "{2}|{3}|{4}|{5}"));
        logger.warn(message);
        assertEquals(Thread.currentThread().getName() + "|" + logger.getName()
            + "|" + SlingLoggerLevel.WARN + "|" + message, w.toString());
    }
}
