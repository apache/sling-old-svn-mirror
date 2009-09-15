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
package org.apache.sling.osgi.event.recorder.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.osgi.event.recorder.OsgiEventsRecorder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/** Display recorded events as a simple graph on the Felix web console */
@SuppressWarnings("serial")
public class FelixConsolePlugin extends AbstractWebConsolePlugin {

	public static final String LABEL = "OSGiEventsRecorder";
	public static final String TITLE = "OSGi Events Recorder";
	
	private static FelixConsolePlugin instance;
	private ServiceRegistration serviceRegistration;
	private ServiceTracker osgiEventsRecorderTracker;
	private String lastClearString = "" + System.currentTimeMillis();
	
	private static final String S = "overflow:visible; white-space:nowrap; text-align:right; padding: 2px; margin: 2px; ";
	private static Map<String, String> styles = new HashMap<String, String>();
	static {
		styles.put(OsgiEventsRecorderImpl.ENTITY_BUNDLE, S + "background-color:#FFCACD; ");
		styles.put(OsgiEventsRecorderImpl.ENTITY_FRAMEWORK, S + "background-color:#DCDCDC; ");
		styles.put(OsgiEventsRecorderImpl.ENTITY_CONFIG, S + "background-color:#FFD700; ");
		styles.put(OsgiEventsRecorderImpl.ENTITY_SERVICE, S + "background-color:#ADFF2F; ");
	}

	public void activate(BundleContext ctx) {
		super.activate(ctx);
		
	    Dictionary<String, Object> props = new Hashtable<String, Object>();
	    props.put(Constants.SERVICE_DESCRIPTION, "Web Console Plugin to display/profile OSGi events");
	    props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
	    props.put(Constants.SERVICE_PID, getClass().getName());
	    props.put(WebConsoleConstants.PLUGIN_LABEL, LABEL);

	    serviceRegistration = ctx.registerService(WebConsoleConstants.SERVICE_NAME, this, props);
	    osgiEventsRecorderTracker = new ServiceTracker(ctx, OsgiEventsRecorder.class.getName(), null);
	    osgiEventsRecorderTracker.open();
	}
	
	public void deactivate() {
	    if(serviceRegistration != null) {
	        serviceRegistration.unregister();
	        serviceRegistration = null;
	    }
	    
	    if(osgiEventsRecorderTracker != null) {
	    	osgiEventsRecorderTracker.close();
	    }
		super.deactivate();
	}
	
	public static void initPlugin(BundleContext context) {
		if (instance == null) {
			FelixConsolePlugin tmp = new FelixConsolePlugin();
			tmp.activate(context);
			instance = tmp;
		}
	}

	public static void destroyPlugin() {
		if (instance != null) {
			try {
				instance.deactivate();
			} finally {
				instance = null;
			}
		}
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
	protected void renderContent(HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException {
		if(osgiEventsRecorderTracker == null) {
			throw new ServletException("ServiceTracker not found");
		}
		
		final OsgiEventsRecorder rec = (OsgiEventsRecorder)osgiEventsRecorderTracker.getService();
		if(rec == null) {
			throw new ServletException("OsgiEventsRecorder not found");
		}
		
		// Use unique numbers to clear, to avoid clearing again if the browser is refreshed
		final String clear = request.getParameter("clear");
		if(clear != null && !clear.equals(lastClearString)) {
			lastClearString = clear;
			rec.clear();
		}
		
		final Iterator<OsgiEventsRecorder.RecordedEvent> it = rec.getEvents();
		final PrintWriter w = response.getWriter();

		w.println("<div class='fullwidth'><div class='statusline'>");
		w.println("This plugin displays a timeline of OSGi events.");
		w.println("<br/>");
		w.println("Times in brackets are milliseconds since the first event was received.");
		w.println("<br/>");
		w.println("To profile system startup, install the recorder bundle with start level 1 to have it start early.");
		w.println("</div></div>");
		
		// Compute scale: startTime is 0, lastTimestamp is 100%
		final long startTime = rec.getStartupTimestamp();
		final long endTime = rec.getLastTimestamp();
		final float scale = 100.0f / (endTime - startTime);
		
		final String clearURL = "./" + LABEL + "?clear=" + System.currentTimeMillis();
		w.println("<h3>OSGi Events Timeline (<a href='" + clearURL + "'>Clear</a>)</h3>");
		w.println("<div class='fullwidth'>");
		if(rec.isActive()) {
			int count = 0;
			while(it.hasNext()) {
				count++;
				renderEvent(w, it.next(), startTime, scale);
			}
			w.println("</div>");
			w.println("<div class='fullwidth'><div class='statusline'>");
			w.println(count);
			w.println(" OSGi events displayed.");
			w.println("</div>");
		} else {
			w.println("<div class='fullwidth'><div class='statusline'>");
			w.println("OSGi Event Recorder is currently <b>disabled</b> by configuration.");
			w.println("</div>");
		}
	}
	
	private void renderEvent(PrintWriter w, OsgiEventsRecorder.RecordedEvent e, long start, float scale) {
		final long msec = e.timestamp - start;
		
		// Compute color bar size and make sure the bar is visible
		int percent = (int)((msec) * scale);
		percent = Math.max(percent, 2);
		
		// Get style according to entity
		String style = styles.get(e.entity);
		if(style == null) {
			style = "";
		}
		
		w.print("<div style='");
		w.print(style);
		w.print("width:");
		w.print(percent);
		w.print("%'>");
		if(e.id != null) {
			w.print("<b>");
			w.print(e.id);
			w.print("</b> ");
		}
		w.print(e.entity);
		w.print(" ");
		w.print(e.action);
		w.print(" <b>(");
		w.print(msec);
		w.println(")</b></div>");
	}
}
