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
package org.apache.sling.launchpad.karaf.tests.bootstrap;

import javax.inject.Inject;

import org.apache.sling.commons.messaging.MessageService;
import org.apache.sling.launchpad.karaf.testing.KarafTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingCommonsMessagingMailIT extends KarafTestSupport {

    @Inject
    @Filter(timeout = 300000)
    public MessageService messageService;

    @Configuration
    public Option[] configuration() {
        return OptionUtils.combine(baseConfiguration(),
            editConfigurationFilePut("etc/org.apache.sling.commons.messaging.mail.internal.SimpleMailBuilder.cfg", "subject", "Default Subject"),
            editConfigurationFilePut("etc/org.apache.sling.commons.messaging.mail.internal.SimpleMailBuilder.cfg", "from", "from@example.net"),
            editConfigurationFilePut("etc/org.apache.sling.commons.messaging.mail.internal.SimpleMailBuilder.cfg", "smtp.hostname", "localhost"),
            editConfigurationFilePut("etc/org.apache.sling.commons.messaging.mail.internal.SimpleMailBuilder.cfg", "smtp.port", "25"),
            addSlingFeatures("sling-commons-messaging-mail")
        );
    }

    @Test
    public void testOrgApacheSlingCommonsMessagingMail() {
        final Bundle bundle = findBundle("org.apache.sling.commons.messaging.mail");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

}
