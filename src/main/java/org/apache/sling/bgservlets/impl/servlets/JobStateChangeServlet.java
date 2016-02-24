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
package org.apache.sling.bgservlets.impl.servlets;

import java.io.IOException;

import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.bgservlets.BackgroundServletConstants;
import org.apache.sling.bgservlets.JobConsole;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/** Handles POST to a job node, to request a job
 *  state change.
 */
@Component
@Service
@Properties({
    @Property(name = "sling.servlet.resourceTypes", value = BackgroundServletConstants.JOB_RESOURCE_TYPE),
    @Property(name = "sling.servlet.methods", value="POST")
})
@SuppressWarnings("serial")
public class JobStateChangeServlet extends SlingAllMethodsServlet {

    public static final String PARAM_STATE = "desiredState";
    
    @Reference
    private JobConsole jobConsole;
    
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        final String str = request.getParameter(PARAM_STATE);
        if(str == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter:" + PARAM_STATE);
        }
        JobStatus.State desiredState = JobStatus.State.NEW;
        try {
            desiredState = JobStatus.State.valueOf(str);
        } catch(Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + PARAM_STATE + ": " + str);
            return;
        }
        
        final Session session = request.getResourceResolver().adaptTo(Session.class);
        if(session == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ResourceResolver does not adapt to a Session");
            return;
        }
        final JobStatus j = jobConsole.getJobStatus(session, request.getResource().getPath());
        final JobStatus.State oldState = j.getState();
        j.requestStateChange(desiredState);
        final JobStatus.State newState = j.getState();
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        final JSONWriter w = new JSONWriter(response.getWriter());
        try {
            w.object();
            w.key("info").value("Requested state change");
            w.key(PARAM_STATE).value(desiredState.toString());
            w.key("path").value(j.getPath());
            w.key("currentState").value(newState);
            w.key("stateChanged").value(newState != oldState);
            w.endObject();
        } catch(JSONException je) {
            throw new ServletException("JSONException in doPost", je);
        }
    }
}
