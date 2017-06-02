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
package org.apache.sling.extensions.mdc.integration.servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.MDC;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

public class MDCStateServlet extends HttpServlet implements BundleActivator{
    private ServiceTracker configAdminTracker;

    public void start(BundleContext context) throws Exception {
        Properties p  = new Properties();
        p.setProperty("alias","/mdc");
        context.registerService(Servlet.class.getName(),this,p);
        configAdminTracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(),null);
        configAdminTracker.open();
    }

    public void stop(BundleContext context) throws Exception {

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();

        if(req.getParameter("createTestConfig") != null){
            createTestConfig();
            pw.print("created");
            return;
        }

        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        JsonGenerator json = Json.createGenerator(pw);
        json.writeStartObject();
        if (copyOfContextMap != null) {
            for (Entry<String, String> entry : copyOfContextMap.entrySet())
            {
                json.write(entry.getKey(), entry.getValue());
            }
        }
        json.writeEnd();
        json.flush();
    }

    private void createTestConfig() throws IOException {
        ConfigurationAdmin ca = (ConfigurationAdmin) configAdminTracker.getService();
        Configuration cfg = ca.getConfiguration("org.apache.sling.extensions.mdc.internal.MDCInsertingFilter",null);

        Dictionary<String,Object> dict = new Hashtable<String, Object>();
        dict.put("headers",new String[]{"mdc-test-header"});
        dict.put("parameters",new String[]{"mdc-test-param"});
        dict.put("cookies",new String[]{"mdc-test-cookie"});
        cfg.update(dict);
    }
}
