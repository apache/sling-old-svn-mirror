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
package org.apache.sling.jcr.jcrinstall.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jcrinstall.jcr.JcrInstallService;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Info and control servlet for jcrinstall.
 * 	Created for integration tests, but might be
 * 	generally useful for monitoring purposes.
 * 
 * @scr.component 
 *  label="jcrinstall servlet" 
 *  description="Information and control servlet for jcrinstall"
 *  immediate="true"
 *  @scr.service
 *  @scr.property 
 *      name="service.description" 
 *      value="Sling jcrinstall Servlet"
 *  @scr.property 
 *      name="service.vendor" 
 *      value="The Apache Software Foundation"
 *      
 * 	@scr.property name="sling.servlet.paths" value="/system/sling/jcrinstall"
 */
@SuppressWarnings("serial")
public class JcrInstallServlet extends SlingAllMethodsServlet {
	
    public static final String POST_ENABLE_PARAM = "enabled";
    
    /** @scr.reference */
    protected StartLevel startLevel;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
	private ComponentContext componentContext;
	
    protected void activate(ComponentContext context) {
    	componentContext = context;
    }

    protected void deactivate(ComponentContext context) {
    	componentContext = null;
    }

    protected JcrInstallService getJcrinstallService() {
    	JcrInstallService result = null;
    	if(componentContext != null) {
    		final ServiceReference ref = componentContext.getBundleContext().getServiceReference(JcrInstallService.class.getName());
    		if(ref != null) {
    			result = (JcrInstallService)componentContext.getBundleContext().getService(ref);
    		}
    	}
    	return result;
    }
    
    /** A POST can be used to deactivate/reactivate this, simulating a disappearing SlingRepository.
     *  Used for integration testing.
     */
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        final String enable = request.getParameter(POST_ENABLE_PARAM);
        if(enable != null) {
        	final JcrInstallService jis = getJcrinstallService();
        	if(jis == null) {
        		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JcrInstallService is not available");
        		
        	} else if(Boolean.parseBoolean(enable)) {
                log.info("Processing POST with {}=true, enabling JcrInstallService", POST_ENABLE_PARAM);
                jis.enable(true);
                
            } else {
                log.info("Processing POST with {}=false, disabling JcrInstallService", POST_ENABLE_PARAM);
                jis.enable(false);
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Use '" + POST_ENABLE_PARAM + "' parameter to enable/disable the RepositoryObserver");
            return;
        }
        
        doGet(request, response);
    }

    /** Report on the jcrinstall enabled/disabled status, number of bundles in each state, etc. */ 
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException 
    {
    	final Properties props = new Properties();
    	
    	if(componentContext != null) {
    		// report on how many bundles we have in the different states
    		final Map<Integer, Integer> states = new HashMap<Integer, Integer>();
    		for(Bundle b : componentContext.getBundleContext().getBundles()) {
    			final Integer s = new Integer(b.getState());
    			Integer i = states.get(s);
    			i = i == null ? new Integer(1) : new Integer(i.intValue() + 1);
    			states.put(s, i);
    		}
    		
    		for(Map.Entry<Integer, Integer> e : states.entrySet()) {
    			props.put("bundles.in.state." + e.getKey().toString(), e.getValue().toString());
    		}
    	}
  
    	props.put("osgi.start.level", String.valueOf(startLevel.getStartLevel()));
    	final JcrInstallService jis = getJcrinstallService();
    	final boolean jisState = jis == null ? false : jis.isEnabled();
    	props.put("jcrinstall.enabled", new Boolean(jisState).toString());
        
        response.setContentType("text/plain");
        props.store(response.getOutputStream(), "jcrinstall status");
    }
}