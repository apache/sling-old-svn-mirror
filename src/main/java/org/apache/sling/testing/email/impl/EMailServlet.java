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
package org.apache.sling.testing.email.impl;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.util.Enumeration;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.utils.json.JSONWriter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Exposes emails stored by the {@link SmtpServerWrapper} through an HTTP API
 * 
 * <p>The entry points for the servlet are:
 * 
 * <ol>
 * <li><tt>GET /system/sling/testing/email/config</tt>, which returns 
 * 	a JSON object containing the configuration properties of the {@link SmtpServerWrapper}</li>
 * <li><tt>GET /system/sling/testing/email/messages</tt>, which returns the messages
 *  currently held by the {@link SmtpServerWrapper}</li>
 *  <li><tt>DELETE /system/sling/testing/email</tt>, which removes all messages.</li>
 * </ol>
 */
@Component(service = Servlet.class, 
		   property = {
				   HTTP_WHITEBOARD_SERVLET_PATTERN + "=/system/sling/testing/email",
				   HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=org.osgi.service.http)"
		   })
public class EMailServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	@Reference
	private SmtpServerWrapper wiser;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		resp.setContentType("application/json");
		JSONWriter w = new JSONWriter(resp.getWriter());
		
		String action = ( req.getPathInfo() == null || req.getPathInfo().isEmpty() ) ? "messages" : req.getPathInfo().substring(1);  

		switch (action) {
			case "messages":
				w.object();
				w.key("messages");
				w.array();
				for ( MimeMessage msg : wiser.getMessages() ) {
					w.object();
					try {
						Enumeration<?> headers = msg.getAllHeaders();
						while ( headers.hasMoreElements()) {
							Header header = (Header) headers.nextElement();
							w.key(header.getName()).value(header.getValue());
						}
						
						w.key("-Content-").value(msg.getContent());
						
					} catch (MessagingException e) {
						throw new ServletException("Failed retrieving message data", e);
					}
					w.endObject();
				}
				w.endArray();
				w.endObject();
				break;
			
			case "config":
				w.object();
				w.key("bindPort").value(wiser.getEffectiveBindPort());
				w.endObject();
				break;
			
			default:
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				break;
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		wiser.clearMessages();
		
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
}
