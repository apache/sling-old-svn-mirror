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
package org.apache.sling.hc.impl;

import static org.apache.sling.hc.api.EvaluationResult.LogLevel.DEBUG;
import static org.apache.sling.hc.api.EvaluationResult.LogLevel.ERROR;
import static org.apache.sling.hc.api.EvaluationResult.LogLevel.INFO;
import static org.apache.sling.hc.api.EvaluationResult.LogLevel.TRACE;
import static org.apache.sling.hc.api.EvaluationResult.LogLevel.WARN;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.api.RuleLogger;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/** {@link RuleLogger} used to evaluate our rules - wraps 
 *  and slf4j Logger to capture log output and keep
 *  track of the highest log level used.
 */
public class RuleLoggerImpl implements RuleLogger {

    private final Logger wrappedLogger;
    private final List<EvaluationResult.LogMessage> messages = new LinkedList<EvaluationResult.LogMessage>();
    private EvaluationResult.LogLevel maxLevel = EvaluationResult.LogLevel.DEBUG;
    
    public RuleLoggerImpl(Logger wrapped) {
        this.wrappedLogger = wrapped;
    }
    
    private void storeMessage(EvaluationResult.LogLevel level, String message) {
        maxLevel = level.ordinal() > maxLevel.ordinal() ? level : maxLevel;
        messages.add(new EvaluationResult.LogMessage(level, message));
    }
    
    public List<EvaluationResult.LogMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }
    
    public EvaluationResult.LogLevel getMaxLevel() {
        return maxLevel;
    }
    
    @Override
    public String getName() {
        return getClass().getName();
    }
    
    @Override
    public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
        storeMessage(DEBUG, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2, arg3);
    }

    @Override
    public void debug(Marker arg0, String arg1, Object arg2) {
        storeMessage(DEBUG, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2);
    }

    @Override
    public void debug(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(DEBUG, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2);
    }

    @Override
    public void debug(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(DEBUG, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2);
    }

    @Override
    public void debug(Marker arg0, String arg1) {
        storeMessage(DEBUG, arg1);
        wrappedLogger.debug(arg0, arg1);
    }

    @Override
    public void debug(String arg0, Object arg1, Object arg2) {
        storeMessage(DEBUG, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.debug(arg0, arg1, arg2);
    }

    @Override
    public void debug(String arg0, Object arg1) {
        storeMessage(DEBUG, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.debug(arg0, arg1);
    }

    @Override
    public void debug(String arg0, Object[] arg1) {
        storeMessage(DEBUG, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.debug(arg0, arg1);
    }

    @Override
    public void debug(String arg0, Throwable arg1) {
        storeMessage(DEBUG, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.debug(arg0, arg1);
    }

    @Override
    public void debug(String arg0) {
        storeMessage(DEBUG, arg0);
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
        storeMessage(INFO, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.info(arg0, arg1, arg2, arg3);
    }

    @Override
    public void info(Marker arg0, String arg1, Object arg2) {
        storeMessage(INFO, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.info(arg0, arg1, arg2);
    }

    @Override
    public void info(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(INFO, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.info(arg0, arg1, arg2);
    }

    @Override
    public void info(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(INFO, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.info(arg0, arg1, arg2);
    }

    @Override
    public void info(Marker arg0, String arg1) {
        storeMessage(INFO, arg1);
        wrappedLogger.info(arg0, arg1);
    }

    @Override
    public void info(String arg0, Object arg1, Object arg2) {
        storeMessage(INFO, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.info(arg0, arg1, arg2);
    }

    @Override
    public void info(String arg0, Object arg1) {
        storeMessage(INFO, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.info(arg0, arg1);
    }

    @Override
    public void info(String arg0, Object[] arg1) {
        storeMessage(INFO, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.info(arg0, arg1);
    }

    @Override
    public void info(String arg0, Throwable arg1) {
        storeMessage(INFO, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.info(arg0, arg1);
    }

    @Override
    public void info(String arg0) {
        storeMessage(INFO, arg0);
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
        storeMessage(WARN, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2, arg3);
    }

    @Override
    public void warn(Marker arg0, String arg1, Object arg2) {
        storeMessage(WARN, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2);
    }

    @Override
    public void warn(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(WARN, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2);
    }

    @Override
    public void warn(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(WARN, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2);
    }

    @Override
    public void warn(Marker arg0, String arg1) {
        storeMessage(WARN, arg1);
        wrappedLogger.warn(arg0, arg1);
    }

    @Override
    public void warn(String arg0, Object arg1, Object arg2) {
        storeMessage(WARN, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.warn(arg0, arg1, arg2);
    }

    @Override
    public void warn(String arg0, Object arg1) {
        storeMessage(WARN, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.warn(arg0, arg1);
    }

    @Override
    public void warn(String arg0, Object[] arg1) {
        storeMessage(WARN, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.warn(arg0, arg1);
    }

    @Override
    public void warn(String arg0, Throwable arg1) {
        storeMessage(WARN, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.warn(arg0, arg1);
    }

    @Override
    public void warn(String arg0) {
        storeMessage(WARN, arg0);
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
        storeMessage(ERROR, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.error(arg0, arg1, arg2, arg3);
    }

    @Override
    public void error(Marker arg0, String arg1, Object arg2) {
        storeMessage(ERROR, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.error(arg0, arg1, arg2);
    }

    @Override
    public void error(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(ERROR, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.error(arg0, arg1, arg2);
    }

    @Override
    public void error(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(ERROR, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.error(arg0, arg1, arg2);
    }

    @Override
    public void error(Marker arg0, String arg1) {
        storeMessage(ERROR, arg1);
        wrappedLogger.error(arg0, arg1);
    }

    @Override
    public void error(String arg0, Object arg1, Object arg2) {
        storeMessage(ERROR, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.error(arg0, arg1, arg2);
    }

    @Override
    public void error(String arg0, Object arg1) {
        storeMessage(ERROR, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.error(arg0, arg1);
    }

    @Override
    public void error(String arg0, Object[] arg1) {
        storeMessage(ERROR, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.error(arg0, arg1);
    }

    @Override
    public void error(String arg0, Throwable arg1) {
        storeMessage(ERROR, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.error(arg0, arg1);
    }

    @Override
    public void error(String arg0) {
        storeMessage(ERROR, arg0);
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
        storeMessage(TRACE, MessageFormatter.format(arg1, arg2, arg3).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2, arg3);
    }

    @Override
    public void trace(Marker arg0, String arg1, Object arg2) {
        storeMessage(TRACE, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2);
    }

    @Override
    public void trace(Marker arg0, String arg1, Object[] arg2) {
        storeMessage(TRACE, MessageFormatter.arrayFormat(arg1, arg2).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2);
    }

    @Override
    public void trace(Marker arg0, String arg1, Throwable arg2) {
        storeMessage(TRACE, MessageFormatter.format(arg1, arg2).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2);
    }

    @Override
    public void trace(Marker arg0, String arg1) {
        storeMessage(TRACE, arg1);
        wrappedLogger.trace(arg0, arg1);
    }

    @Override
    public void trace(String arg0, Object arg1, Object arg2) {
        storeMessage(TRACE, MessageFormatter.format(arg0, arg1, arg2).getMessage());
        wrappedLogger.trace(arg0, arg1, arg2);
    }

    @Override
    public void trace(String arg0, Object arg1) {
        storeMessage(TRACE, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.trace(arg0, arg1);
    }

    @Override
    public void trace(String arg0, Object[] arg1) {
        storeMessage(TRACE, MessageFormatter.arrayFormat(arg0, arg1).getMessage());
        wrappedLogger.trace(arg0, arg1);
    }

    @Override
    public void trace(String arg0, Throwable arg1) {
        storeMessage(TRACE, MessageFormatter.format(arg0, arg1).getMessage());
        wrappedLogger.trace(arg0, arg1);
    }

    @Override
    public void trace(String arg0) {
        storeMessage(TRACE, arg0);
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
