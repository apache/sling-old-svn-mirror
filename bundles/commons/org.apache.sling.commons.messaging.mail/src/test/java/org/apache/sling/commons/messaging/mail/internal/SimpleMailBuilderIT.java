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
package org.apache.sling.commons.messaging.mail.internal;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.mail.Email;
import org.apache.sling.commons.messaging.mail.MailBuilder;
import org.apache.sling.commons.messaging.mail.MailBuilderConfigurations;
import org.apache.sling.commons.messaging.mail.MailTestSupport;
import org.apache.sling.commons.messaging.mail.MailUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import static org.junit.Assert.assertEquals;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SimpleMailBuilderIT extends MailTestSupport {

    @Configuration
    public Option[] configuration() {
        return baseConfiguration();
    }

    @Before
    public void setup() throws Exception {
        final String factoryPid = "org.apache.sling.commons.messaging.mail.internal.SimpleMailBuilder";
        final Dictionary<String, Object> properties = MailBuilderConfigurations.minimal();
        createFactoryConfiguration(factoryPid, properties);
    }

    @Test
    public void testBuildWithDefaults() throws Exception {
        final MailBuilder mailBuilder = getService(MailBuilder.class);
        final Email email = mailBuilder.build("Stop your messing around, Better think of your future...", "rudy@ghosttown", Collections.emptyMap());
        email.buildMimeMessage();
        final byte[] bytes = MailUtil.toByteArray(email);
        final String mail = new String(bytes, StandardCharsets.UTF_8);
        logger.debug("mail: " + mail);
        assertEquals("rudy@ghosttown", email.getToAddresses().get(0).getAddress());
        assertEquals("Rudy, A Message to You", email.getSubject());
        assertEquals("dandy.livingstone@kingston.jamaica", email.getFromAddress().getAddress());
        assertEquals("localhost", email.getHostName());
        logger.debug(email.getMimeMessage().getContent().toString());
    }

    @Test
    public void testBuildWithData() throws Exception {
        final MailBuilder mailBuilder = getService(MailBuilder.class);
        final Map<String, String> configuration = new HashMap<>();
        configuration.put("mail.subject", "Rudy, A Message to You");
        configuration.put("mail.from", "specials@thespecials.com");
        final Map data = Collections.singletonMap("mail", configuration);
        final Email email = mailBuilder.build("A Message to You, Rudy", "rudy@ghosttown", data);
        email.buildMimeMessage();
        final byte[] bytes = MailUtil.toByteArray(email);
        final String mail = new String(bytes, StandardCharsets.UTF_8);
        logger.debug("mail: " + mail);
        assertEquals("rudy@ghosttown", email.getToAddresses().get(0).getAddress());
        assertEquals("Rudy, A Message to You", email.getSubject());
        assertEquals("specials@thespecials.com", email.getFromAddress().getAddress());
        assertEquals("localhost", email.getHostName());
        logger.debug(email.getMimeMessage().getContent().toString());
    }

}
