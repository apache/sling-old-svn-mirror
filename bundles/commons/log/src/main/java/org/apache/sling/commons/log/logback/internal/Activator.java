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

package org.apache.sling.commons.log.logback.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Activator implements BundleActivator {
    private static final String JUL_SUPPORT = "org.apache.sling.commons.log.julenabled";

    private LogbackManager logManager;

    public void start(BundleContext context) throws Exception {
        // SLING-2373
        if (Boolean.parseBoolean(context.getProperty(JUL_SUPPORT))) {
            // In config one must enable the LevelChangePropagator
            // http://logback.qos.ch/manual/configuration.html#LevelChangePropagator
            // make sure configuration is empty unless explicitly set
            if (System.getProperty("java.util.logging.config.file") == null
                && System.getProperty("java.util.logging.config.class") == null) {
                final Thread ct = Thread.currentThread();
                final ClassLoader old = ct.getContextClassLoader();
                try {
                    ct.setContextClassLoader(getClass().getClassLoader());
                    System.setProperty("java.util.logging.config.class",
                        "org.apache.sling.commons.log.internal.Activator.DummyLogManagerConfiguration");
                    java.util.logging.LogManager.getLogManager().reset();
                } finally {
                    ct.setContextClassLoader(old);
                    System.clearProperty("java.util.logging.config.class");
                }
            }

            SLF4JBridgeHandler.install();
        }

        logManager = new LogbackManager(context);
    }

    public void stop(BundleContext context) throws Exception {
        SLF4JBridgeHandler.uninstall();

        if (logManager != null) {
            logManager.shutdown();
            logManager = null;
        }
    }

    /**
     * The <code>DummyLogManagerConfiguration</code> class is used as JUL
     * LogginManager configurator to preven reading platform default
     * configuration which just duplicate log output to be redirected to SLF4J.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class DummyLogManagerConfiguration {
    }
}
