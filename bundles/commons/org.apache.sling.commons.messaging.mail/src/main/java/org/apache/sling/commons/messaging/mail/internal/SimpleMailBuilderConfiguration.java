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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Apache Sling Commons Messaging Mail “Simple Mail Builder”",
    description = "simple mail builder for Sling Commons Messaging Mail"
)
@interface SimpleMailBuilderConfiguration {

    @AttributeDefinition(
        name = "subject",
        description = "default subject for mails"
    )
    String subject();

    @AttributeDefinition(
        name = "from",
        description = "default from (sender) address for mails"
    )
    String from();

    @AttributeDefinition(
        name = "SMTP hostname",
        description = "hostname of SMTP server"
    )
    String smtpHostname() default "localhost";

    @AttributeDefinition(
        name = "SMTP port",
        description = "port of SMTP server"
    )
    int smtpPort() default 25;

    @AttributeDefinition(
        name = "SMTP username",
        description = "username for SMTP server"
    )
    String smtpUsername();

    @AttributeDefinition(
        name = "SMTP password",
        description = "password for SMTP server"
    )
    String smtpPassword();

}
