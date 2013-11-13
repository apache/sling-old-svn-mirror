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

package org.apache.sling.commons.log.logback.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.sling.commons.log.logback.internal.util.Util;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class AppenderTracker extends ServiceTracker implements LogbackResetListener {

    private static final String PROP_LOGGER = "loggers";

    private final LoggerContext loggerContext;

    private final Map<ServiceReference, AppenderInfo> appenders = new ConcurrentHashMap<ServiceReference, AppenderInfo>();

    private final Map<String,Set<String>> appenderNameToLoggerMap = new ConcurrentHashMap<String, Set<String>>();

    public AppenderTracker(final BundleContext context, final LoggerContext loggerContext) throws InvalidSyntaxException {
        super(context, createFilter(), null);
        this.loggerContext = loggerContext;
        super.open();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object addingService(final ServiceReference reference) {
        final Appender<ILoggingEvent> a = (Appender<ILoggingEvent>) super.addingService(reference);
        a.setContext(loggerContext);
        a.start();

        final AppenderInfo ai = new AppenderInfo(reference, a);
        appenders.put(reference, ai);
        attachAppender(ai);

        return ai;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void modifiedService(final ServiceReference reference, final Object service) {
        detachAppender(appenders.remove(reference));
        final AppenderInfo nai = new AppenderInfo(reference, (Appender<ILoggingEvent>) service);
        appenders.put(reference, nai);
        attachAppender(nai);
    }

    @Override
    public void removedService(final ServiceReference reference, final Object service) {
        final AppenderInfo ai = appenders.remove(reference);
        this.detachAppender(ai);
        if ( ai != null ) {
            ai.appender.stop();
            // context can't be unset
        }

        super.removedService(reference, service);
    }

    public Collection<AppenderInfo> getAppenderInfos() {
        return appenders.values();
    }

    private void detachAppender(final AppenderInfo ai) {
        if (ai != null) {
            for (final String name : ai.getLoggers()) {
                final Logger logger = loggerContext.getLogger(name);

                logger.detachAppender(ai.appender);
            }
        }
    }

    private void attachAppender(final AppenderInfo ai) {
        if (ai != null) {
            for (final String name : ai.getLoggers()) {
                final Logger logger = loggerContext.getLogger(name);

                logger.addAppender(ai.appender);
            }
        }
    }

    @Override
    public void onResetStart(final LoggerContext context) {
        attachAppenders();
    }

    @Override
    public void onResetComplete(final LoggerContext context) {
        @SuppressWarnings("unchecked")
        Map<String,Set<String>> appenderRefBag =
                (Map<String, Set<String>>) context.getObject(OsgiAppenderRefInternalAction.OSGI_APPENDER_REF_BAG);
        if(appenderRefBag == null){
            appenderRefBag = Collections.emptyMap();
        }
        this.appenderNameToLoggerMap.clear();
        this.appenderNameToLoggerMap.putAll(appenderRefBag);

        attachAppenders();
    }

    @Override
    public synchronized void close() {
        super.close();
        appenders.clear();
    }

    private void attachAppenders() {
        for (AppenderInfo ai : appenders.values()) {
            attachAppender(ai);
        }
    }

    private static Filter createFilter() throws InvalidSyntaxException {
        String filter = String.format("(&(objectClass=%s)(%s=*))", Appender.class.getName(), PROP_LOGGER);
        return FrameworkUtil.createFilter(filter);
    }

    class AppenderInfo {
        private final List<String> loggers;

        final Appender<ILoggingEvent> appender;

        final String pid;

        final String name;

        public AppenderInfo(final ServiceReference ref, Appender<ILoggingEvent> appender) {
            this.appender = appender;
            this.pid = ref.getProperty(Constants.SERVICE_ID).toString();
            List<String> loggers = new ArrayList<String>();
            for (String logger : Util.toList(ref.getProperty(PROP_LOGGER))) {
                loggers.add(logger);
            }

            this.loggers = loggers;
            this.name = appender.getName();
        }

        public Set<String> getLoggers(){
            Set<String> result = new HashSet<String>(loggers);

            if(name != null){
                Set<String> loggersFromConfig = appenderNameToLoggerMap.get(name);
                if(loggersFromConfig != null){
                    result.addAll(loggersFromConfig);
                }
            }

            return Collections.unmodifiableSet(result);
        }


    }
}
