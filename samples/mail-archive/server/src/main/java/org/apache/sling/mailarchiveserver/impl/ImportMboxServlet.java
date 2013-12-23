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
