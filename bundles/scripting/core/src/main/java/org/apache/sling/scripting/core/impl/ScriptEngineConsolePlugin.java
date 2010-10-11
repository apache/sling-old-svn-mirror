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
package org.apache.sling.scripting.core.impl;

import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class ScriptEngineConsolePlugin {

    // --------- setup and shutdown

    private static ScriptEngineConsolePlugin INSTANCE;

    static void initPlugin(BundleContext context,
            ScriptEngineManagerFactory scriptEngineManagerFactory) {
        if (INSTANCE == null) {
            ScriptEngineConsolePlugin tmp = new ScriptEngineConsolePlugin(
                    scriptEngineManagerFactory);
            tmp.activate(context);
            INSTANCE = tmp;
        }
    }

    static void destroyPlugin() {
        if (INSTANCE != null) {
            try {
                INSTANCE.deactivate();
            } finally {
                INSTANCE = null;
            }
        }
    }

    private ServiceRegistration serviceRegistration;

    private final ScriptEngineManagerFactory scriptEngineManagerFactory;

    // private constructor to force using static setup and shutdown
    private ScriptEngineConsolePlugin(
            ScriptEngineManagerFactory scriptEngineManagerFactory) {
        this.scriptEngineManagerFactory = scriptEngineManagerFactory;
    }

    public String getTitle() {
        return "Script Engines";
    }

    public void printConfiguration(final PrintWriter pw) {
        pw.println("Available Script Engines");
        pw.println("========================");

        ScriptEngineManager manager = scriptEngineManagerFactory.getScriptEngineManager();
        List<?> factories = manager.getEngineFactories();
        for (Iterator<?> fi = factories.iterator(); fi.hasNext();) {

            final ScriptEngineFactory factory = (ScriptEngineFactory) fi.next();

            pw.println();
            pw.print(factory.getEngineName());
            pw.print(" ");
            pw.println(factory.getEngineVersion());
            pw.println("-------------------------------------");
            pw.print("- Language : ");
            pw.print(factory.getLanguageName());
            pw.print(", ");
            pw.println(factory.getLanguageVersion());

            pw.print("- Extensions : ");
            printArray(pw, factory.getExtensions());

            pw.print("- MIME Types : ");
            printArray(pw, factory.getMimeTypes());

            pw.print("- Names : ");
            printArray(pw, factory.getNames());
        }
    }

    private void printArray(PrintWriter pw, List<?> values) {
        if (values == null || values.size() == 0) {
            pw.println("-");
        } else {
            for (Iterator<?> vi = values.iterator(); vi.hasNext();) {
                pw.print(vi.next());
                if (vi.hasNext()) {
                    pw.print(", ");
                }
            }
            pw.println();
        }
    }

    public void activate(BundleContext context) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Web Console Plugin for ScriptEngine implementations");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_PID, getClass().getName());

        props.put("felix.webconsole.label", "slingscripting");
        props.put("felix.webconsole.title", "Script Engines");
        props.put("felix.webconsole.configprinter.modes", "always");

        serviceRegistration = context.registerService(
            this.getClass().getName(), this, props);
    }

    public void deactivate() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
