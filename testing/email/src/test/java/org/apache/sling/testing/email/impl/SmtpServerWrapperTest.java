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
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.apache.sling.testing.clients.util.PortAllocator;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class SmtpServerWrapperTest {
	
	@Rule public OsgiContext ctx = new OsgiContext();
	
	private SmtpServerWrapper wrapper;

	@After
	public void cleanUp() {
		if ( wrapper != null ) {
			wrapper.deactivate();
		}
	}

	@Test
	public void startupWithPreconfiguredPort() throws ReflectiveOperationException {
		
		final int configuredPort = new PortAllocator().allocatePort();
		
		wrapper = ctx.registerInjectActivateService(new SmtpServerWrapper(), Collections.singletonMap("bind.port", configuredPort));
		
		assertThat("bindPort", wrapper.getEffectiveBindPort(), equalTo(configuredPort));
	}
	
	@Test
	public void startupWithRandomPort() throws ReflectiveOperationException {
		
		wrapper = ctx.registerInjectActivateService(new SmtpServerWrapper(), Collections.singletonMap("bind.port", 0));
		
		assertThat("bindPort", wrapper.getEffectiveBindPort(), greaterThan(0));
	}
}
