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
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
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
		@Property(name = "service.description", value = "Download Servlet for binary properties"),
		@Property(name = "service.vendor", value = "The Apache Software Foundation"),
		@Property(name = "sling.servlet.selectors", value = "property"),
		@Property(name = "sling.servlet.extensions", value = "download"),
		@Property(name = "sling.servlet.resourceTypes", value = "sling/servlet/default")

})
public class DownloadBinaryProperty extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = -1L;

	/** default log */
	private final Logger log = LoggerFactory
			.getLogger(DownloadBinaryProperty.class);

	@Override
	protected void doGet(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws ServletException,
			IOException {
		String propertyName = request.getParameter("property");
		if ("".equals(propertyName) || propertyName == null)
			throw new ResourceNotFoundException("No property specified.");

		ServletOutputStream out = response.getOutputStream();
		Resource resource = request.getResource();
		Node currentNode = resource.adaptTo(Node.class);
		javax.jcr.Property property = null;
		try {
			property = currentNode.getProperty(propertyName);
		} catch (PathNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found.");
			throw new ResourceNotFoundException("Not found.");
		} catch (RepositoryException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found.");
			throw new ResourceNotFoundException("Not found.");
		}
		InputStream stream = null;
		try {
			if (property == null || property.getType() != PropertyType.BINARY) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found.");
				throw new ResourceNotFoundException("Not found.");
			}
			long length = property.getLength();
			if (length > 0) {
				if (length < Integer.MAX_VALUE) {
					response.setContentLength((int) length);
				} else {
					response.setHeader("Content-Length", String.valueOf(length));
				}
			}
			stream = property.getStream();
			byte[] buf = new byte[2048];
			int rd;
			while ((rd = stream.read(buf)) >= 0) {
				out.write(buf, 0, rd);
			}
		} catch (ValueFormatException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Error downloading the property content.");
			log.debug("error reading the property " + property + " of path "
					+ resource.getPath(), e);
		} catch (RepositoryException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Error downloading the property content.");
			log.debug("error reading the property " + property + " of path "
					+ resource.getPath(), e);
		} finally {
			stream.close();
		}

	}
}