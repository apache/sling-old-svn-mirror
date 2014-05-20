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
package org.apache.sling.extensions.datasource.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = DriverRegistry.class)
public class DriverRegistry {
    private static final String DRIVER_SERVICE = "META-INF/services/"
            + Driver.class.getName();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private BundleTracker<Collection<DriverInfo>> bundleTracker;

    private ConcurrentMap<DriverInfo, Driver> driverInfos = new ConcurrentHashMap<DriverInfo, Driver>();

    public Collection<Driver> getDrivers() {
        return driverInfos.values();
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        bundleTracker = new BundleTracker<Collection<DriverInfo>>(bundleContext,
                Bundle.ACTIVE, new DriverBundleTracker());
        bundleTracker.open();
    }

    @Deactivate
    protected void deactivate() {
        if (bundleTracker != null) {
            bundleTracker.close();
        }
    }

    private void registerDrivers(Collection<DriverInfo> drivers) {
        for (DriverInfo di : drivers) {
            driverInfos.put(di, di.driver);
            log.info("Registering {}", di);
        }
    }

    private void deregisterDrivers(Collection<DriverInfo> drivers) {
        for (DriverInfo di : drivers) {
            driverInfos.remove(di);
            log.info("Deregistering {}", di);
        }
    }

    private Collection<DriverInfo> createDrivers(final Bundle bundle) {
        URL url = bundle.getEntry(DRIVER_SERVICE);
        InputStream ins = null;
        final List<DriverInfo> extensions = new ArrayList<DriverInfo>();
        try {
            ins = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && line.trim().length() > 0) {
                    try {
                        Class<?> clazz = bundle.loadClass(line);
                        extensions.add(new DriverInfo(bundle, (Driver) clazz.newInstance()));
                    } catch (Throwable t) {
                        log.warn("Cannot register java.sql.Driver [{}] from bundle [{}]",
                                new Object[]{line, bundle, t});
                    }
                }
            }
        } catch (IOException ioe) {
            // ignore
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }

        return extensions;
    }

    private class DriverBundleTracker implements BundleTrackerCustomizer<Collection<DriverInfo>> {
        public Collection<DriverInfo> addingBundle(Bundle bundle, BundleEvent event) {
            if (bundle.getEntry(DRIVER_SERVICE) != null) {
                Collection<DriverInfo> drivers = createDrivers(bundle);
                registerDrivers(drivers);
                return drivers;
            }
            return null;
        }

        public void modifiedBundle(Bundle bundle, BundleEvent event, Collection<DriverInfo> object) {

        }

        public void removedBundle(Bundle bundle, BundleEvent event, Collection<DriverInfo> drivers) {
            deregisterDrivers(drivers);
        }
    }

    private static class DriverInfo {
        final Driver driver;
        final Bundle bundle;

        DriverInfo(Bundle bundle, Driver driver) {
            this.driver = driver;
            this.bundle = bundle;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DriverInfo that = (DriverInfo) o;

            if (!(bundle == that.bundle)) return false;
            if (!(driver == that.driver)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = driver.hashCode();
            result = 31 * result + bundle.hashCode();
            return result;
        }

        public String toString() {
            return String.format("java.sql.Driver [%s] from bundle [%s]", driver.getClass().getName(), bundle);
        }
    }
}
