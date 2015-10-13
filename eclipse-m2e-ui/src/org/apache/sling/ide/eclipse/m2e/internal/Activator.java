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
package org.apache.sling.ide.eclipse.m2e.internal;

import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.eclipse.core.ServiceUtil;
import org.apache.sling.ide.eclipse.core.debug.PluginLoggerRegistrar;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends Plugin {

    public static final String PLUGIN_ID = "org.apache.sling.ide.eclipse-m2e-ui";
    public static Activator INSTANCE;

    private ServiceTracker<EmbeddedArtifactLocator, EmbeddedArtifactLocator> artifactLocator;
    private ServiceTracker<OsgiClientFactory, OsgiClientFactory> osgiClientFactory;

    private ServiceRegistration<Logger> tracerRegistration;
    private ServiceTracker<Logger, Logger> tracer;

    public static Activator getDefault() {
        return INSTANCE;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        INSTANCE = this;

        artifactLocator = new ServiceTracker<>(context, EmbeddedArtifactLocator.class, null);
        artifactLocator.open();

        osgiClientFactory = new ServiceTracker<>(context, OsgiClientFactory.class,
                null);
        osgiClientFactory.open();

        tracerRegistration = PluginLoggerRegistrar.register(this);

        tracer = new ServiceTracker<>(context, tracerRegistration.getReference(), null);
        tracer.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        INSTANCE = null;

        artifactLocator.close();

        super.stop(context);
    }

    public EmbeddedArtifactLocator getArtifactsLocator() {

        return ServiceUtil.getNotNull(artifactLocator);
    }

    public OsgiClientFactory getOsgiClientFactory() {

        return ServiceUtil.getNotNull(osgiClientFactory);
    }

    public Logger getPluginLogger() {
        return (Logger) ServiceUtil.getNotNull(tracer);
    }
}
