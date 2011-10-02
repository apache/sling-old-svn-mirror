/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.logservice.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

/**
 * The <code>LogServiceFactory</code> implements the OSGi Log Service
 * specification and provides the functionality for the logging system. This
 * service should be one of the first services loaded in the system.
 */
public class LogServiceFactory implements ServiceFactory {

    private LogSupport logSupport;

    /**
     * Initializes the logging system with settings from some startup properties
     * before the real configuration is read after ContentBus bootstrap.
     * 
     * @param properties The startup properties to initialize the logging system
     *            with.
     */
    LogServiceFactory(LogSupport logSupport) {
        this.logSupport = logSupport;

    }

    // ---------- ServiceFactory

    public Object getService(Bundle bundle, ServiceRegistration registration) {
        return new LogServiceImpl(bundle);
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration,
            Object service) {
        // nothing to do currently
    }

    private class LogServiceImpl implements LogService {

        private Bundle bundle;

        /**
         * Initializes the logging system with settings from some startup
         * properties before the real configuration is read after ContentBus
         * bootstrap.
         * 
         * @param properties The startup properties to initialize the logging
         *            system with.
         */
        /* package */LogServiceImpl(Bundle bundle) {
            this.bundle = bundle;
        }

        // ---------- LogService

        public void log(int level, String message) {
            this.log(null, level, message, null);
        }

        public void log(int level, String message, Throwable exception) {
            this.log(null, level, message, exception);
        }

        public void log(ServiceReference sr, int level, String message) {
            this.log(sr, level, message, null);
        }

        public void log(ServiceReference sr, int level, String message,
                Throwable exception) {
            // simply fire a log event
            LogEntry entry = new LogEntryImpl(this.bundle, sr, level, message,
                exception);
            LogServiceFactory.this.logSupport.fireLogEvent(entry);
        }
    }

}
