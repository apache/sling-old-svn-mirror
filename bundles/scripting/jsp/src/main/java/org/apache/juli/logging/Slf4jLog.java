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

public class Slf4jLog implements Log {

    private Logger delegatee;

    Slf4jLog(String name) {
        this.delegatee = LoggerFactory.getLogger(name);
    }

    public void debug(Object message, Throwable t) {
        delegatee.debug(message.toString(), t);
    }

    public void debug(Object message) {
        delegatee.debug(message.toString());
    }

    public void error(Object message, Throwable t) {
        delegatee.error(message.toString(), t);
    }

    public void error(Object message) {
        delegatee.error(message.toString());
    }

    public void fatal(Object message, Throwable t) {
        delegatee.error(message.toString(), t);
    }

    public void fatal(Object message) {
        delegatee.error(message.toString());
    }

    public void info(Object message, Throwable t) {
        delegatee.info(message.toString(), t);
    }

    public void info(Object message) {
        delegatee.info(message.toString());
    }

    public boolean isDebugEnabled() {
        return delegatee.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return delegatee.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return delegatee.isErrorEnabled();
    }

    public boolean isInfoEnabled() {
        return delegatee.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return delegatee.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return delegatee.isWarnEnabled();
    }

    public void trace(Object message, Throwable t) {
        delegatee.trace(message.toString(), t);
    }

    public void trace(Object message) {
        delegatee.trace(message.toString());
    }

    public void warn(Object message, Throwable t) {
        delegatee.warn(message.toString(), t);
    }

    public void warn(Object message) {
        delegatee.warn(message.toString());
    }
    
    
}
