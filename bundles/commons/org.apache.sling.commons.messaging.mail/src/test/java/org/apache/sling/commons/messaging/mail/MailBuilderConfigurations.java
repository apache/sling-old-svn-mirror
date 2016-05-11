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
package org.apache.sling.commons.messaging.mail;

import java.util.Dictionary;
import java.util.Hashtable;

public class MailBuilderConfigurations {

    /**
     * @return minimal configuration properties for building mails
     */
    public static Dictionary<String, Object> minimal() {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("subject", "Rudy, A Message to You");
        properties.put("from", "dandy.livingstone@kingston.jamaica");
        properties.put("smtpHostname", "localhost");
        return properties;
    }

    /**
     * @param smtpPort SMTP port to use for sending
     * @return configuration properties including authentication for sending
     */
    public static Dictionary<String, Object> full(final int smtpPort) {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("subject", "Testing the Simple Mail Service");
        properties.put("from", "sender@example.net");
        properties.put("smtpHostname", "localhost");
        properties.put("smtpPort", smtpPort);
        properties.put("smtpUsername", "test");
        properties.put("smtpPassword", "test");
        return properties;
    }

}
