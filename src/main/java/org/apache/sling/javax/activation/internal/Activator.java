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
package org.apache.sling.javax.activation.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <tt>Activator</tt> locates <tt>activation</tt>-related services defined in <tt>mailcap</tt> files and registers
 * them so they can be used by the Java Activation Framework
 * 
 * <p>
 * The mailcap entries are expected to be found in a <tt>/META-INF/mailcap</tt> file inside the bundle.
 * </p>
 * 
 * <p>
 * This implementation does not support the full lookup algorithm specified by the {@link MailcapCommandMap}.
 * </p>
 * 
 */
public class Activator implements BundleActivator {

    private static final String MAILCAP_FILE_NAME = "/META-INF/mailcap";
    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    private BundleTracker bundleTracker;
    private OsgiMailcapCommandMap commandMap;

    public void start(BundleContext context) throws Exception {

        commandMap = new OsgiMailcapCommandMap();

        for (Bundle bundle : context.getBundles())
            registerBundleMailcapEntries(bundle);

        CommandMap.setDefaultCommandMap(commandMap);

        bundleTracker = new BundleTracker(context, Bundle.ACTIVE | Bundle.UNINSTALLED | Bundle.STOP_TRANSIENT,
                new BundleTrackerCustomizer() {

                    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
                        unregisterBundleMailcapEntries(bundle);
                    }

                    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
                        unregisterBundleMailcapEntries(bundle);
                        registerBundleMailcapEntries(bundle);
                    }

                    public Object addingBundle(Bundle bundle, BundleEvent event) {
                        registerBundleMailcapEntries(bundle);
                        return bundle;
                    }
                });

        bundleTracker.open();
    }

    private void registerBundleMailcapEntries(Bundle bundle) {

        if (bundle.getState() != Bundle.ACTIVE)
            return;

        URL mailcapEntry = bundle.getEntry(MAILCAP_FILE_NAME);
        if (mailcapEntry == null)
            return;

        InputStream input = null;

        try {
            input = mailcapEntry.openStream();

            commandMap.addMailcapEntries(input, bundle);

        } catch (IOException e) {
            log.warn("Failed loading " + MAILCAP_FILE_NAME + " from bundle " + bundle, e);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                // don't care
            }
        }
    }

    private void unregisterBundleMailcapEntries(Bundle bundle) {

        commandMap.removeMailcapEntriesForBundle(bundle);
    }

    public void stop(BundleContext context) throws Exception {

        if (bundleTracker != null) {
            bundleTracker.close();
            bundleTracker = null;
        }

        CommandMap.setDefaultCommandMap(null);
    }

}
