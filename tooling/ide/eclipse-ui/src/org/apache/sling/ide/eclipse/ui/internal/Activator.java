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
package org.apache.sling.ide.eclipse.ui.internal;

import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.eclipse.core.ServiceUtil;
import org.apache.sling.ide.eclipse.core.debug.PluginLoggerRegistrar;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.apache.sling.ide.serialization.SerializationManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.apache.sling.ide.eclipse-core";
    public static Activator INSTANCE;

    private ServiceTracker<SerializationManager, SerializationManager> serializationManager;
    private ServiceTracker<FilterLocator, FilterLocator> filterLocator;
    private ServiceTracker<EventAdmin, EventAdmin> eventAdmin;
    private ServiceTracker<EmbeddedArtifactLocator, EmbeddedArtifactLocator> artifactLocator;
    private ServiceTracker<OsgiClientFactory, OsgiClientFactory> osgiClientFactory;
    private ServiceTracker<Logger, Logger> tracer;

    private ServiceRegistration<Logger> tracerRegistration;

    public static Activator getDefault() {

        return INSTANCE;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        tracerRegistration = PluginLoggerRegistrar.register(this);

        serializationManager = new ServiceTracker<>(context, SerializationManager.class, null);
        serializationManager.open();

        filterLocator = new ServiceTracker<>(context, FilterLocator.class, null);
        filterLocator.open();

        eventAdmin = new ServiceTracker<>(context, EventAdmin.class, null);
        eventAdmin.open();

        artifactLocator = new ServiceTracker<>(context,
                EmbeddedArtifactLocator.class, null);
        artifactLocator.open();

        osgiClientFactory = new ServiceTracker<>(context, OsgiClientFactory.class,
                null);
        osgiClientFactory.open();

        tracer = new ServiceTracker<>(context, tracerRegistration.getReference(), null);
        tracer.open();

        INSTANCE = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        INSTANCE = null;
        serializationManager.close();
        filterLocator.close();
        eventAdmin.close();
        artifactLocator.close();
        osgiClientFactory.close();

        super.stop(context);
    }

    public SerializationManager getSerializationManager() {
        return ServiceUtil.getNotNull(serializationManager);
    }

    public FilterLocator getFilterLocator() {
        return ServiceUtil.getNotNull(filterLocator);
    }

    public EventAdmin getEventAdmin() {
        return ServiceUtil.getNotNull(eventAdmin);
    }

    public EmbeddedArtifactLocator getArtifactLocator() {

        return ServiceUtil.getNotNull(artifactLocator);
    }

    public OsgiClientFactory getOsgiClientFactory() {
        return ServiceUtil.getNotNull(osgiClientFactory);
    }

    public Logger getPluginLogger() {
        return (Logger) ServiceUtil.getNotNull(tracer);
    }
}
