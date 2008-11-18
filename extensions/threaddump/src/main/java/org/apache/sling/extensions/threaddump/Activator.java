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
package org.apache.sling.extensions.threaddump;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class Activator implements BundleActivator {

    public void start(BundleContext bundleContext) {
        try {
            register(bundleContext,
                new String[] { "org.apache.felix.shell.Command" },
                new ThreadDumpCommand(), null);
        } catch (Throwable t) {
            // shell service might not be available, don't care
        }
        try {
            ThreadDumperPanel tdp = new ThreadDumperPanel();
            tdp.activate(bundleContext);

            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put("felix.webconsole.label", tdp.getLabel());

            register(bundleContext, new String[] { "javax.servlet.Servlet",
                "org.apache.felix.webconsole.ConfigurationPrinter" }, tdp,
                properties);
        } catch (Throwable t) {
            // shell service might not be available, don't care
        }
    }

    public void stop(BundleContext bundleContext) {
    }

    private void register(BundleContext context, String[] serviceNames,
            Object service, Dictionary<String, Object> properties) {

        // ensure properties
        if (properties == null) {
            properties = new Hashtable<String, Object>();
        }

        // default settings
        properties.put(Constants.SERVICE_DESCRIPTION, "Thread Dumper ("
            + serviceNames[0] + ")");
        properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");

            context.registerService(serviceNames, service, properties);
    }
}
