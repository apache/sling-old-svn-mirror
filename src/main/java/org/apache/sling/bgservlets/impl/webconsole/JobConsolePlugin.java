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
package org.apache.sling.bgservlets.impl.webconsole;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.bgservlets.JobConsole;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Felix OSGi console plugin for the ExecutionEngine */
public class JobConsolePlugin {
    private static final Logger log = LoggerFactory.getLogger(JobConsolePlugin.class);
    private static Plugin plugin;
    public static final String LABEL = "bgservlets";
    public static final String TITLE = "Background Servlets & Jobs";
    public static final String STATUS_EXTENSION = "html";

    public static void initPlugin(BundleContext context, JobConsole jobConsole) {
        if (plugin == null) {
            Plugin tmp = new Plugin(jobConsole);
            tmp.activate(context);
            plugin = tmp;
            log.info("{} activated", plugin);
        }
    }

    public static void destroyPlugin() {
        if (plugin != null) {
            try {
                plugin.deactivate();
                log.info("{} deactivated", plugin);
            } finally {
                plugin = null;
            }
        }
    }

    @SuppressWarnings("serial")
    public static final class Plugin extends AbstractWebConsolePlugin {
        private ServiceRegistration serviceRegistration;
        private final JobConsole jobConsole;
        private ServiceTracker repositoryTracker;
        private ServiceTracker resourceResolverFactoryTracker;

        public Plugin(JobConsole console) {
            jobConsole = console;
        }
        
        public void activate(BundleContext ctx) {
            super.activate(ctx);

            repositoryTracker = new ServiceTracker(ctx, SlingRepository.class.getName(), null);
            repositoryTracker.open();
            
            resourceResolverFactoryTracker = new ServiceTracker(ctx, ResourceResolverFactory.class.getName(), null);
            resourceResolverFactoryTracker.open();

            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION,
                    "Web Console Plugin to display Background servlets and ExecutionEngine status");
            props.put(Constants.SERVICE_VENDOR,
                    "The Apache Software Foundation");
            props.put(Constants.SERVICE_PID, getClass().getName());
            props.put(WebConsoleConstants.PLUGIN_LABEL, LABEL);

            serviceRegistration = ctx.registerService(WebConsoleConstants.SERVICE_NAME, this, props);
        }

        public void deactivate() {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
            if (repositoryTracker != null) {
                repositoryTracker.close();
                repositoryTracker = null;
            }
            if (resourceResolverFactoryTracker != null) {
                resourceResolverFactoryTracker.close();
                resourceResolverFactoryTracker = null;
            }
            super.deactivate();
        }

        @Override
        public String getLabel() {
            return LABEL;
        }

        @Override
        public String getTitle() {
            return TITLE;
        }
        
        /** Return the JCR session of the current request's user */
        private Session getRequestSession() throws ServletException {
            Session result = null;
            final ResourceResolverFactory f = (ResourceResolverFactory)resourceResolverFactoryTracker.getService();
            if(f == null) {
                throw new ServletException("Unable to acquire ResourceResolverFactory service");
            }
        
            final ResourceResolver r = f.getThreadResourceResolver();
            if(r == null) {
                throw new ServletException(
                        "Unable to acquire ResourceResolver from ResourceResolverFactory service. "
                        + "This usually happens if the webconsole does not use the Sling Security Provider."
                );
            }
            
            result = r.adaptTo(Session.class);
        
            if(result == null) {
                throw new ServletException("ResourceResolver does not adapt to Session");
            }
            return result;
        }

        @Override
        protected void renderContent(HttpServletRequest req,
                HttpServletResponse res) throws ServletException, IOException {
            final PrintWriter pw = res.getWriter();
            
            // Access required services
            final SlingRepository repository = (SlingRepository)repositoryTracker.getService();
            if(repository == null) {
                pw.println("No SlingRepository service found");
                return;
            }
            renderJobs(req, pw, getRequestSession(), jobConsole);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            final PrintWriter pw = resp.getWriter();

            // Access required services
            final SlingRepository repository = (SlingRepository)repositoryTracker.getService();
            if(repository == null) {
                pw.println("No SlingRepository service found");
                return;
            }
            final Session s = getRequestSession();
            final String jobPath = req.getParameter("jobPath");
            if (jobPath != null) {
                final JobStatus job = jobConsole.getJobStatus(s, jobPath);
                if (job != null) {
                    final String action = req.getParameter("action");
                    if ("suspend".equals(action)) {
                        job.requestStateChange(JobStatus.State.SUSPENDED);
                    } else if ("stop".equals(action)) {
                        job.requestStateChange(JobStatus.State.STOPPED);
                    } else if ("resume".equals(action)) {
                        job.requestStateChange(JobStatus.State.RUNNING);
                    }
                }
            }

            resp.sendRedirect(req.getServletPath() + req.getPathInfo());
        }
        
        private void renderJobs(HttpServletRequest req, PrintWriter pw, Session s, JobConsole console) {
            pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
            pw.println("<thead>");
            pw.println("<tr class='content'>");
            pw.println("<th class='content container'>Controls</th>");
            pw.println("<th class='content container'>State</th>");
            pw.println("<th class='content container'>ETA</th>");
            pw.println("<th class='content container'>Progress</th>");
            pw.println("<th class='content container'>Path</th>");
            pw.println("</tr>");
            pw.println("</thead>");
            pw.println("<tbody>");

            final int maxJobsDisplayed = 100;
            boolean truncated = false;
            final boolean activeOnly = false;
            final Iterator<JobStatus> it = console.getJobStatus(s, activeOnly);
            int count = 0;
            while (it.hasNext()) {
                renderJobStatus(req, pw, console, it.next());
                count++;
                if(count > maxJobsDisplayed) {
                    truncated = true;
                    break;
                }
            }
            pw.println("</tbody>");
            pw.println("</table>");
            pw.println("Total <b>" + count + "</b> jobs.<br />");
            if(truncated) {
                pw.println("(List truncated after " + maxJobsDisplayed + " jobs)<br />");
            }
        }

        private void renderJobStatus(HttpServletRequest request, PrintWriter pw, JobConsole console, JobStatus job) {
            pw.println("<tr class='content'>");
            pw.println("<td><form action='./" + LABEL + "' method='POST'>");
            final String[] actions = { "suspend", "resume", "stop" };
            for (String action : actions) {
                pw.println("<input type='submit' name='action' value='"
                        + action + "'/>&nbsp;");
            }
            pw.println("<input type='hidden' name='jobPath' value='"
                    + job.getPath() + "'/>&nbsp;");
            pw.println("</form></td>");
            pw.println("<td>");
            pw.println(escape(job.getState().toString()));
            pw.println("</td>");
            pw.println("<td>");
            final Date eta = job.getProgressInfo().getEstimatedCompletionTime();
            pw.println(eta == null ? "-" : eta.toString());
            pw.println("</td>");
            pw.println("<td>");
            pw.println(escape(job.getProgressInfo().getProgressMessage()));
            pw.println("</td>");
            pw.print("<td>\n<a href='");
            pw.print(escape(console.getJobStatusPagePath(request, job, STATUS_EXTENSION)));
            pw.print("'>");
            pw.print(escape(job.getPath()));
            pw.println("</a>");
            pw.println("</td>");
            pw.println("</tr>");
        }
        
        static String escape(String str) {
            if(str == null) {
                return null;
            }
            final StringBuilder sb = new StringBuilder();
            for(int i=0; i < str.length(); i++) {
                final char c = str.charAt(i);
                if(c == '<') {
                    sb.append("&lt;");
                } else if(c== '>') {
                    sb.append("&gt;");
                } else if(c == '&') {
                    sb.append("&amp;");
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

    }
}