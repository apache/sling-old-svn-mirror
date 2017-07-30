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
package org.apache.sling.mailarchive.stats.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.felix.utils.json.JSONWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

@SuppressWarnings("serial")
@SlingServlet(
        resourceTypes = "mailserver/stats/destination",
        methods = "GET",
        extensions="json",
        selectors="statsdata")
/** Generates json data for the destination.esp script.
 * 
 *  Produces output like 
 *  <pre>
 *  
 *  var statsData = [
 *      { "period": "2012/01",
 *        "senders" : {
 *          "netenviron.com" : 1,
 *          "builds.apache.org" : 12
 *      }},
 *      { "period": "2013/09",
 *        "senders" : {
 *          "yahoo.com" : 32,
 *          "netenviron.com" : 6,
 *          "builds.apache.org" : 9
 *      }}
 *  ];
 *  var layers = [
 *      "yahoo.com",
 *      "netenviron.com",
 *      "builds.apache.org"
 *  ];
 *  
 *  </pre>
 *  
 */
public class StatsDataServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setCharacterEncoding("UTF-8");
        final PrintWriter out = response.getWriter();
        
        try {
            out.write("// data provided by " + getClass().getName() + "\n");
            final SortedSet<String> layers = new TreeSet<String>();
            
            // Visit our child resources and build the statsData object
            // from those that have the stats data resource type
            {
                final JSONWriter w = new JSONWriter(response.getWriter());
                out.write("var statsData = ");
                w.array();
                dumpStatsData(request.getResource(), w, layers);
                w.endArray();
                out.flush();
                out.write(";\n");
            }
            
            // Output the layers array in JSON
            {
                final JSONWriter w = new JSONWriter(response.getWriter());
                out.write("var layers = ");
                w.array();
                for(String layer : layers) {
                    w.value(layer);
                };
                w.endArray();
                out.write(";");
            }
            
        } catch(IOException je) {
            throw new ServletException("JSONException in doGet", je);
        }
    }
    
    /** Dump stats data to w if r is a stats data resource,
     *  and recurse into children
     */
    private void dumpStatsData(Resource r, JSONWriter w, Set<String> layers) throws IOException {
        if(MailStatsProcessorImpl.DATA_RESOURCE_TYPE.equals(r.getResourceType())) {
            final ValueMap m = r.adaptTo(ValueMap.class);
            if(m != null) {
                w.object();
                w.key("period").value(m.get(MailStatsProcessorImpl.PERIOD_PROP, "NO_PERIOD"));
                w.key("senders");
                w.object();
                for(String key : m.keySet()) {
                    if(key.startsWith(MailStatsProcessorImpl.SOURCE_PROP_PREFIX)) {
                        final String source = key.substring(MailStatsProcessorImpl.SOURCE_PROP_PREFIX.length());
                        layers.add(source);
                        w.key(source).value(m.get(key));
                    }
                }
                w.endObject();
                w.endObject();
            }
        }
        
        for(Resource child : r.getChildren()) {
            dumpStatsData(child, w, layers);
        }
    }
}