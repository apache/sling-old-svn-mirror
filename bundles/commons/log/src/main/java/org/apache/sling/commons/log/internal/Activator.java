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
package org.apache.sling.commons.log.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * The <code>Activator</code> class is the <code>BundleActivator</code> for
 * the log service bundle. This activator sets up logging in NLog4J and
 * registers the <code>LogService</code> and <code>LogReaderService</code>.
 * When the bundle is stopped, the NLog4J subsystem is simply shutdown.
 */
public class Activator implements BundleActivator {

    private static final String JUL_SUPPORT = "org.apache.sling.commons.log.julenabled";

    private LogManager logManager;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        logManager = new LogManager(context);

        if (Boolean.parseBoolean(context.getProperty(JUL_SUPPORT))) {
            SLF4JBridgeHandler.install();
        }
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        SLF4JBridgeHandler.uninstall();

        if (logManager != null) {
            logManager.shutdown();
            logManager = null;
        }
    }
}
