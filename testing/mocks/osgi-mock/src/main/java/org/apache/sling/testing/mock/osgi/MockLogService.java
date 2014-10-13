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
package org.apache.sling.testing.mock.osgi;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock {@link LogService} implementation.
 */
class MockLogService implements LogService {

    private final Logger log;

    public MockLogService(final Class<?> loggerContext) {
        this.log = LoggerFactory.getLogger(loggerContext);
    }

    @Override
    public void log(final int level, final String message) {
        switch (level) {
        case LogService.LOG_ERROR:
            this.log.error(message);
            break;
        case LogService.LOG_WARNING:
            this.log.warn(message);
            break;
        case LogService.LOG_INFO:
            this.log.info(message);
            break;
        case LogService.LOG_DEBUG:
            this.log.debug(message);
            break;
        default:
            throw new IllegalArgumentException("Invalid log level: " + level);
        }
    }

    @Override
    public void log(final int level, final String message, final Throwable exception) {
        switch (level) {
        case LogService.LOG_ERROR:
            this.log.error(message, exception);
            break;
        case LogService.LOG_WARNING:
            this.log.warn(message, exception);
            break;
        case LogService.LOG_INFO:
            this.log.info(message, exception);
            break;
        case LogService.LOG_DEBUG:
            this.log.debug(message, exception);
            break;
        default:
            throw new IllegalArgumentException("Invalid log level: " + level);
        }
    }

    @Override
    public void log(final ServiceReference sr, final int level, final String message) {
        log(level, message);
    }

    @Override
    public void log(final ServiceReference sr, final int level, final String message, final Throwable exception) {
        log(level, message, exception);
    }

}
