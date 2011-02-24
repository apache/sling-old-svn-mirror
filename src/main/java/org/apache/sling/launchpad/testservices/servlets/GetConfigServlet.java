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
package org.apache.sling.launchpad.testservices.servlets;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/** GET returns the contents of an OSGi config by PID */
@Component
@Service
@Properties({
    @Property(name="service.description", value="GetConfig Test Servlet"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="sling.servlet.paths",value="/testing/GetConfigServlet"),
    @Property(name="sling.servlet.extensions",value="json")
})
public class GetConfigServlet extends SlingSafeMethodsServlet {

    @Reference
    private ConfigurationAdmin configAdmin;

    @Override
    protected void doGet(SlingHttpServletRequest request,SlingHttpServletResponse response)
    throws ServletException,IOException {

        // PID comes from request suffix, like /testing/GetConfigServlet.tidy.json/integrationTestsConfig
        String pid = request.getRequestPathInfo().getSuffix();
        if(pid == null || pid.length() == 0) {
            throw new ServletException("Configuration PID must be provided in request suffix");
        }
        if(pid.startsWith("/")) {
            pid = pid.substring(1);
        }

        // Get config and properties
        final Configuration cfg = configAdmin.getConfiguration(pid);
        if(cfg == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Config with pid=" + pid + " not found");
        }
        final Dictionary<?, ?> props = cfg.getProperties();
        if(props == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Properties of config with pid=" + pid + " not found");
        }

        // Dump config in JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        final Enumeration<?> keys = props.keys();
        try {
            final JSONWriter w = new JSONWriter(response.getWriter());
            w.setTidy(Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy"));
            w.object();
            w.key("source").value(getClass().getName());
            w.key("pid").value(pid);
            w.key("properties");
            w.object();
            while(keys.hasMoreElements()) {
                final Object key = keys.nextElement();
                final Object value = props.get(key);
                if(value != null) {
                    w.key(key.toString()).value(value.toString());
                }
            }
            w.endObject();
            w.endObject();
        } catch(JSONException je) {
            throw (IOException)new IOException("JSONException in doGet").initCause(je);
        }
    }
}
