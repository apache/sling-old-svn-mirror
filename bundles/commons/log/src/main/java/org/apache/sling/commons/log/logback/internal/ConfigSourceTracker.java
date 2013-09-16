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

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import ch.qos.logback.classic.LoggerContext;

import org.apache.sling.commons.log.logback.ConfigProvider;
import org.apache.sling.commons.log.logback.internal.util.XmlUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.InputSource;

public class ConfigSourceTracker extends ServiceTracker implements LogbackResetListener {
    /**
     * Service property name indicating that String object is a Logback config
     * fragment
     */
    private static final String PROP_LOGBACK_CONFIG = "logbackConfig";

    /**
     * Reverse sorted map of ConfigSource based on ranking of ServiceReferences
     */
    private final Map<ServiceReference, ConfigSourceInfo> inputSources = new ConcurrentSkipListMap<ServiceReference, ConfigSourceInfo>(
        Collections.reverseOrder());

    private final LogbackManager logbackManager;

    public ConfigSourceTracker(BundleContext context, LogbackManager logbackManager) throws InvalidSyntaxException {
        super(context, createFilter(), null);
        this.logbackManager = logbackManager;
        super.open();
    }

    public Collection<ConfigSourceInfo> getSources() {
        return inputSources.values();
    }

    @Override
    public synchronized void close() {
        super.close();
        inputSources.clear();
    }

    // ~--------------------------------- ServiceTracker

    @Override
    public Object addingService(ServiceReference reference) {
        Object o = super.addingService(reference);
        inputSources.put(reference, new ConfigSourceInfo(reference, getConfig(o)));
        logbackManager.configChanged();
        return o;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        super.modifiedService(reference, service);
        // A ConfigProvider can modify its service registration properties
        // to indicate that config has changed and a reload is required
        logbackManager.configChanged();
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        if (inputSources.remove(reference) != null) {
            logbackManager.configChanged();
        }
    }

    // ~----------------------------------- LogbackResetListener
    @Override
    public void onResetStart(LoggerContext context) {
        // export the tracker instance. It would later be used in
        // OSGiInternalAction
        context.putObject(ConfigSourceTracker.class.getName(), this);
    }

    @Override
    public void onResetComplete(LoggerContext context) {

    }

    // ~----------------------------------ConfigSourceInfo

    public static class ConfigSourceInfo {
        private final ServiceReference reference;

        private final ConfigProvider configProvider;

        public ConfigSourceInfo(ServiceReference reference, ConfigProvider configProvider) {
            this.reference = reference;
            this.configProvider = configProvider;
        }

        public ConfigProvider getConfigProvider() {
            return configProvider;
        }

        public ServiceReference getReference() {
            return reference;
        }

        public String getSourceAsString() {
            return XmlUtil.prettyPrint(getConfigProvider().getConfigSource());
        }

        public String getSourceAsEscapedString() {
            return XmlUtil.escapeXml(getSourceAsString());
        }

        public String toString() {
            return String.format("Service ID %s", reference.getProperty(Constants.SERVICE_ID));
        }
    }

    private static ConfigProvider getConfig(Object o) {
        // If string then wrap it in StringSourceProvider
        if (o instanceof String) {
            return new StringSourceProvider((String) o);
        }
        return (ConfigProvider) o;
    }

    private static Filter createFilter() throws InvalidSyntaxException {
        // Look for either ConfigProvider or String's with property
        // logbackConfig set
        String filter = String.format("(|(objectClass=%s)(&(objectClass=java.lang.String)(%s=*)))",
            ConfigProvider.class.getName(), PROP_LOGBACK_CONFIG);
        return FrameworkUtil.createFilter(filter);
    }

    private static class StringSourceProvider implements ConfigProvider {
        private final String source;

        private StringSourceProvider(String source) {
            this.source = source;
        }

        public InputSource getConfigSource() {
            return new InputSource(new StringReader(source));
        }
    }
}
