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
package org.apache.sling.installer.core.impl;

import org.apache.sling.installer.api.event.InstallationEvent;
import org.apache.sling.installer.api.event.InstallationListener;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class InstallListener implements InstallationListener {

    private final ServiceTracker tracker;

    public InstallListener(final BundleContext context) {
        this.tracker = new ServiceTracker(context, InstallationListener.class.getName(), null);
        this.tracker.open();
    }

    public void dispose() {
        this.tracker.close();
    }

    /**
     * @see org.apache.sling.installer.api.event.InstallationListener#onEvent(org.apache.sling.installer.api.event.InstallationEvent)
     */
    public void onEvent(final InstallationEvent event) {
        final Object[] listeners = this.tracker.getServices();
        if ( listeners != null ) {
            for(final Object l : listeners) {
                if ( l instanceof InstallationListener ) {
                    ((InstallationListener)l).onEvent(event);
                }
            }
        }
    }
}
