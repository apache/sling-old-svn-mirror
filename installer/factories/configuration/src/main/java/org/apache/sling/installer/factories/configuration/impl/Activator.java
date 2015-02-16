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
package org.apache.sling.installer.factories.configuration.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator registers the configuration support service.
 */
public class Activator implements BundleActivator {

    /** Property for bundle location default. */
    private static final String PROP_LOCATION_DEFAULT = "sling.installer.config.useMulti";

    /** Services listener. */
    private ServicesListener listener;

    public static String DEFAULT_LOCATION;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        String locationDefault = null;
        if ( context.getProperty(PROP_LOCATION_DEFAULT) != null ) {
            final Boolean bool = Boolean.valueOf(context.getProperty(PROP_LOCATION_DEFAULT).toString());
            if ( bool.booleanValue() ) {
                locationDefault = "?";
            }
        }
        DEFAULT_LOCATION = locationDefault;
        this.listener = new ServicesListener(context);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) {
        if ( this.listener != null ) {
            this.listener.deactivate();
            this.listener = null;
        }
    }
}
