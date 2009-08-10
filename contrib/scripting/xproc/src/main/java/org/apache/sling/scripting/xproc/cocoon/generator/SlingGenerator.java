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
package org.apache.sling.scripting.xproc.cocoon.generator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletResponse;

import org.apache.cocoon.pipeline.component.sax.AbstractGenerator;
import org.apache.cocoon.pipeline.util.XMLUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Cocoon Generator that uses internal Sling
 * request processing in order to get the initial
 * XML for the pipeline.
 * 
 * In order of preference the generator tries
 * to resolve the current resource as:
 * 
 * 	+-- a XML file (adapts to {@link InputStream})
 * 	+-- dynamically generated XML (using inclusion procedure)
 *  +-- the underlying node´s export document view
 */
public class SlingGenerator extends AbstractGenerator {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private SlingHttpServletRequest request;
	private SlingHttpServletResponse response;
	
	public SlingGenerator(SlingScriptHelper sling) {
		this.request = sling.getRequest();
		this.response = sling.getResponse();
	}
	
	public void execute() {
		try {
			Session session;
			InputStream srcIs = genXmlSource();			
			if (srcIs != null)
				XMLUtils.toSax(srcIs, this.getXMLConsumer());
			else if ((session = request.getResource().adaptTo(Session.class)) != null) {
				session.exportDocumentView(request.getResource().getPath(), this.getXMLConsumer(), true, true);
			} else
				throw new IllegalArgumentException("cannot generate xml source for " + request.getResource().getPath());
			
		} catch (Throwable t) {
			log.error("SlingGenerator: cannot generate xml source for " 
					+ request.getResource().getPath(), t);
		}
	}
	
	private InputStream genXmlSource() throws Exception {
	
		String xmlPath = request.getResource().getPath() + ".xml";
		
		// The source is a xml file
		Resource xmlResource = this.request.getResourceResolver().resolve(xmlPath);
		InputStream xmlSourceFile = xmlResource.adaptTo(InputStream.class);
		if (xmlSourceFile != null) 
			return xmlSourceFile;
		
		// The source is dynamically generated 
		RequestDispatcher dispatcher = request.getRequestDispatcher(xmlPath);
		SlingGeneratorServletOutputStream output = new SlingGeneratorServletOutputStream();
		ServletResponse newResponse = new SlingGeneratorServletResponse(response, output);
		dispatcher.include(request, newResponse);
		byte[] bytes = output.toByteArray();
		if (bytes.length > 0)
			return new ByteArrayInputStream(bytes);
		
		return null;
		
	}

}
