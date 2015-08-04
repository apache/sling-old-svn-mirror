
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
package org.apache.sling.reseditor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streams the content of the property specified by the request parameter
 * 'property' to the response of the request.
 */
@Component
@Service(Servlet.class)
@Properties({
		@Property(name = "service.description", value = "List the Resource Types available in the repository."),
		@Property(name = "service.vendor", value = "The Apache Software Foundation"),
		@Property(name = "sling.servlet.extensions", value = "json"),
		@Property(name = "sling.servlet.resourceTypes", value = "sling/resource-editor/resource-type-list")

})
public class ResourceTypeList extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = -1L;

	/** default log */
	private final Logger log = LoggerFactory
			.getLogger(ResourceTypeList.class);

	@Override
	protected void doGet(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws ServletException,
			IOException {
		PrintWriter responseWriter = null;
		Set<String> resourceTypes = new HashSet<String>();
		try {
			Iterator<Resource> resources = request.getResourceResolver().findResources("//*[@sling:resourceType]", "xpath");
			while (resources.hasNext()) {
				Resource resource = (Resource) resources.next();
				String resourceTypeString = StringEscapeUtils.escapeHtml(resource.getResourceType());
				resourceTypeString = "\""+resourceTypeString+"\"";
				resourceTypes.add(resourceTypeString);
			}
			List<String> resourceTypeList = new LinkedList<String>(resourceTypes);
			Collections.sort(resourceTypeList);
			responseWriter = response.getWriter();
			responseWriter.write(resourceTypeList.toString());
		} finally {
			responseWriter.close();
		}

	}
}
