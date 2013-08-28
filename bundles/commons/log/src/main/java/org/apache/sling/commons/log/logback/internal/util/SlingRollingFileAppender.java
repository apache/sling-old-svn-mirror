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

package org.apache.sling.commons.log.logback.internal.util;

import ch.qos.logback.core.rolling.RollingFileAppender;

import org.apache.sling.commons.log.logback.internal.LogWriter;

/**
 * Custom class to allow the SlingLogPanel to differentiate between default
 * appenders and Sling Config based appenders
 * 
 * @param <E>
 */
public class SlingRollingFileAppender<E> extends RollingFileAppender<E> {
    private LogWriter logWriter;

    public LogWriter getLogWriter() {
        return logWriter;
    }

    public void setLogWriter(LogWriter logWriter) {
        this.logWriter = logWriter;
    }
}
