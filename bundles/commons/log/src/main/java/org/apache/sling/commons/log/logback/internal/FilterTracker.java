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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import org.apache.sling.commons.log.logback.internal.util.Util;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FilterTracker extends ServiceTracker implements LogbackResetListener{
    private static final String PROP_APPENDER = "appenders";

    private final LoggerContext loggerContext;
    private final LogbackManager logbackManager;
    private Map<ServiceReference, FilterInfo> filters = new ConcurrentHashMap<ServiceReference, FilterInfo>();

    public FilterTracker(BundleContext context, LogbackManager logbackManager) throws InvalidSyntaxException {
        super(context, createFilter(), null);
        this.logbackManager = logbackManager;
        this.loggerContext = logbackManager.getLoggerContext();
        super.open();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object addingService(ServiceReference reference) {
        Filter<ILoggingEvent> f = (Filter<ILoggingEvent>) super.addingService(reference);
        f.setContext(loggerContext);
        f.start();

        FilterInfo fi = new FilterInfo(reference, f);
        filters.put(reference, fi);
        attachFilter(fi, getAppenderMap());
        return fi;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        FilterInfo fi = filters.remove(reference);
        detachFilter(fi, getAppenderMap());
        filters.put(reference, new FilterInfo(reference, (Filter<ILoggingEvent>) service));
        attachFilter(fi, getAppenderMap());
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        FilterInfo fi = filters.remove(reference);
        fi.stop();

        detachFilter(fi, getAppenderMap());
        super.removedService(reference, service);
    }

    @Override
    public synchronized void close() {
        super.close();
        filters.clear();
    }

    //~-----------------------------------LogbackResetListener

    @Override
    public void onResetStart(LoggerContext context) {

    }

    @Override
    public void onResetComplete(LoggerContext context) {
        //The filters are attached at end when all appenders have been instantiated
        Map<String,Appender<ILoggingEvent>> appenderMap = getAppenderMap();
        for(FilterInfo fi : filters.values()){
            attachFilter(fi,appenderMap);
        }
    }

    //~-----------------------------------Internal Methods

    private void attachFilter(FilterInfo fi, Map<String,Appender<ILoggingEvent>> appenderMap) {
        //TODO Support attaching a filter to all appender if the appenerName list contains '*'
        for(String appenderName : fi.appenderNames){
            Appender<ILoggingEvent> appender = appenderMap.get(appenderName);
            if(appender != null){
                attachFilter(appender,fi);
            }else{
                //TODO Log warning
            }
        }
    }

    private void detachFilter(FilterInfo fi,Map<String,Appender<ILoggingEvent>> appenderMap) {
        for(String appenderName : fi.appenderNames){
            Appender<ILoggingEvent> appender = appenderMap.get(appenderName);
            if(appender != null){
                detachFilter(appender, fi);
            }else{
                //TODO Log warning
            }
        }
    }

    private void attachFilter(Appender<ILoggingEvent> appender, FilterInfo fi){
        //TOCHECK Should we add based on some ranking
        if(!appender.getCopyOfAttachedFiltersList().contains(fi.filter)){
            appender.addFilter(fi.filter);
        }
    }

    private void detachFilter(Appender<ILoggingEvent> appender, FilterInfo fi){
        //No method to directly remove filter. So clone -> remove -> add
        if(appender.getCopyOfAttachedFiltersList().contains(fi.filter)){
            //Clone
            List<Filter<ILoggingEvent>> filters = appender.getCopyOfAttachedFiltersList();

            //Clear
            appender.clearAllFilters();

            //Add
            for(Filter<ILoggingEvent> filter : filters){
                if(!fi.filter.equals(filter)){
                    appender.addFilter(filter);
                }
            }
        }
    }

    private Map<String,Appender<ILoggingEvent>> getAppenderMap() {
        return logbackManager.determineLoggerState().getAppenderMap();
    }


    private static org.osgi.framework.Filter createFilter() throws InvalidSyntaxException {
        String filter = String.format("(&(objectClass=%s)(%s=*))", Filter.class.getName(), PROP_APPENDER);
        return FrameworkUtil.createFilter(filter);
    }

    public static class FilterInfo {
        final ServiceReference reference;
        final Filter<ILoggingEvent> filter;
        final Set<String> appenderNames;

        FilterInfo(ServiceReference reference, Filter<ILoggingEvent> filter) {
            this.reference = reference;
            this.filter = filter;

            this.appenderNames = Collections.unmodifiableSet(
                    new HashSet<String>(Util.toList(reference.getProperty(PROP_APPENDER))));
        }

        public void stop(){
            if(filter.isStarted()){
                filter.stop();
            }
        }
    }
}
