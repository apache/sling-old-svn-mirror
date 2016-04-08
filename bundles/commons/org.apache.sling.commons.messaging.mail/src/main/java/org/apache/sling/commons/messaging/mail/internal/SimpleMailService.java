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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.sling.commons.messaging.MessageService;
import org.apache.sling.commons.messaging.Result;
import org.apache.sling.commons.messaging.mail.MailBuilder;
import org.apache.sling.commons.messaging.mail.MailResult;
import org.apache.sling.commons.messaging.mail.MailUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = MessageService.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Service to send messages by mail.",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = SimpleMailServiceConfiguration.class
)
public class SimpleMailService implements MessageService {

    @Reference(
        cardinality = ReferenceCardinality.MANDATORY,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile MailBuilder mailBuilder;

    @Reference(
        cardinality = ReferenceCardinality.MANDATORY,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ThreadPoolManager threadPoolManager;

    // the ThreadPool used for sending mails
    private ThreadPool threadPool;

    private final Logger logger = LoggerFactory.getLogger(SimpleMailService.class);

    public SimpleMailService() {
    }

    @Activate
    private void activate(final SimpleMailServiceConfiguration configuration) {
        logger.debug("activate");
        configure(configuration);
    }

    @Modified
    private void modified(final SimpleMailServiceConfiguration configuration) {
        logger.debug("modified");
        configure(configuration);
    }

    @Deactivate
    protected void deactivate() {
        logger.info("deactivate");
        threadPoolManager.release(threadPool);
        threadPool = null;
    }

    private void configure(final SimpleMailServiceConfiguration configuration) {
        threadPoolManager.release(threadPool);
        threadPool = threadPoolManager.get(configuration.threadpoolName());
    }

    @Override
    public CompletableFuture<Result> send(@Nonnull final String message, @Nonnull final String recipient) {
        return send(message, recipient, Collections.emptyMap());
    }

    @Override
    public CompletableFuture<Result> send(@Nonnull final String message, @Nonnull final String recipient, @Nonnull final Map data) {
        return CompletableFuture.supplyAsync(() -> sendMail(message, recipient, data, mailBuilder), threadPool);
    }

    private MailResult sendMail(final String message, final String recipient, final Map data, final MailBuilder mailBuilder) {
        try {
            final Email email = mailBuilder.build(message, recipient, data);
            final String messageId = email.send();
            logger.info("mail '{}' sent", messageId);
            final byte[] bytes = MailUtil.toByteArray(email);
            return new MailResult(bytes);
        } catch (EmailException | MessagingException | IOException e) {
            throw new CompletionException(e);
        }
    }

}
