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
package org.apache.sling.scripting.core.impl;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.sling.scripting.core.impl.LogWriter;
import org.slf4j.Logger;
import org.slf4j.Marker;

public class LogWriterTest extends TestCase {

    public void testCharacter() {
        MockLogger logger = new MockLogger();
        LogWriter logWriter = new LogWriter(logger);

        // ensure logger is empty
        logger.getLastMessage();

        // empty flush
        logWriter.flush();
        String msg = logger.getLastMessage();
        assertNull(msg);

        // write a single character, message only after flush
        logWriter.write('a');
        assertNull(logger.getLastMessage());
        logWriter.flush();
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals("a", msg);

        // write a single CR, no message
        logWriter.write('\r');
        assertNull(logger.getLastMessage());

        // write a single LF, no message
        logWriter.write('\n');
        assertNull(logger.getLastMessage());

        // write three characters (one is CR)
        logWriter.write('a');
        logWriter.write('\r');
        logWriter.write('b');

        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals("a", msg);

        logWriter.flush();

        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals("b", msg);

        // write three characters (one is LF)
        logWriter.write('a');
        logWriter.write('\n');
        logWriter.write('b');

        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals("a", msg);

        logWriter.flush();

        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals("b", msg);
    }

    public void testStringCR() throws IOException {
        MockLogger logger = new MockLogger();
        LogWriter logWriter = new LogWriter(logger);
        
        // ensure logger is empty
        logger.getLastMessage();
        
        // empty flush
        logWriter.flush();
        String msg = logger.getLastMessage();
        assertNull(msg);
        
        // intermediate CR
        String tMsg1 = "Ein";
        String tMsg2 = "Text";
        String tMsg = tMsg1 + "\r" + tMsg2;
        logWriter.write(tMsg);
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg1, msg);
        
        logWriter.flush();
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg2, msg);
        
        // initial CR
        tMsg = "\r" + tMsg1 + tMsg2;
        logWriter.write(tMsg);
        msg = logger.getLastMessage();
        assertNull(msg);
        
        logWriter.flush();
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg1 + tMsg2, msg);
        
        // terminating CR
        tMsg = tMsg1 + tMsg2 + "\r";
        logWriter.write(tMsg);
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg1+tMsg2, msg);
        
        logWriter.flush();
        msg = logger.getLastMessage();
        assertNull(msg);
    }
    
    public void testStringLF() throws IOException {
        MockLogger logger = new MockLogger();
        LogWriter logWriter = new LogWriter(logger);

        // ensure logger is empty
        logger.getLastMessage();

        // empty flush
        logWriter.flush();
        String msg = logger.getLastMessage();
        assertNull(msg);

        // intermediate LF
        String tMsg1 = "Ein";
        String tMsg2 = "Text";
        String tMsg = tMsg1 + "\n" + tMsg2;
        logWriter.write(tMsg);
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg1, msg);

        logWriter.flush();
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg2, msg);

        // initial LF
        tMsg = "\n" + tMsg1 + tMsg2;
        logWriter.write(tMsg);
        msg = logger.getLastMessage();
        assertNull(msg);

        logWriter.flush();
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg1 + tMsg2, msg);

        // terminating LF
        tMsg = tMsg1 + tMsg2 + "\n";
        logWriter.write(tMsg);
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg1+tMsg2, msg);

        logWriter.flush();
        msg = logger.getLastMessage();
        assertNull(msg);
    }

    public void testString() throws IOException {
        MockLogger logger = new MockLogger();
        LogWriter logWriter = new LogWriter(logger);

        // ensure logger is empty
        logger.getLastMessage();

        // empty flush
        logWriter.flush();
        String msg = logger.getLastMessage();
        assertNull(msg);

        // flushed line
        String tMsg = "Ein Text";
        logWriter.write(tMsg);
        msg = logger.getLastMessage();
        assertNull(msg);

        logWriter.flush();
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg, msg);

        // CR terminated line
        logWriter.write(tMsg + "\r");
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg, msg);

        // LF terminated line
        logWriter.write(tMsg + "\n");
        msg = logger.getLastMessage();
        assertNotNull(msg);
        assertEquals(tMsg, msg);
    }

    private static class MockLogger implements Logger {

        private String lastMessage;

        String getLastMessage() {
            String msg = lastMessage;
            lastMessage = null;
            return msg;
        }

        public void debug(String msg) {
            fail("Unexpected call");
        }

        public void debug(String format, Object arg) {
            fail("Unexpected call");
        }

        public void debug(String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void debug(String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void debug(Marker marker, String msg) {
            fail("Unexpected call");
        }

        public void debug(String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public void debug(Marker marker, String format, Object arg) {
            fail("Unexpected call");
        }

        public void debug(Marker marker, String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void debug(Marker marker, String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void debug(Marker marker, String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public void error(String msg) {
            lastMessage = msg;
        }

        public void error(String format, Object arg) {
            fail("Unexpected call");
        }

        public void error(String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void error(String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void error(Marker marker, String msg) {
            fail("Unexpected call");
        }

        public void error(String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public void error(Marker marker, String format, Object arg) {
            fail("Unexpected call");
        }

        public void error(Marker marker, String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void error(Marker marker, String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void error(Marker marker, String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public String getName() {
            return "mock";
        }

        public void info(String msg) {
            fail("Unexpected call");
        }

        public void info(String format, Object arg) {
            fail("Unexpected call");
        }

        public void info(String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void info(String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void info(Marker marker, String msg) {
            fail("Unexpected call");
        }

        public void info(String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public void info(Marker marker, String format, Object arg) {
            fail("Unexpected call");
        }

        public void info(Marker marker, String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void info(Marker marker, String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void info(Marker marker, String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public boolean isDebugEnabled() {
            return false;
        }

        public boolean isDebugEnabled(Marker marker) {
            return false;
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public boolean isErrorEnabled(Marker marker) {
            return true;
        }

        public boolean isInfoEnabled() {
            return false;
        }

        public boolean isInfoEnabled(Marker marker) {
            return false;
        }

        public boolean isTraceEnabled() {
            return false;
        }

        public boolean isTraceEnabled(Marker marker) {
            return false;
        }

        public boolean isWarnEnabled() {
            return false;
        }

        public boolean isWarnEnabled(Marker marker) {
            return false;
        }

        public void trace(String msg) {
            fail("Unexpected call");
        }

        public void trace(String format, Object arg) {
            fail("Unexpected call");
        }

        public void trace(String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void trace(String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void trace(Marker marker, String msg) {
            fail("Unexpected call");
        }

        public void trace(String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public void trace(Marker marker, String format, Object arg) {
            fail("Unexpected call");
        }

        public void trace(Marker marker, String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void trace(Marker marker, String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void trace(Marker marker, String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public void warn(String msg) {
            fail("Unexpected call");
        }

        public void warn(String format, Object arg) {
            fail("Unexpected call");
        }

        public void warn(String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void warn(String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void warn(Marker marker, String msg) {
            fail("Unexpected call");
        }

        public void warn(String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }

        public void warn(Marker marker, String format, Object arg) {
            fail("Unexpected call");
        }

        public void warn(Marker marker, String format, Object[] argArray) {
            fail("Unexpected call");
        }

        public void warn(Marker marker, String msg, Throwable t) {
            fail("Unexpected call");
        }

        public void warn(Marker marker, String format, Object arg1, Object arg2) {
            fail("Unexpected call");
        }
    }
}
