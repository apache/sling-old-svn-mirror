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
package org.apache.sling.osgi.installer.impl;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/** EventsCounter implementation - simply counts events,
 *  to avoid having to make each BundleStartTask that's
 *  waiting for retries a FrameworkListener and BundleListener 
 */
class EventsCounterImpl implements EventsCounter, FrameworkListener, BundleListener {
    private long eventsCount;
    private final BundleContext bundleContext;
    
    EventsCounterImpl(BundleContext bc) {
        this.bundleContext = bc;
        bundleContext.addBundleListener(this);
        bundleContext.addFrameworkListener(this);
    }
    
    void deactivate() {
        bundleContext.removeBundleListener(this);
        bundleContext.removeFrameworkListener(this);
    }
    
    public long getTotalEventsCount() {
        return eventsCount;
    }

    public void frameworkEvent(FrameworkEvent arg0) {
        // we'll retry as soon as any FrameworkEvent or BundleEvent happens
        eventsCount++;
    }

    public void bundleChanged(BundleEvent arg0) {
        // we'll retry as soon as any FrameworkEvent or BundleEvent happens
        eventsCount++;
    }
}
