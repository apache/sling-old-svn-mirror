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
package org.apache.sling.installer.it;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/** Utility that waits for no OSGi events to happen in a given amount
 *  of time.
 */
class EventsDetector implements FrameworkListener, BundleListener, ConfigurationListener, ServiceListener {

    private long lastEvent;
    private final ServiceRegistration configReg;
    private final BundleContext ctx;

    EventsDetector(BundleContext ctx) {
        this.ctx = ctx;
        ctx.addBundleListener(this);
        ctx.addFrameworkListener(this);
        ctx.addServiceListener(this);
        configReg = ctx.registerService(ConfigurationListener.class.getName(), this, null);
    }

    void close() {
        configReg.unregister();
        ctx.removeServiceListener(this);
        ctx.removeFrameworkListener(this);
        ctx.removeBundleListener(this);
    }

    void waitForNoEvents(long timeWithoutEventsMsec, long timeoutMsec) throws InterruptedException {
        final long endTime = System.currentTimeMillis() + timeoutMsec;
        final long exitTime = lastEvent + timeWithoutEventsMsec;
        while(System.currentTimeMillis() < endTime) {
            if(System.currentTimeMillis() >= exitTime) {
                return;
            }
            Thread.sleep(100L);
        }
        throw new IllegalStateException("Did not get " + timeWithoutEventsMsec + " msec without events after waiting " + timeoutMsec);
    }

    private void recordLastEvent() {
        lastEvent = System.currentTimeMillis();
    }

    public void frameworkEvent(FrameworkEvent arg0) {
        recordLastEvent();
    }

    public void bundleChanged(BundleEvent arg0) {
        recordLastEvent();
    }

    public void configurationEvent(ConfigurationEvent arg0) {
        recordLastEvent();
    }

    public void serviceChanged(ServiceEvent arg0) {
        recordLastEvent();
    }
}
