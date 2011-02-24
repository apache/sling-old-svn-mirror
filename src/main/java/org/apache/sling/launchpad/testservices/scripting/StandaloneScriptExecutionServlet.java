/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.testservices.scripting;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * Test Servlet that executes a named script in standalone mode, i.e.
 * without a request or response. 
 */

@Component(immediate=true, metatype=false)
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name="service.description", value="StandaloneScriptExecutionServlet Test Servlet"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="sling.servlet.resourceTypes", value="sling/servlet/default"),
    @Property(name="sling.servlet.selectors", value = "StandaloneScriptExecutionServlet"),
    @Property(name="sling.servlet.extensions",value = "txt")
})
@SuppressWarnings("serial")
public class StandaloneScriptExecutionServlet extends SlingSafeMethodsServlet {

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
	throws ServletException, IOException {
		final SlingScript script = request.getResource().adaptTo(SlingScript.class);
		if(script == null) {
			throw new ServletException("Resource does not adapt to a SlingScript:" + request.getResource().getPath());
		}
		
		// Execute the script without providing a request or response, in the simplest possible way
		final SlingBindings bindings = new SlingBindings();
		final StringWriter sw = new StringWriter();
		bindings.put("StandaloneScriptExecutionServletOutput", sw);
		script.eval(bindings);
		
		response.setContentType("text/plain");
		response.getWriter().write(sw.toString());
	}
}
