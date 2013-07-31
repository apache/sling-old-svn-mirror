/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.api;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/** Wraps an slf4 Logger to save its messages, 
 *  to make the log of {@link HealthCheck} executions 
 *  available as part of the {@link Result}.
 **/
public class ResultLog implements Logger {
    
    private final Logger wrappedLogger;
    private Level maxLevel = Level.DEBUG;
    private final List<Entry> entries;
    
    /** Log messages at or above this level change the {@link Result's status} */
    public static final Level MIN_LEVEL_TO_REPORT = Level.WARN;
    
    /** The log level of our entries */
    public enum Level {
        OK,
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
    
    /** An entry in our mini-log */
    public static class Entry {
        private final Level level;
        private final String message;
    
        Entry(Level level, String message) {
            this.level = level;
            this.message = message;
        }
        
        public String toString() {
            return level + ": " + message;
        }
        
        public Level getLevel() {
            return level;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public ResultLog(Logger wrappedLogger) {
        this.wrappedLogger = wrappedLogger;
        entries = new LinkedList<Entry>();
    }
    
    public Level getMaxLevel() {
        return maxLevel;
    }
    
    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
    
    private void storeMessage(ResultLog.Level level, String message) {
        maxLevel = level.ordinal() > maxLevel.ordinal() ? level : maxLevel;
        entries.add(new ResultLog.Entry(level, message));
    }
    
    @Override
    public String getName() {
        return getClass().getName();
    }
    
    @Override
    public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
        storeMessage(Level.DEBUG, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2, arg3);
    }

    @Override
    public void debug(Marker arg0, String arg1, Object arg2) {
        storeMessage(Level.DEBUG, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2);
    }

    @Override
    public void debug(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(Level.DEBUG, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2);
    }

    @Override
    public void debug(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(Level.DEBUG, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2);
    }

    @Override
    public void debug(Marker arg0, String arg1) {
        storeMessage(Level.DEBUG, arg1);
        wrappedLogger.debug(arg0, arg1);
    }

    @Override
    public void debug(String arg0, Object arg1, Object arg2) {
        storeMessage(Level.DEBUG, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2);
    }

    @Override
    public void debug(String arg0, Object arg1) {
        storeMessage(Level.DEBUG, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.debug(arg0, arg1);
    }

    @Override
    public void debug(String arg0, Object[] arg1) {
        storeMessage(Level.DEBUG, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.debug(arg0, arg1);
    }

    @Override
    public void debug(String arg0, Throwable arg1) {
        storeMessage(Level.DEBUG, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.debug(arg0, arg1);
    }

    @Override
    public void debug(String arg0) {
        storeMessage(Level.DEBUG, arg0);
        wrappedLogger.debug(arg0);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isDebugEnabled(Marker arg0) {
        return true;
    }
    
    @Override
    public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
        storeMessage(Level.INFO, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.info(arg0, arg1, arg2, arg3);
    }

    @Override
    public void info(Marker arg0, String arg1, Object arg2) {
        storeMessage(Level.INFO, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.info(arg0, arg1, arg2);
    }

    @Override
    public void info(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(Level.INFO, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.info(arg0, arg1, arg2);
    }

    @Override
    public void info(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(Level.INFO, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.info(arg0, arg1, arg2);
    }

    @Override
    public void info(Marker arg0, String arg1) {
        storeMessage(Level.INFO, arg1);
        wrappedLogger.info(arg0, arg1);
    }

    @Override
    public void info(String arg0, Object arg1, Object arg2) {
        storeMessage(Level.INFO, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.info(arg0, arg1, arg2);
    }

    @Override
    public void info(String arg0, Object arg1) {
        storeMessage(Level.INFO, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.info(arg0, arg1);
    }

    @Override
    public void info(String arg0, Object[] arg1) {
        storeMessage(Level.INFO, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.info(arg0, arg1);
    }

    @Override
    public void info(String arg0, Throwable arg1) {
        storeMessage(Level.INFO, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.info(arg0, arg1);
    }

    @Override
    public void info(String arg0) {
        storeMessage(Level.INFO, arg0);
        wrappedLogger.info(arg0);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled(Marker arg0) {
        return true;
    }
    
    @Override
    public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
        storeMessage(Level.WARN, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2, arg3);
    }

    @Override
    public void warn(Marker arg0, String arg1, Object arg2) {
        storeMessage(Level.WARN, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2);
    }

    @Override
    public void warn(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(Level.WARN, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2);
    }

    @Override
    public void warn(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(Level.WARN, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2);
    }

    @Override
    public void warn(Marker arg0, String arg1) {
        storeMessage(Level.WARN, arg1);
        wrappedLogger.warn(arg0, arg1);
    }

    @Override
    public void warn(String arg0, Object arg1, Object arg2) {
        storeMessage(Level.WARN, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2);
    }

    @Override
    public void warn(String arg0, Object arg1) {
        storeMessage(Level.WARN, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.warn(arg0, arg1);
    }

    @Override
    public void warn(String arg0, Object[] arg1) {
        storeMessage(Level.WARN, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.warn(arg0, arg1);
    }

    @Override
    public void warn(String arg0, Throwable arg1) {
        storeMessage(Level.WARN, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.warn(arg0, arg1);
    }

    @Override
    public void warn(String arg0) {
        storeMessage(Level.WARN, arg0);
        wrappedLogger.warn(arg0);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled(Marker arg0) {
        return true;
    }
    
    @Override
    public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
        storeMessage(Level.ERROR, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.error(arg0, arg1, arg2, arg3);
    }

    @Override
    public void error(Marker arg0, String arg1, Object arg2) {
        storeMessage(Level.ERROR, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.error(arg0, arg1, arg2);
    }

    @Override
    public void error(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(Level.ERROR, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.error(arg0, arg1, arg2);
    }

    @Override
    public void error(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(Level.ERROR, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.error(arg0, arg1, arg2);
    }

    @Override
    public void error(Marker arg0, String arg1) {
        storeMessage(Level.ERROR, arg1);
        wrappedLogger.error(arg0, arg1);
    }

    @Override
    public void error(String arg0, Object arg1, Object arg2) {
        storeMessage(Level.ERROR, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.error(arg0, arg1, arg2);
    }

    @Override
    public void error(String arg0, Object arg1) {
        storeMessage(Level.ERROR, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.error(arg0, arg1);
    }

    @Override
    public void error(String arg0, Object[] arg1) {
        storeMessage(Level.ERROR, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.error(arg0, arg1);
    }

    @Override
    public void error(String arg0, Throwable arg1) {
        storeMessage(Level.ERROR, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.error(arg0, arg1);
    }

    @Override
    public void error(String arg0) {
        storeMessage(Level.ERROR, arg0);
        wrappedLogger.error(arg0);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled(Marker arg0) {
        return true;
    }
    
    @Override
    public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
        storeMessage(Level.TRACE, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2, arg3);
    }

    @Override
    public void trace(Marker arg0, String arg1, Object arg2) {
        storeMessage(Level.TRACE, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2);
    }

    @Override
    public void trace(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(Level.TRACE, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2);
    }

    @Override
    public void trace(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(Level.TRACE, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2);
    }

    @Override
    public void trace(Marker arg0, String arg1) {
        storeMessage(Level.TRACE, arg1);
        wrappedLogger.trace(arg0, arg1);
    }

    @Override
    public void trace(String arg0, Object arg1, Object arg2) {
        storeMessage(Level.TRACE, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2);
    }

    @Override
    public void trace(String arg0, Object arg1) {
        storeMessage(Level.TRACE, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.trace(arg0, arg1);
    }

    @Override
    public void trace(String arg0, Object[] arg1) {
        storeMessage(Level.TRACE, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.trace(arg0, arg1);
    }

    @Override
    public void trace(String arg0, Throwable arg1) {
        storeMessage(Level.TRACE, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.trace(arg0, arg1);
    }

    @Override
    public void trace(String arg0) {
        storeMessage(Level.TRACE, arg0);
        wrappedLogger.trace(arg0);
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public boolean isTraceEnabled(Marker arg0) {
        return true;
    }
}
