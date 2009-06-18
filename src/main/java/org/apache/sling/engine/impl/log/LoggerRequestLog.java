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
package org.apache.sling.engine.impl.log;

import org.apache.sling.engine.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>LoggerRequestLog</code> class implements the
 * <code>RequestLog</code> interface writing log messages to an SLF4J logger.
 */
class LoggerRequestLog implements RequestLog {

    private Logger logger;

    LoggerRequestLog(String loggerName) {
        this.logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * @see org.apache.sling.engine.RequestLog#write(java.lang.String)
     */
    public void write(String message) {
        this.logger.info(message);
    }

    public void close() {
        // nothing to be done here
    }
}
