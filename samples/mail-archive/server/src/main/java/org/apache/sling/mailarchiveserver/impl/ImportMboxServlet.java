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
package org.apache.sling.mailarchiveserver.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.mailarchiveserver.api.MboxParser;
import org.apache.sling.mailarchiveserver.api.MessageStore;
import org.apache.sling.mailarchiveserver.util.MailArchiveServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@SlingServlet(
		resourceTypes = "mailarchiveserver/import",
		methods = {"POST", "PUT"})
public class ImportMboxServlet extends SlingAllMethodsServlet {

	private static final Logger logger = LoggerFactory.getLogger(ImportMboxServlet.class);

	private static final String IMPORT_FILE_ATTRIB_NAME = "mboxfile";

	@Reference
	private MboxParser parser;
	@Reference
	private MessageStore store;

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
			throws ServletException, IOException {
		RequestParameter param = request.getRequestParameter(IMPORT_FILE_ATTRIB_NAME);
		if (param != null) {
			logger.info("Processing attachment: " + param.toString());

			InputStream mboxIS = param.getInputStream();
			store.saveAll(parser.parse(mboxIS));

			response.sendRedirect(MailArchiveServerConstants.ARCHIVE_PATH + ".html");
		} else {
			logger.info("No attachment to process.");
		}
	}

}
