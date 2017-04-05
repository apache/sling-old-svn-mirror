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

package org.apache.sling.commons.log.webconsole.internal;

import org.apache.sling.commons.log.logback.webconsole.LogPanel;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
    private ServiceTracker<LogPanel, LogWebConsolePlugin> panelTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        panelTracker = new ServiceTracker<LogPanel, LogWebConsolePlugin>(context, LogPanel.class, null) {
            @Override
            public LogWebConsolePlugin addingService(ServiceReference<LogPanel> reference) {
                LogPanel panel = context.getService(reference);
                LogWebConsolePlugin plugin = new LogWebConsolePlugin(panel);
                plugin.register(context);
                return plugin;
            }

            @Override
            public void removedService(ServiceReference<LogPanel> reference, LogWebConsolePlugin plugin) {
                plugin.unregister();
                context.ungetService(reference);
            }
        };
        panelTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (panelTracker != null) {
            panelTracker.close();
        }
    }
}
