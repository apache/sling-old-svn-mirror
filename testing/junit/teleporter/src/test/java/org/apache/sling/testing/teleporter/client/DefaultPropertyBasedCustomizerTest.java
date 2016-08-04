/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.teleporter.client;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultPropertyBasedCustomizerTest {

    private DefaultPropertyBasedCustomizer customizer;
    @Mock
    private ClientSideTeleporter clientSideTeleporter;
    
    @Before
    public void setUp() {
        // remove all properties starting with "ClientSideTeleporter."
        // removing all properties without conditions would prevent other java code from being executed without errors, because some java code at least relies on "java.home" being set!
        Properties oldProperties = System.getProperties();
        Properties newProperties = new Properties();
        for (Object key : oldProperties.keySet()) {
            if (!((String)key).startsWith("ClientSideTeleporter.")) {
                newProperties.put(key, oldProperties.get(key));
            }
        }
        System.setProperties(newProperties);
    }

    @Test(expected=AssertionError.class)
    public void testBaseUrlNotSet() {
        customizer = new DefaultPropertyBasedCustomizer();
        customizer.customize(clientSideTeleporter, null);
    }

    @Test(expected=AssertionError.class)
    public void testEmptyBaseUrl() {
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_BASE_URL, "   ");
        customizer = new DefaultPropertyBasedCustomizer();
        customizer.customize(clientSideTeleporter, null);
    }

    @Test
    public void testSettingAllProperties() {
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_BASE_URL, "base-url");
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_INCLUDE_DEPENDENCY_PREFIXES, "include-dependency1,include-dependency2");
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_EXCLUDE_DEPENDENCY_PREFIXES, "exclude-dependency1,exclude-dependency2");
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_EMBED_CLASSES, "org.apache.sling.testing.teleporter.client.ClassResourceVisitor,org.apache.sling.testing.teleporter.client.ClientSideTeleporter");
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_TESTREADY_TIMEOUT_SECONDS, "50");
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_SERVER_USERNAME, "adminuser");
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_SERVER_PASSWORD, "adminpassword");
        customizer = new DefaultPropertyBasedCustomizer();
        customizer.customize(clientSideTeleporter, null);
        Mockito.verify(clientSideTeleporter).setBaseUrl("base-url");
        Mockito.verify(clientSideTeleporter).includeDependencyPrefix("include-dependency1");
        Mockito.verify(clientSideTeleporter).includeDependencyPrefix("include-dependency2");
        Mockito.verify(clientSideTeleporter).excludeDependencyPrefix("exclude-dependency1");
        Mockito.verify(clientSideTeleporter).excludeDependencyPrefix("exclude-dependency2");
        Mockito.verify(clientSideTeleporter).embedClass(ClassResourceVisitor.class);
        Mockito.verify(clientSideTeleporter).embedClass(ClientSideTeleporter.class);
        Mockito.verify(clientSideTeleporter).setTestReadyTimeoutSeconds(50);
        Mockito.verify(clientSideTeleporter).setServerCredentials("adminuser", "adminpassword");
    }

    @Test(expected=AssertionError.class)
    public void testEmbeddingInvalidClass() {
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_BASE_URL, "base-url");
        System.setProperty(DefaultPropertyBasedCustomizer.PROPERTY_EMBED_CLASSES, "org.apache.sling.testing.teleporter.client.InvalidClass");
        customizer = new DefaultPropertyBasedCustomizer();
        customizer.customize(clientSideTeleporter, null);
    }
}
