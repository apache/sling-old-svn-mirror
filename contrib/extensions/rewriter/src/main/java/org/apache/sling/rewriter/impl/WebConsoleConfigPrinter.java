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
package org.apache.sling.rewriter.impl;

import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * This is a configuration printer for the web console which
 * prints out the currently configured processors/pipelines.
 *
 */
public class WebConsoleConfigPrinter {

    final ProcessorManagerImpl manager;

    private static ServiceRegistration REG;

    public WebConsoleConfigPrinter(final ProcessorManagerImpl manager) {
        this.manager = manager;
    }

    public static void register(final BundleContext bundleContext,
                                final ProcessorManagerImpl manager) {
        final WebConsoleConfigPrinter printer = new WebConsoleConfigPrinter(manager);
        final Dictionary<String, String> serviceProps = new Hashtable<String, String>();
        serviceProps.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Rewriter Configuration Printer");
        serviceProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        serviceProps.put("felix.webconsole.label", "slingrewriter");
        serviceProps.put("felix.webconsole.title", "Sling Rewriter");
        serviceProps.put("felix.webconsole.configprinter.modes", "always");

        REG = bundleContext.registerService(WebConsoleConfigPrinter.class.getName(),
                printer,
                serviceProps);
    }

    public static void unregister() {
        if ( REG != null) {
            REG.unregister();
            REG = null;
        }
    }

    /**
     * Print out the rewriter configs.
     * See org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter).
     */
    public void printConfiguration(PrintWriter pw) {
        this.manager.printConfiguration(pw);
    }
}
