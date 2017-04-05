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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.mail.AuthenticationFailedException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.sling.commons.messaging.MessageService;
import org.apache.sling.commons.messaging.Result;
import org.apache.sling.commons.messaging.mail.MailBuilderConfigurations;
import org.apache.sling.commons.messaging.mail.MailResult;
import org.apache.sling.commons.messaging.mail.MailTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.subethamail.wiser.Wiser;

import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SimpleMailServiceIT extends MailTestSupport {

    protected Wiser wiser;

    public static final String FACTORY_PID = "org.apache.sling.commons.messaging.mail.internal.SimpleMailBuilder";

    @Configuration
    public Option[] configuration() {
        return baseConfiguration();
    }

    @Before
    public void setup() throws Exception {
        final int smtpPort = findFreePort();
        wiser = new Wiser(smtpPort);
        wiser.start();
    }

    @After
    public void teardown() {
        wiser.stop();
        wiser = null;
    }

    @Test
    public void send() throws Exception {
        final Dictionary<String, Object> properties = MailBuilderConfigurations.full(wiser.getServer().getPort());
        createFactoryConfiguration(FACTORY_PID, properties);
        final MessageService messageService = getService(MessageService.class);
        final CompletableFuture<Result> future = messageService.send("simple test message", "recipient@example.net");
        final MailResult result = (MailResult) future.get();
        final String message = new String(result.getMessage(), StandardCharsets.UTF_8);
        logger.info("message: {}", message); // TODO assert
    }

    @Test
    public void sendWithData() throws Exception {
        final Dictionary<String, Object> properties = MailBuilderConfigurations.full(wiser.getServer().getPort());
        createFactoryConfiguration(FACTORY_PID, properties);
        final MessageService messageService = getService(MessageService.class);
        final Map configuration = Collections.singletonMap("mail.subject", "Testing the Simple Mail Service with a custom subject");
        final Map data = Collections.singletonMap("mail", configuration);
        final CompletableFuture<Result> future = messageService.send("simple test message", "recipient@example.net", data);
        final MailResult result = (MailResult) future.get();
        final String message = new String(result.getMessage(), StandardCharsets.UTF_8);
        logger.info("message: {}", message); // TODO assert
    }

    @Test
    public void sendWithoutAuthentication() throws Exception {
        final Dictionary<String, Object> properties = MailBuilderConfigurations.minimal();
        createFactoryConfiguration(FACTORY_PID, properties);
        final MessageService messageService = getService(MessageService.class);
        final CompletableFuture<Result> future = messageService.send("simple test message", "recipient@example.net");
        try {
            future.get();
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            assertTrue(ExceptionUtils.getRootCause(e) instanceof AuthenticationFailedException);
        }
    }

}
