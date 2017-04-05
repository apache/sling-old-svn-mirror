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
package org.apache.sling.jcr.js.nodetypes;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns all completely registered node types of the repository in JSON format. Nodetypes without names will not end up in the JSON file.
 * The binary default values of properties will not be part of the JSON file. They will have a String containing a URL. Use this URL to
 * download the binary content.
 *  
 * The URL is <code>/libs/jsnodetypes/content/nodetypes.json</code>.
 */
@Component
@Service(Servlet.class)
@Properties({ @Property(name = "service.description", value = "Returns the node types as a JSON file"),
		@Property(name = "service.vendor", value = "Sandro Boehme"),
		@Property(name = "sling.servlet.extensions", value = "json"),
		@Property(name = "sling.servlet.resourceTypes", value = "jsnodetypes")

})
public class NodeTypesJSONServlet extends SlingSafeMethodsServlet {
	/*
	 * In /src/main/resources/libs/jsnodetypes/content.json there is an automatically loaded node with a child node
	 * that has the resource type 'jsnodetypes'. This servlet can render this resource type and thus provides the JSON
	 * content at the URL as written in the JavaDoc.
	 */

	private static final long serialVersionUID = -1L;

	/** default log */
	private final Logger log = LoggerFactory.getLogger(NodeTypesJSONServlet.class);

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		PrintWriter writer = response.getWriter();
		Resource resource = request.getResource();
		Node currentNode = resource.adaptTo(Node.class);
		try {
			NodeTypeIterator nodeTypeIterator = currentNode.getSession().getWorkspace().getNodeTypeManager().getAllNodeTypes();
	        JSONObject nodeTypes = new JSONObject();
			while (nodeTypeIterator.hasNext()) {
				NodeType nodeType = nodeTypeIterator.nextNodeType();
				if (nodeType.getName() != null) {
					JSONNodeType jsonNodeType = new JSONNodeType(nodeType);
					nodeTypes.put(nodeType.getName(), jsonNodeType.getJson());
				}
			}
			writer.println(nodeTypes.toString(2));
			writer.flush();
			writer.close();
		} catch (RepositoryException e) {
			log.error("Could not generate the node types.", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} catch (JSONException e) {
			log.error("Could not generate the node types.", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}

}
