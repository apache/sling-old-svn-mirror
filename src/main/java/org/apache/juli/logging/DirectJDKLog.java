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
package org.apache.juli.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overriding the DirectJDKLog impl to delegate the logging to Slf4j
 */
@SuppressWarnings("UnusedDeclaration")
class DirectJDKLog implements Log {
    private final Logger logger;

    public DirectJDKLog(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    static Log getInstance(String name) {
        return new DirectJDKLog( name );
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public void trace(Object message) {
        logger.trace(String.valueOf(message));
    }

    public void trace(Object message, Throwable t) {
        logger.trace(String.valueOf(message), t);
    }

    public void debug(Object message) {
        logger.debug(String.valueOf(message));
    }

    public void debug(Object message, Throwable t) {
        logger.debug(String.valueOf(message), t);
    }

    public void info(Object message) {
        logger.info(String.valueOf(message));
    }

    public void info(Object message, Throwable t) {
        logger.info(String.valueOf(message), t);
    }

    public void warn(Object message) {
        logger.warn(String.valueOf(message));
    }

    public void warn(Object message, Throwable t) {
        logger.warn(String.valueOf(message), t);
    }

    public void error(Object message) {
        logger.error(String.valueOf(message));
    }

    public void error(Object message, Throwable t) {
        logger.error(String.valueOf(message), t);
    }

    public void fatal(Object message) {
        logger.error(String.valueOf(message));
    }

    public void fatal(Object message, Throwable t) {
        logger.error(String.valueOf(message), t);
    }
}
