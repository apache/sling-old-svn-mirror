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

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.commons.messaging.mail.MailBuilder;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = MailBuilder.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Service to build simple mails.",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    },
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(
    ocd = SimpleMailBuilderConfiguration.class
)
public class SimpleMailBuilder implements MailBuilder {

    // TODO use encryption and support more configuration options

    private String subject;

    private String from;

    private String smtpHostname;

    private int smtpPort;

    private String smtpUsername;

    private String smtpPassword;

    private static final String SUBJECT_KEY = "mail.subject";

    private static final String FROM_KEY = "mail.from";

    private static final String SMTP_HOSTNAME_KEY = "mail.smtp.hostname";

    private static final String SMTP_PORT_KEY = "mail.smtp.port";

    private static final String SMTP_USERNAME_KEY = "mail.smtp.username";

    private static final String SMTP_PASSWORD_KEY = "mail.smtp.password";

    private final Logger logger = LoggerFactory.getLogger(SimpleMailBuilder.class);

    public SimpleMailBuilder() {
    }

    @Activate
    private void activate(final SimpleMailBuilderConfiguration configuration) {
        logger.debug("activate");
        configure(configuration);
    }

    @Modified
    private void modified(final SimpleMailBuilderConfiguration configuration) {
        logger.debug("modified");
        configure(configuration);
    }

    private void configure(final SimpleMailBuilderConfiguration configuration) {
        subject = configuration.subject();
        from = configuration.from();
        smtpHostname = configuration.smtpHostname();
        smtpPort = configuration.smtpPort();
        smtpUsername = configuration.smtpUsername();
        smtpPassword = configuration.smtpPassword();
    }

    @Override
    public Email build(@Nonnull final String message, @Nonnull final String recipient, @Nonnull final Map data) throws EmailException {
        final Map configuration = (Map) data.getOrDefault("mail", Collections.EMPTY_MAP);
        final String subject = (String) configuration.getOrDefault(SUBJECT_KEY, this.subject);
        final String from = (String) configuration.getOrDefault(FROM_KEY, this.from);
        final String smtpHostname = (String) configuration.getOrDefault(SMTP_HOSTNAME_KEY, this.smtpHostname);
        final int smtpPort = (Integer) configuration.getOrDefault(SMTP_PORT_KEY, this.smtpPort);
        final String smtpUsername = (String) configuration.getOrDefault(SMTP_USERNAME_KEY, this.smtpUsername);
        final String smtpPassword = (String) configuration.getOrDefault(SMTP_PASSWORD_KEY, this.smtpPassword);

        final Email email = new SimpleEmail();
        email.setMsg(message);
        email.addTo(recipient);
        email.setSubject(subject);
        email.setFrom(from);
        email.setHostName(smtpHostname);
        email.setSmtpPort(smtpPort);
        email.setAuthentication(smtpUsername, smtpPassword);
        return email;
    }

}
