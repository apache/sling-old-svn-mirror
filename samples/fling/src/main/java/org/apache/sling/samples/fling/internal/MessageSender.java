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
package org.apache.sling.samples.fling.internal;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.sling.commons.messaging.MessageService;
import org.apache.sling.commons.messaging.Result;
import org.apache.sling.samples.fling.SmtpService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Fling Sample “Message Sender”",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    },
    immediate = true
)
public class MessageSender {

    // depend on SmtpService so MessageService can deliver the messages
    @Reference
    private SmtpService smtpService;

    @Reference
    private MessageService messageService;

    private final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    public MessageSender() {
    }

    @Activate
    public void activate() throws ExecutionException, InterruptedException {
        logger.info("activate()");
        for (int i = 0; i < 10; i++) {
            final String message = "a simple text message";
            final String recipient = "form@fling";
            final String subject = String.format("message number %s", i);
            final Map configuration = Collections.singletonMap("mail.subject", subject);
            final CompletableFuture<Result> future = messageService.send(message, recipient, Collections.singletonMap("mail", configuration));
            future.thenAccept(result -> {
                final byte[] bytes = (byte[]) result.getMessage();
                logger.debug("message sent: {}", new String(bytes, StandardCharsets.UTF_8));
            });
        }
    }

}
