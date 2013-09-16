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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TurboFilterTracker extends ServiceTracker implements LogbackResetListener{
    private final LoggerContext loggerContext;
    private final Map<ServiceReference,TurboFilter> filters = new ConcurrentHashMap<ServiceReference, TurboFilter>();

    public TurboFilterTracker(BundleContext context, LoggerContext loggerContext) {
        super(context, TurboFilter.class.getName(), null);
        this.loggerContext = loggerContext;
        super.open();
    }

    @Override
    public Object addingService(ServiceReference reference) {
        TurboFilter tf = (TurboFilter) super.addingService(reference);
        tf.setContext(loggerContext);
        tf.start();

        attachFilter(tf);
        filters.put(reference,tf);
        return tf;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        TurboFilter tf = (TurboFilter) service;
        filters.remove(reference);
        loggerContext.getTurboFilterList().remove(tf);
        super.removedService(reference, service);
    }

    @Override
    public void onResetStart(LoggerContext context) {
        for(TurboFilter tf : filters.values()){
            attachFilter(tf);
        }
    }

    @Override
    public synchronized void close() {
        super.close();
        filters.clear();
    }

    @Override
    public void onResetComplete(LoggerContext context) {

    }

    public Map<ServiceReference, TurboFilter> getFilters() {
        return Collections.unmodifiableMap(filters);
    }

    private void attachFilter(TurboFilter tf) {
        if(!loggerContext.getTurboFilterList().contains(tf)){
            loggerContext.addTurboFilter(tf);
        }
    }
}
