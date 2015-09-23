/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.teleporter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.launchpad.testservices.exported.StringTransformer;
import org.junit.Rule;
import org.junit.Test;

/** Test Teleporter access to services using LDAP filters */
public class LdapFilterTeleporterTest {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");
    
    @Test
    public void testUppercase() {
        final StringTransformer t = teleporter.getService(StringTransformer.class, "(mode=uppercase)");
        assertNotNull("Expecting an uppercase transformer", t);
        assertEquals("FOOBAR", t.transform("fooBAR"));
    }
    
    @Test
    public void testLowercase() {
        final StringTransformer t = teleporter.getService(StringTransformer.class, "(mode=lowercase)");
        assertNotNull("Expecting a lowercase transformer", t);
        assertEquals("foobar", t.transform("fooBAR"));
    }
    
}