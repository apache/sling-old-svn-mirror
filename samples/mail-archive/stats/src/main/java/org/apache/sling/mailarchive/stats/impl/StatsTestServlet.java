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
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.james.mime4j.dom.Message;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.mailarchive.stats.MailStatsProcessor;
import org.apache.sling.mailarchiveserver.api.MboxParser;

@SuppressWarnings("serial")
@SlingServlet(
        resourceTypes = "mailarchiveserver/import",
        methods = {"POST", "PUT"},
        selectors="stats")
/** Test the stats import with
 *   curl -u admin:admin -XPOST -Fmboxfile=@jackrabbit-dev-201201.mbox http://localhost:8080/content/mailarchiveserver/import.stats.txt
 */
public class StatsTestServlet extends SlingAllMethodsServlet {

    private static final String IMPORT_FILE_ATTRIB_NAME = "mboxfile";

    @Reference
    private MboxParser parser;
    
    @Reference
    private MailStatsProcessor processor;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        final RequestParameter param = request.getRequestParameter(IMPORT_FILE_ATTRIB_NAME);
        if(param == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter " + IMPORT_FILE_ATTRIB_NAME);
            return;
        }
        
        InputStream is = null;
        final PrintWriter pw = response.getWriter();
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        
        try {
            is = param.getInputStream();
            pw.println("Creating stats from supplied mbox file...");
            int counter=0;
            final Iterator<Message> it = parser.parse(is);
            while(it.hasNext()) {
                final Message m = it.next();
                final String [] to = MailStatsProcessorImpl.toArray(m.getTo());
                final String [] cc = MailStatsProcessorImpl.toArray(m.getCc());
                for(String from : MailStatsProcessorImpl.toArray(m.getFrom())) {
                    processor.computeStats(m.getDate(), from.toString(), to, cc);
                }
                counter++;
            }
            pw.println(counter + " messages parsed");
        } finally {
            processor.flush();
            pw.flush();
            if(is != null) {
                is.close();
            }
        }
    }
}