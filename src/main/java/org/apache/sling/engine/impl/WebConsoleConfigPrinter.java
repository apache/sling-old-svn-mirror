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
package org.apache.sling.engine.impl;

import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.engine.impl.filter.ServletFilterManager;
import org.apache.sling.engine.impl.filter.ServletFilterManager.FilterChainType;
import org.apache.sling.engine.impl.filter.SlingFilterChainHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * This is a configuration printer for the web console which
 * prints out the currently configured filter chains.
 *
 */
public class WebConsoleConfigPrinter {

    private final ServletFilterManager filterManager;

    public WebConsoleConfigPrinter(final ServletFilterManager filterManager) {
        this.filterManager = filterManager;
    }

    private static final class Registration {
        public ServiceRegistration filterPlugin;
    }

    public static Object register(final BundleContext bundleContext,
            final ServletFilterManager filterManager) {
        final Registration reg = new Registration();

        // first we register the plugin for the filters
        final WebConsoleConfigPrinter filterPrinter = new WebConsoleConfigPrinter(filterManager);
        final Dictionary<String, String> serviceProps = new Hashtable<String, String>();
        serviceProps.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Servlet Filter Configuration Printer");
        serviceProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        serviceProps.put("felix.webconsole.label", "slingfilter");
        serviceProps.put("felix.webconsole.title", "Sling Servlet Filter");
        serviceProps.put("felix.webconsole.configprinter.modes", "always");

        reg.filterPlugin = bundleContext.registerService(WebConsoleConfigPrinter.class.getName(),
                filterPrinter,
                serviceProps);
        return reg;
    }

    public static void unregister(final Object reg) {
        if ( reg instanceof Registration ) {
            final Registration registration = (Registration)reg;
            if ( registration.filterPlugin != null) {
                registration.filterPlugin.unregister();
                registration.filterPlugin = null;
            }
        }
    }

    /**
     * Helper method for printing out a filter chain.
     */
    private void printFilterChain(final PrintWriter pw, final SlingFilterChainHelper.FilterListEntry[] entries) {
        if ( entries == null ) {
            pw.println("---");
        } else {
            for(final SlingFilterChainHelper.FilterListEntry entry : entries) {
                pw.print(entry.getOrder());
                pw.print(" : ");
                pw.print(entry.getFilter().getClass());
                pw.print(" (id: ");
                pw.print(entry.getFilterId());
                pw.print(", property: ");
                pw.print(entry.getOrderSource());
                pw.println(")");
            }
        }
    }

    /**
     * Print out the servlet filter chains.
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw) {
        pw.println("Current Apache Sling Servlet Filter Configuration");
        for (FilterChainType type : FilterChainType.values()) {
            pw.println();
            pw.println(type + " Filters:");
            printFilterChain(pw,
                filterManager.getFilterChain(type).getFilterListEntries());
        }
    }
}
