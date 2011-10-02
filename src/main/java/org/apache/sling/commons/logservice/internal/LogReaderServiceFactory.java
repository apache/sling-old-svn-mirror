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

import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * The <code>LogReaderServiceFactory</code> is the service factory for
 * <code>LogReader</code> service instances supplied to bundles.
 * <p>
 * <blockquote> When a bundle which registers a LogListener object is stopped or
 * otherwise releases the Log Reader Service, the Log Reader Service must remove
 * all of the bundle's listeners.</blockquote>
 */
public class LogReaderServiceFactory implements ServiceFactory {

    private LogSupport logSupport;

    LogReaderServiceFactory(LogSupport logSupport) {
        this.logSupport = logSupport;
    }

    // ---------- ServiceFactory interface ------------------------------------

    public Object getService(Bundle bundle, ServiceRegistration registration) {
        return new LogReaderServiceImpl(bundle);
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration,
            Object service) {
        ((LogReaderServiceImpl) service).shutdown();
    }

    // --------- internal LogReaderService implementation ----------------------

    private class LogReaderServiceImpl implements LogReaderService {

        private Bundle bundle;

        /* package */LogReaderServiceImpl(Bundle bundle) {
            this.bundle = bundle;
        }

        /* package */void shutdown() {
            LogReaderServiceFactory.this.logSupport.removeLogListeners(this.bundle);
        }

        public void addLogListener(LogListener listener) {
            LogReaderServiceFactory.this.logSupport.addLogListener(this.bundle,
                listener);
        }

        public void removeLogListener(LogListener listener) {
            LogReaderServiceFactory.this.logSupport.removeLogListener(listener);
        }

        @SuppressWarnings("rawtypes")
        public Enumeration getLog() {
            return LogReaderServiceFactory.this.logSupport.getLog();
        }
    }

}
