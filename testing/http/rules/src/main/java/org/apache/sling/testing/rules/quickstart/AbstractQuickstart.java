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
package org.apache.sling.testing.rules.quickstart;


import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.quickstart.QuickstartConfiguration;

import java.lang.reflect.Constructor;
import java.net.URI;

public abstract class AbstractQuickstart implements Quickstart {

    @Override
    public <T extends SlingClient> T getClient(Class<T> clientClass, String user, String pass) {
        QuickstartConfiguration configuration = getConfiguration();

        Constructor<T> constructor;

        try {
            constructor = clientClass.getConstructor(URI.class, String.class, String.class);
        } catch (NoSuchMethodException e) {
            return null;
        }

        T client;

        try {
            client = constructor.newInstance(configuration.getUrl(), user, pass);
        } catch (Exception e) {
            return null;
        }

        return client;
    }

    @Override
    public <T extends SlingClient> T getAdminClient(Class<T> clientClass) {
        return getClient(clientClass, "admin", "admin");
    }

    @Override
    public <T extends SlingClient> T getAuthorClient(Class<T> clientClass) {
        return getClient(clientClass, "author", "author");
    }

}
