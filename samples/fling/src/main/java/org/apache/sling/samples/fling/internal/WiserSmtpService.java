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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.sling.samples.fling.SmtpService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

@Component(
    service = SmtpService.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Fling Sample “Wiser SMTP Service”",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    },
    immediate = true
)
@Designate(
    ocd = WiserSmtpServiceConfiguration.class
)
public class WiserSmtpService implements SmtpService {

    private Wiser wiser;

    private final Logger logger = LoggerFactory.getLogger(WiserSmtpService.class);

    @Activate
    public void activate(final WiserSmtpServiceConfiguration configuration) throws Exception {
        wiser = new Wiser(configuration.smtpPort());
        wiser.start();
    }

    @Deactivate
    protected void deactivate() {
        wiser.stop();
        wiser = null;
    }

    @Override
    public List<MimeMessage> getMessages() {
        final List<MimeMessage> messages = new ArrayList<>();
        if (wiser != null) {
            for (final WiserMessage message : wiser.getMessages()) {
                try {
                    messages.add(message.getMimeMessage());
                } catch (MessagingException e) {
                    logger.error("error getting message from server: {}", e.getMessage(), e);
                }
            }
        }
        return messages;
    }

}
