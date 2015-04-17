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
package org.apache.sling.installer.provider.file.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The <code>Activator</code>
 */
public class Activator implements BundleActivator {

    public static final String KEY_DIR = "sling.fileinstall.dir";
    public static final String KEY_DELAY = "sling.fileinstall.interval";
    public static final String KEY_WRITEBACK = "sling.fileinstall.writeback";

    /** The services listener will activate the installer. */
    private ServicesListener servicesListener;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) {
        // read initial scan configurations
        final List<ScanConfiguration> configs = new ArrayList<ScanConfiguration>();
        final Object dir = getProp(context, KEY_DIR);
        if ( dir != null ) {
            Long delay = null;
            final Object interval = getProp(context, KEY_DELAY);
            if ( interval != null ) {
                if ( interval instanceof Number ) {
                    delay = ((Number)interval).longValue();
                } else {
                    delay = Long.valueOf(interval.toString());
                }
            }
            final StringTokenizer st = new StringTokenizer(dir.toString(), ",");
            while ( st.hasMoreTokens() ) {
                final ScanConfiguration sc = new ScanConfiguration();
                sc.directory = st.nextToken();
                sc.scanInterval = delay;

                configs.add(sc);
            }
        }
        this.servicesListener = new ServicesListener(context, configs);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) {
        this.servicesListener.deactivate();
        this.servicesListener = null;
    }

    public static Object getProp(final BundleContext bundleContext, final String key) {
        Object o = bundleContext.getProperty(key);
        if (o == null) {
            o = System.getProperty(key);
            if ( o == null ) {
                o = System.getProperty(key.toUpperCase().replace('.', '_'));
            }
        }
        return o;
    }
}
