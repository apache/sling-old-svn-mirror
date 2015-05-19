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
package org.apache.sling.crankstart.testservices;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/** Dump configs, for testing
 */
@Component(immediate=true,metatype=true)
@Service(value=Servlet.class)
@Reference(name="httpService",referenceInterface=HttpService.class)
public class ConfigDumpServlet extends TestServlet {
    private static final long serialVersionUID = -6918378772515948581L;
    
    @Reference
    private ConfigurationAdmin configAdmin;

    @Activate
    protected void activate(Map<String, Object> config) throws ServletException, NamespaceException {
        message = "no message yet";
        path = "/test/config";
        register();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String configPid = req.getPathInfo().substring(1);
        final Configuration cfg = configAdmin.getConfiguration(configPid);
        
        final SortedSet<String> keys = new TreeSet<String>();
        final Enumeration<?> e = cfg.getProperties().keys();
        while(e.hasMoreElements()) {
            keys.add(e.nextElement().toString());
        }
        final StringBuilder b = new StringBuilder();
        b.append(configPid).append("#");
        for(String key : keys) {
            final Object value = cfg.getProperties().get(key);
            b.append(key)
            .append("=(")
            .append(value.getClass().getSimpleName())
            .append(")")
            .append(prettyprint(value))
            .append("#")
            ;
        }
        b.append("#EOC#");
        
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(b.toString());
        resp.getWriter().flush();
    }
    
    private static String prettyprint(Object value) {
        if(value instanceof String []) {
            return Arrays.asList((String[])value).toString();
        } else {
            return value.toString();
        }
    }
}
