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
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashSet;

import junit.framework.TestCase;

public class SlingLoggerTest extends TestCase {

    private SlingLoggerWriter output = new SlingLoggerWriter(null) {
        {
            try {
                configure(null, -1, "1GB");
            } catch (IOException ioe) {

            }
        }

        public void writeln() throws IOException {
            // just flush, no end of line
            flush();
        }
    };

    private String messageOnly = "{5}";

    private SlingLoggerConfig config;

    private SlingLogger logger;

    private Method delegateeSetter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        config = new SlingLoggerConfig(getClass().getName(), "",
            new HashSet<String>(), SlingLoggerLevel.DEBUG, output);

        logger = new SlingLogger("sample");
        logger.setLoggerConfig(config);
    }

    @Override
    protected void tearDown() throws Exception {
        output.close();
        super.tearDown();
    }

    public void testCheckLogLevelTrace() {
        // initial assertion
        config.setLogLevel(SlingLoggerLevel.TRACE);
        assertEquals(SlingLoggerLevel.TRACE, config.getLogLevel());

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
        config.setLogLevel(SlingLoggerLevel.DEBUG);
        assertEquals(SlingLoggerLevel.DEBUG, config.getLogLevel());

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
        config.setLogLevel(SlingLoggerLevel.INFO);
        assertEquals(SlingLoggerLevel.INFO, config.getLogLevel());

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
        config.setLogLevel(SlingLoggerLevel.WARN);
        assertEquals(SlingLoggerLevel.WARN, config.getLogLevel());

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
        config.setLogLevel(SlingLoggerLevel.ERROR);
        assertEquals(SlingLoggerLevel.ERROR, config.getLogLevel());

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
        setDelagateeOn(output, w);

        // a single message
        config.configure(messageOnly, new HashSet<String>(),
            SlingLoggerLevel.DEBUG, output);

        String message = "This is a message";
        logger.warn(message);
        assertEquals(message, w.toString());

        // reset output buffer and format with logger name and message
        w.getBuffer().delete(0, w.getBuffer().length());
        config.configure("{3}|{5}", new HashSet<String>(),
            SlingLoggerLevel.DEBUG, output);

        logger.warn(message);
        assertEquals(logger.getName() + "|" + message, w.toString());

        // reset output buffer and format with logger name, level, thread and
        // message
        w.getBuffer().delete(0, w.getBuffer().length());
        config.configure("{2}|{3}|{4}|{5}", new HashSet<String>(),
            SlingLoggerLevel.DEBUG, output);
        logger.warn(message);
        assertEquals(Thread.currentThread().getName() + "|" + logger.getName()
            + "|" + SlingLoggerLevel.WARN + "|" + message, w.toString());
    }

    private static void setDelagateeOn(SlingLoggerWriter output, Writer w) {
        try {
            Method delegateeSetter = SlingLoggerWriter.class.getDeclaredMethod(
                "setDelegatee", new Class[] { Writer.class });
            delegateeSetter.setAccessible(true);
            delegateeSetter.invoke(output, new Object[] { w });
        } catch (Throwable t) {
            fail("Cannot get or invoke SlingLoggerWriter.setDelagetee method: "
                + t.getMessage());
        }
    }
}
