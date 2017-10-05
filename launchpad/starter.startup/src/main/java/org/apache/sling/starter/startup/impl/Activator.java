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
package org.apache.sling.starter.startup.impl;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.info.InstallationState;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    private final String[] REQUIRED_SERVICES = new String[] {
            "org.apache.sling.api.auth.Authenticator",
            "org.apache.sling.api.resource.ResourceResolverFactory",
            "org.apache.sling.api.servlets.ServletResolver"
    };

    private volatile ServiceTracker<InfoProvider, InfoProvider> infoProviderTracker;

    private volatile HttpStartupSetup httpSetup;

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Override
    public void start(final BundleContext context) throws Exception {
        this.httpSetup = new HttpStartupSetup(context);
        this.httpSetup.start();

        this.setupInfoProviderTracker(context);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        stopped.set(true);
        this.stopInfoProviderTracker();

        if ( this.httpSetup != null ) {
            this.httpSetup.stop();
            this.httpSetup = null;
        }
    }

    /**
     * Setup the info provider tracker
     * @param context The bundle context
     */
    private void setupInfoProviderTracker(final BundleContext context) {
        this.infoProviderTracker = new ServiceTracker<>(context, InfoProvider.class, new ServiceTrackerCustomizer<InfoProvider, InfoProvider>() {

            private final AtomicInteger counter = new AtomicInteger();

            private final Timer timer = new Timer();

            @Override
            public InfoProvider addingService(final ServiceReference<InfoProvider> reference) {
                if ( stopped.get() ) {
                    return null;
                }
                final InfoProvider service = context.getService(reference);
                if ( counter.incrementAndGet() == 1 ) {
                    startCheck(service);
                }
                return service;
            }

            @Override
            public void modifiedService(final ServiceReference<InfoProvider> reference, final InfoProvider service) {
                // nothing to do
            }

            @Override
            public void removedService(final ServiceReference<InfoProvider> reference, final InfoProvider service) {
                if ( counter.decrementAndGet() == 0 ) {
                    stopCheck();
                }
                context.ungetService(reference);
            }

            private void startCheck(final InfoProvider service) {
                final TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        final InstallationState state = service.getInstallationState();
                        if ( state.getActiveResources().isEmpty()
                             && state.getUntransformedResources().isEmpty()
                             && checkServices(context)) {

                            httpSetup.stop();
                            this.cancel();
                        }
                    }
                };
                timer.schedule(task, 1000, 1000);
            }

            private void stopCheck() {
                timer.cancel();
                if ( !stopped.get() ) {
                    httpSetup.start();
                }
            }


        });
        this.infoProviderTracker.open();
    }

    /**
     * Stop the info provider tracker
     */
    private void stopInfoProviderTracker() {
        if ( infoProviderTracker != null ) {
            infoProviderTracker.close();
            infoProviderTracker = null;
        }
    }

    private boolean checkServices(final BundleContext context) {
        for(final String name : REQUIRED_SERVICES) {
            final ServiceReference<?> ref = context.getServiceReference(name);
            if ( ref == null ) {
                return false;
            }
            final Object service = context.getService(ref);
            if ( service == null ) {
                return false;
            }
            context.ungetService(ref);
        }
        return true;
    }
}
