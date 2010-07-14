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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.bgservlets.ExecutionEngine;
import org.apache.sling.bgservlets.JobStatus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Felix OSGi console plugin for the ExecutionEngine */
public class ExecutionEngineConsolePlugin {
	private static final Logger log = LoggerFactory.getLogger(ExecutionEngineConsolePlugin.class);
	private static Plugin plugin;
    public static final String LABEL = "bgservlets";
    public static final String TITLE = "Background Servlets & Jobs";
	
    public static void initPlugin(BundleContext context) {
        if (plugin == null) {
            Plugin tmp = new Plugin();
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
        private ServiceTracker executionEngineTracker;
        
    	public void activate(BundleContext ctx) {
            super.activate(ctx);
            
            executionEngineTracker = new ServiceTracker(ctx, ExecutionEngine.class.getName(), null);
            executionEngineTracker.open();
            
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION,
                "Web Console Plugin to display Background servlets and ExecutionEngine status");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            props.put(Constants.SERVICE_PID, getClass().getName());
            props.put(WebConsoleConstants.PLUGIN_LABEL, LABEL);

            serviceRegistration = ctx.registerService(WebConsoleConstants.SERVICE_NAME, this, props);
    	}
    	public void deactivate() {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
    		if(executionEngineTracker != null) {
    			executionEngineTracker.close();
    			executionEngineTracker = null;
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
        
        @Override
        protected void renderContent(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException {
        	final PrintWriter pw = res.getWriter();
        	final ExecutionEngine ee = (ExecutionEngine)executionEngineTracker.getService();
        	if(ee == null) {
        		pw.println("No ExecutionEngine service found");
        		return;
        	}
        	
        	// TODO should use POST
    		final String jobPath = req.getParameter("jobPath");
    		if(jobPath != null) {
        		final JobStatus job = ee.getJobStatus(jobPath);
        		if(job != null) {
            		final String action = req.getParameter("action");
            		if("suspend".equals(action)) {
            			job.requestStateChange(JobStatus.State.SUSPENDED);
            		} else if("stop".equals(action)) {
            			job.requestStateChange(JobStatus.State.STOPPED);
            		} else if("resume".equals(action)) {
            			job.requestStateChange(JobStatus.State.RUNNING);
            		}
        		}
    		}
    		
    		pw.println("TODO: provide a way to cleanup old jobs<br/>");
    		pw.println("TODO: optionally list active jobs only<br/>");
  
            pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
            pw.println("<thead>");
            pw.println("<tr class='content'>");
            pw.println("<th class='content container'>Controls</th>");
            pw.println("<th class='content container'>State</th>");
            pw.println("<th class='content container'>Path</th>");
            pw.println("</tr>");
            pw.println("</thead>");
            pw.println("<tbody>");

        	final Iterator<JobStatus> it = ee.getMatchingJobStatus(null);
        	int count = 0;
        	while(it.hasNext()) {
        		renderJobStatus(pw, it.next());
        		count++;
        	}
            pw.println("</tbody>");
        	pw.println("</table>");
        	pw.println("Total <b>" + count + "</b> jobs.<br />");
        }
        
        private void renderJobStatus(PrintWriter pw, JobStatus job) {
        	// TODO should use POST
            pw.println("<tr class='content'>");
        	pw.println("<td><form action='./" + LABEL + "' method='GET'>");
        	final String [] actions = { "suspend", "resume", "stop" };
        	for(String action : actions) {
        		pw.println("<input type='submit' name='action' value='" + action + "'/>&nbsp;");
        	}
        	pw.println("<input type='hidden' name='jobPath' value='" + job.getPath() + "'/>&nbsp;");
        	pw.println("</form></td>");
        	pw.println("<td>");
        	pw.println(job.getState());
        	pw.println("</td>");
        	pw.println("<td>");
        	pw.println(job.getPath());
        	pw.println("</td>");
        	pw.println("</tr>");
        }
    
    }
}