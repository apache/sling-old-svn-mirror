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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

import org.apache.sling.commons.log.logback.internal.util.Util;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class AppenderTracker extends ServiceTracker implements LogbackResetListener {
    private static final String PROP_LOGGER = "loggers";

    private final LoggerContext loggerContext;

    private final Map<ServiceReference, AppenderInfo> appenders = new ConcurrentHashMap<ServiceReference, AppenderInfo>();

    public AppenderTracker(BundleContext context, LoggerContext loggerContext) throws InvalidSyntaxException {
        super(context, createFilter(), null);
        this.loggerContext = loggerContext;
        super.open();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object addingService(ServiceReference reference) {
        Appender<ILoggingEvent> a = (Appender<ILoggingEvent>) super.addingService(reference);
        a.setContext(loggerContext);
        a.start();

        AppenderInfo ai = new AppenderInfo(reference, a);
        appenders.put(reference, ai);
        attachAppender(ai);
        return ai;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        AppenderInfo ai = appenders.remove(reference);
        detachAppender(ai);
        appenders.put(reference, new AppenderInfo(reference, (Appender<ILoggingEvent>) service));
        attachAppender(ai);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        detachAppender(appenders.remove(reference));
        // Probably we should remove the context from appender
        super.removedService(reference, service);
    }

    public Collection<AppenderInfo> getAppenderInfos() {
        return appenders.values();
    }

    private void detachAppender(AppenderInfo ai) {
        if (ai != null) {
            for (LoggerInfo li : ai.loggers) {
                Logger logger = loggerContext.getLogger(li.name);

                // Reset values back to old ones if they match the
                // ones we modified to
                if (li.level.equals(logger.getLevel())) {
                    logger.setLevel(li.oldLevel);
                }

                if (logger.isAdditive() == li.additive) {
                    logger.setAdditive(li.oldAdditive);
                }

                logger.detachAppender(ai.appender);
            }
        }
    }

    private void attachAppender(AppenderInfo ai) {
        if (ai == null) {
            return;
        }
        for (LoggerInfo li : ai.loggers) {
            Logger logger = loggerContext.getLogger(li.name);

            li.oldLevel = logger.getLevel();
            li.oldAdditive = logger.isAdditive();

            logger.setLevel(li.level);
            logger.setAdditive(li.additive);

            logger.addAppender(ai.appender);
        }
    }

    @Override
    public void onResetStart(LoggerContext context) {
        for (AppenderInfo ai : appenders.values()) {
            attachAppender(ai);
        }
    }

    @Override
    public void onResetComplete(LoggerContext context) {

    }

    @Override
    public synchronized void close() {
        super.close();
        appenders.clear();
    }

    private static Filter createFilter() throws InvalidSyntaxException {
        String filter = String.format("(&(objectClass=%s)(%s=*))", Appender.class.getName(), PROP_LOGGER);
        return FrameworkUtil.createFilter(filter);
    }

    static class AppenderInfo {
        final List<LoggerInfo> loggers;

        final Appender<ILoggingEvent> appender;

        final ServiceReference serviceReference;

        public AppenderInfo(ServiceReference ref, Appender<ILoggingEvent> appender) {
            this.appender = appender;
            this.serviceReference = ref;

            List<LoggerInfo> loggers = new ArrayList<LoggerInfo>();
            for (String logger : Util.toList(ref.getProperty(PROP_LOGGER))) {
                loggers.add(new LoggerInfo(logger));
            }

            this.loggers = loggers;
        }
    }

    private static class LoggerInfo {
        final Level level;

        final String name;

        final boolean additive;

        Level oldLevel;

        boolean oldAdditive;

        public LoggerInfo(String loggerSpec) {
            String[] parts = loggerSpec.split(":");

            Level level = Level.INFO;
            if (parts.length >= 2) {
                level = Level.valueOf(safeTrim(parts[1]));
            }

            boolean additive = false;
            if (parts.length >= 3) {
                additive = Boolean.valueOf(parts[2]);
            }

            this.level = level;
            this.name = safeTrim(parts[0]);
            this.additive = additive;
        }

    }

    private static String safeTrim(String s) {
        if (s != null) {
            return s.trim();
        }
        return null;
    }

}
