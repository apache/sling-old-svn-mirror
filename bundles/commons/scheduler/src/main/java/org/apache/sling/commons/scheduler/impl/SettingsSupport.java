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
package org.apache.sling.commons.scheduler.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * The <code>SettingsSupport</code> listens for the settings service.
 */
@Component
public class SettingsSupport {

    private static final String SETTINGS_NAME = "org.apache.sling.settings.SlingSettingsService";

    /** The listener for the settings service. */
    private volatile Listener settingsListener;

    /**
     * Start the component.
     * @param bc Bundle context
     */
    @Activate
    protected void activate(final BundleContext bc) {
        this.settingsListener = new Listener(bc);
        this.settingsListener.start();
    }

    /**
     * Stop the component.
     */
    @Deactivate
    protected void deactivate() {
        if ( this.settingsListener != null ) {
            this.settingsListener.stop();
            this.settingsListener = null;
        }
    }

    /**
     * Helper class listening for the settings service
     */
    protected static final class Listener implements ServiceListener {

        /** The bundle context. */
        private final BundleContext bundleContext;

        private final AtomicBoolean active = new AtomicBoolean(false);

        /**
         * Constructor
         */
        public Listener(final BundleContext bundleContext) {
            this.bundleContext = bundleContext;
        }

        /**
         * Start the listener.
         * First register a service listener and then check for the service.
         */
        public void start() {
            try {
                bundleContext.addServiceListener(this, "("
                        + Constants.OBJECTCLASS + "=" + SETTINGS_NAME + ")");
            } catch (final InvalidSyntaxException ise) {
                // this should really never happen
                throw new RuntimeException("Unexpected exception occured.", ise);
            }
            active.set(true);
            this.retainService();
        }

        /**
         * Unregister the listener.
         */
        public void stop() {
            if ( active.compareAndSet(true, false) ) {
                bundleContext.removeServiceListener(this);
            }
        }

        /**
         * Try to get the service and the Sling ID
         */
        private synchronized void retainService() {
            final ServiceReference reference = bundleContext.getServiceReference(SETTINGS_NAME);
            if ( reference != null ) {
                final SlingSettingsService service = (SlingSettingsService)bundleContext.getService(reference);
                if ( service != null ) {
                    QuartzJobExecutor.SLING_ID = service.getSlingId();
                    this.bundleContext.ungetService(reference);
                    // stop the listener, we don't need it anymore
                    this.stop();
                }
            }
        }

        /**
         * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
         */
        public void serviceChanged(final ServiceEvent event) {
            if (event.getType() == ServiceEvent.REGISTERED) {
                this.retainService();
            }
        }
    }
}
