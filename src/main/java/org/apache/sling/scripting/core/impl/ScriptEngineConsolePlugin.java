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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class ScriptEngineConsolePlugin extends HttpServlet {

    private static final String LABEL = "scriptengines";

    // --------- setup and shutdown

    private static ScriptEngineConsolePlugin INSTANCE;

    static void initPlugin(BundleContext context,
            SlingScriptAdapterFactory scriptAdapterFactory) {
        if (INSTANCE == null) {
            ScriptEngineConsolePlugin tmp = new ScriptEngineConsolePlugin(
                scriptAdapterFactory);
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

    private final SlingScriptAdapterFactory scriptAdapterFactory;

    // private constructor to force using static setup and shutdown
    private ScriptEngineConsolePlugin(
            SlingScriptAdapterFactory scriptAdapterFactory) {
        this.scriptAdapterFactory = scriptAdapterFactory;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        PrintWriter pw = res.getWriter();

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        ScriptEngineManager manager = scriptAdapterFactory.getScriptEngineManager();
        List<?> factories = manager.getEngineFactories();
        for (Iterator<?> fi = factories.iterator(); fi.hasNext();) {

            ScriptEngineFactory factory = (ScriptEngineFactory) fi.next();

            pw.println("<tr class='content'>");
            pw.println("<th colspan='3'class='content container'>");
            pw.print(factory.getEngineName());
            pw.print(", ");
            pw.println(factory.getEngineVersion());
            pw.println("</th>");
            pw.println("</tr>");

            pw.println("<tr class='content'>");
            pw.println("<td class='content'>&nbsp;</td>");
            pw.println("<td class='content'>Language</td>");
            pw.println("<td class='content'>");
            pw.print(factory.getLanguageName());
            pw.print(", ");
            pw.println(factory.getLanguageVersion());
            pw.println("</td>");
            pw.println("</tr>");

            pw.println("<tr class='content'>");
            pw.println("<td class='content'>&nbsp;</td>");
            pw.println("<td class='content'>Extensions</td>");
            pw.println("<td class='content'>");
            printArray(pw, factory.getExtensions());
            pw.println("</td>");
            pw.println("</tr>");

            pw.println("<tr class='content'>");
            pw.println("<td class='content'>&nbsp;</td>");
            pw.println("<td class='content'>MIME Types</td>");
            pw.println("<td class='content'>");
            printArray(pw, factory.getMimeTypes());
            pw.println("</td>");
            pw.println("</tr>");

            pw.println("<tr class='content'>");
            pw.println("<td class='content'>&nbsp;</td>");
            pw.println("<td class='content'>Names</td>");
            pw.println("<td class='content'>");
            printArray(pw, factory.getNames());
            pw.println("</td>");
            pw.println("</tr>");

        }

        pw.println("</table>");
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
        props.put("felix.webconsole.label", LABEL);
        props.put("felix.webconsole.title", "Script Engines");

        serviceRegistration = context.registerService(
            "javax.servlet.Servlet", this, props);
    }

    public void deactivate() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
