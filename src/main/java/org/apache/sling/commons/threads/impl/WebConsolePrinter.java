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
package org.apache.sling.commons.threads.impl;

import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.webconsole.ConfigurationPrinter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * This is a configuration printer for the web console which
 * prints out the thread pools.
 *
 */
public class WebConsolePrinter implements ConfigurationPrinter {

    private static ServiceRegistration plugin;

    public static void initPlugin(final BundleContext bundleContext,
            final DefaultThreadPoolManager dtpm) {
        final WebConsolePrinter propertiesPrinter = new WebConsolePrinter(dtpm);
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Thread Pool Configuration Printer");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        plugin = bundleContext.registerService(ConfigurationPrinter.class.getName(),
                                               propertiesPrinter, props);
    }

    public static void destroyPlugin() {
        if ( plugin != null) {
            plugin.unregister();
            plugin = null;
        }
    }

    private static String HEADLINE = "Apache Sling Thread Pools";

    private final DefaultThreadPoolManager mgr;

    public WebConsolePrinter(final DefaultThreadPoolManager dtpm) {
        this.mgr = dtpm;
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#getTitle()
     */
    public String getTitle() {
        return "Sling Thread Pools";
    }

    /**
     * Print out the servlet filter chains.
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw) {
        pw.println(HEADLINE);
        pw.println();
        final DefaultThreadPoolManager.Entry[] configs = this.mgr.getConfigurations();
        if ( configs.length > 0 ) {
            for(final DefaultThreadPoolManager.Entry entry : configs ) {
                pw.print("Pool ");
                pw.println(entry.getName());
                if ( entry.getPid() != null ) {
                    pw.print("- from configuration : ");
                    pw.println(entry.getPid());
                }
                pw.print("- used : ");
                pw.println(entry.isUsed());
                pw.print("- min pool size : ");
                pw.println(entry.getConfig().getMinPoolSize());
                pw.print("- max pool size : ");
                pw.println(entry.getConfig().getMaxPoolSize());
                pw.print("- queue size : ");
                pw.println(entry.getConfig().getQueueSize());
                pw.print("- keep alive time : ");
                pw.println(entry.getConfig().getKeepAliveTime());
                pw.print("- block policy : ");
                pw.println(entry.getConfig().getBlockPolicy());
                pw.print("- priority : ");
                pw.println(entry.getConfig().getPriority());
                pw.print("- shutdown graceful : ");
                pw.println(entry.getConfig().isShutdownGraceful());
                pw.print("- shutdown wait time : ");
                pw.println(entry.getConfig().getShutdownWaitTimeMs());
                pw.print("- daemon : ");
                pw.println(entry.getConfig().isDaemon());
                pw.println();
            }
        } else {
            pw.println("No pools configured.");
        }
    }
}
