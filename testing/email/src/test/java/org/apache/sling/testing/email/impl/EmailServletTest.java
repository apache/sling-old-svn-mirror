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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.clients.util.PortAllocator;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.jayway.jsonpath.JsonPath;

public class EmailServletTest {
	
	@Rule
	public SlingContext ctx = new SlingContext();
	private int bindPort;
	private EMailServlet servlet;
	
	@Before
	public void prepare() {
		
		bindPort = new PortAllocator().allocatePort();
		
		ctx.registerInjectActivateService(new SmtpServerWrapper(), Collections.singletonMap("bind.port", bindPort));
		
		servlet = ctx.registerInjectActivateService(new EMailServlet());
	}

	@Test
	public void getBindPort() throws ServletException, IOException {
		
		// SLING-6947
		MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(ctx.resourceResolver()) {
			@Override
			public String getPathInfo() {
				return "/config";
			}
		};
		
		MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
		servlet.service(request, response);
		
		assertEquals("response.status", HttpServletResponse.SC_OK, response.getStatus());
		
		// SLING-6948
		byte[] out = response.getOutputAsString().getBytes();
		int configuredPort = JsonPath.read(new ByteArrayInputStream(out), "$.bindPort");
		
		assertThat("bindPort", configuredPort, equalTo(bindPort));
	}
	
	@Test
	public void getMessages() throws ServletException, IOException, MessagingException {
		
		String subject1 = "Test email";
		String body1 = "A long message \r\nbody";
		sendEmail(subject1, body1);
		
		String subject2 = "Verification email";
		String body2 = "A shorter message body";
		sendEmail(subject2, body2);
		
		// SLING-6947
		MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(ctx.resourceResolver()) {
			@Override
			public String getPathInfo() {
				return "/messages";
			}
		};
		
		MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
		servlet.service(request, response);
		
		assertEquals("response.status", HttpServletResponse.SC_OK, response.getStatus());
		
		// SLING-6948
		byte[] out = response.getOutputAsString().getBytes();
		List<String> subjects = JsonPath.read(new ByteArrayInputStream(out), "$.messages[*].Subject");
		
		assertThat("subjects.size", subjects, hasSize(2));
		assertThat("subjects", subjects, Matchers.hasItems(subject1, subject2));
		
		String readBody = JsonPath.read(new ByteArrayInputStream(out), "$.messages[0].['-Content-']");
		assertThat("body", readBody, equalTo(body1));
	}
	
	@Test
	public void getMessages_empty() throws ServletException, IOException {
		
		// SLING-6947
		MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(ctx.resourceResolver()) {
			@Override
			public String getPathInfo() {
				return "/messages";
			}
		};
		
		MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
		servlet.service(request, response);
		
		assertEquals("response.status", HttpServletResponse.SC_OK, response.getStatus());
		
		// SLING-6948
		byte[] out = response.getOutputAsString().getBytes();
		int messageCount = JsonPath.read(new ByteArrayInputStream(out), "$.messages.length()");
		
		assertThat("messages.length", messageCount, Matchers.equalTo(0));
	}
	
	@Test
	public void deleteMessages() throws MessagingException, ServletException, IOException {
		
		// send an email
		sendEmail("Test email", "A long message \r\nbody");
		
		// delete all messages
		MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(ctx.resourceResolver());
		request.setMethod("DELETE");
		MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
		servlet.service(request, response);
		
		assertEquals("response.status", HttpServletResponse.SC_NO_CONTENT, response.getStatus());
		
		// validate that no messages are stored
		getMessages_empty();
	}

	private void sendEmail(String subject, String body) throws MessagingException, AddressException {
		
		Properties mailProps = new Properties();
		mailProps.put("mail.smtp.host", "localhost");
		mailProps.put("mail.smtp.port", String.valueOf(bindPort));
		
		Session mailSession = Session.getInstance(mailProps);
		
		MimeMessage msg = new MimeMessage(mailSession);
		msg.setFrom(new InternetAddress("sender@localhost"));
		msg.addRecipient(RecipientType.TO, new InternetAddress("receiver@localhost"));
		msg.setSubject(subject);
		msg.setText(body);
		
		Transport.send(msg);
	}
}
