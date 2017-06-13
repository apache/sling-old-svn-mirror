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

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

// we want the component to be immediate since it is usable outside the OSGi service registry
// via SMTP connections
@Component(service = SmtpServerWrapper.class, immediate = true)
@Designate(ocd = SmtpServerWrapper.Config.class)
public class SmtpServerWrapper {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@ObjectClassDefinition(name="Apache Sling Testing SMTP Server Wrapper")
	public @interface Config {
		
		@AttributeDefinition(name="Bind port", description="The port on which the server will bind. A value of 0 requests binding on any available port ")
		int bind_port() default 0;
	}

	// wiser is not thread-safe so guard access to the instance
	private final Object sync = new Object();
	private Wiser wiser;
	private int effectiveBindPort;

	@Activate
	public void activate(Config cfg) throws ReflectiveOperationException {
		
		int bindPort;
		
		synchronized (sync) {
			wiser = new Wiser();
			wiser.setPort(cfg.bind_port());
			wiser.start();
			bindPort = cfg.bind_port() == 0 ? reflectiveGetEffectiveBindPort(wiser.getServer()) : cfg.bind_port();
		}
		
		effectiveBindPort = bindPort;
		
		logger.info("Started, Wiser listening on port {}", effectiveBindPort);
	}

	private int reflectiveGetEffectiveBindPort(SMTPServer server) throws ReflectiveOperationException {
		
		// we control the version of Wiser used so there is no risk of an exception here 
		Field field = SMTPServer.class.getDeclaredField("serverSocket");
		field.setAccessible(true);
		ServerSocket socket = (ServerSocket) field.get(server);

		return socket.getLocalPort();
	}

	@Deactivate
	public void deactivate() {
		
		synchronized (sync) {
			wiser.stop();
		}
	}
	
	public int getEffectiveBindPort() {
		
		return effectiveBindPort;
	}

	public void clearMessages() {
		
		synchronized (sync) {
			wiser.getMessages().clear();	
		}
	}
	
	public List<MimeMessage> getMessages() {
		
		try {
			List<MimeMessage> messages = new  ArrayList<>();
			synchronized (sync) {
				for ( WiserMessage message : wiser.getMessages()) {
					messages.add(message.getMimeMessage());
				}
			}
			return messages;
		} catch (MessagingException e) {
			throw new RuntimeException("Failed converting to " + MimeMessage.class.getName(), e);
		}
	}
}
