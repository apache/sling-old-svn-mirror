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
package org.apache.sling.settings.impl;

import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.shell.Command;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Run mode command for the shell.
 */
public class RunModeCommand implements Command {

    private static ServiceRegistration pluginReg;

    public static void initPlugin(final BundleContext bundleContext,
            final Set<String> modes) {
        final RunModeCommand command = new RunModeCommand(modes);

        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Sling Run Mode Shell Command");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        pluginReg = bundleContext.registerService(ConfigurationPrinter.class.getName(),
                command,
                props);
    }

    public static void destroyPlugin() {
        if ( pluginReg != null) {
            pluginReg.unregister();
            pluginReg = null;
        }
    }

    private static final String CMD_NAME = "runmodes";

    /** @scr.reference */
    private Set<String> modes;

    public RunModeCommand(final Set<String> modes) {
        this.modes = modes;
    }

    /**
     * @see org.apache.felix.shell.Command#getName()
     */
    public String getName() {
        return CMD_NAME;
    }

    /**
     * @see org.apache.felix.shell.Command#getShortDescription()
     */
    public String getShortDescription() {
        return "lists current run modes";
    }

    /**
     * @see org.apache.felix.shell.Command#getUsage()
     */
    public String getUsage() {
        return CMD_NAME;
    }

    /**
     * @see org.apache.felix.shell.Command#execute(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void execute(String command, PrintStream out, PrintStream err) {
        out.print("Current Run Modes: ");
        if (modes == null || modes.size() == 0) {
            out.println("-");
        } else {
            out.println(modes);
        }
    }
}
