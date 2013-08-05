/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.HealthCheckSelector;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Webconsole plugin to execute health check services */ 
@Component(immediate=true)
@Service(Servlet.class)
@SuppressWarnings("serial")
@Properties({
    @Property(name=Constants.SERVICE_DESCRIPTION, value="Sling Health Check Web Console Plugin"),
    @Property(name=Constants.SERVICE_VENDOR, value="The Apache Software Foundation"),
    @Property(name="felix.webconsole.label", value=HealthCheckWebconsolePlugin.LABEL),
    @Property(name="felix.webconsole.title", value=HealthCheckWebconsolePlugin.TITLE),
    @Property(name="felix.webconsole.category", value=HealthCheckWebconsolePlugin.CATEGORY),
    @Property(name="felix.webconsole.css", value="/healthcheck/res/ui/healthcheck.css")
})
public class HealthCheckWebconsolePlugin extends HttpServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String TITLE = "Sling Health Check";
    public static final String LABEL = "healthcheck";
    public static final String CATEGORY = "Sling";
    public static final String PARAM_TAGS = "tags";
    
    @Reference
    private HealthCheckSelector selector;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Static resource?
        final String pathInfo = req.getPathInfo();
        if(pathInfo!= null && pathInfo.contains("res/ui")) {
            final String prefix = "/" + LABEL;
            final InputStream is = getClass().getResourceAsStream(pathInfo.substring(prefix.length()));
            if(is == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, pathInfo);
            }
            final byte [] buffer = new byte[16384];
            int n=0;
            while( (n = is.read(buffer, 0, buffer.length)) > 0) {
                resp.getOutputStream().write(buffer, 0, n); 
            }
            resp.getOutputStream().flush();
            return;
        }
        
        final String tags = getParam(req, PARAM_TAGS, "");
        
        doForm(req, resp, tags);
        
        final List<HealthCheck> checks = selector.getTaggedHealthCheck(tags.split(","));
        final PrintWriter pw = resp.getWriter();
        pw.println("<table class='content healthcheck' cellpadding='0' cellspacing='0' width='100%'>");
        for(HealthCheck hc : checks) {
            final ResultLog rl = new ResultLog(log);
            renderResult(resp, hc.execute(rl));
        }
        pw.println("</table>");
    }
    
    private void renderResult(HttpServletResponse resp, Result result) throws IOException {
        final WebConsoleHelper c = new WebConsoleHelper(resp.getWriter());

        c.tr();
        c.tdLabel(ResponseUtil.escapeXml(result.getHealthCheck().toString()));
        c.closeTd();
        
        // TODO tags and info
        // dataRow(c, "Tags", ResponseUtil.escapeXml(r.getRule().getTags().toString()));
            
        c.tdContent();
        for(ResultLog.Entry e : result.getLogEntries()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("<div class='log").append(e.getLevel().toString()).append("'>");
            sb.append(e.getLevel().toString())
                .append(" ")
                .append(ResponseUtil.escapeXml(e.getMessage()))
                .append("</div>");
            c.writer().println(sb.toString());
        }
        c.closeTd();
    }
    
    private void doForm(HttpServletRequest req, HttpServletResponse resp, String tags) throws IOException {
        final PrintWriter pw = resp.getWriter();
        final WebConsoleHelper c = new WebConsoleHelper(pw);
        pw.print("<form method='get'>");
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        c.titleHtml(TITLE, "To execute health check services, enter "
                + " an optional list of tags, to select specific health checks, or no tags for all checks.");
        
        c.tr(); 
        c.tdLabel("Rule tags (comma-separated)");
        c.tdContent();
        pw.println("<input type='text' name='" + PARAM_TAGS + "' value='" + tags + "' class='input' size='80'>");
        c.closeTd(); 
        c.closeTr();
        
        c.tr(); 
        c.tdContent();
        pw.println("<input type='submit' value='Execute selected health checks'/>");
        c.closeTd(); 
        c.closeTr();
        
        pw.println("</table></form>");
    }

    private String getParam(HttpServletRequest req, String name, String defaultValue) {
        String result = req.getParameter(name);
        if(result == null) {
            result = defaultValue;
        }
        return result;
    }
}
