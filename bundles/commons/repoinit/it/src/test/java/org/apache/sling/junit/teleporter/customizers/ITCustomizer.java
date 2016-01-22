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
package org.apache.sling.junit.teleporter.customizers;

import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.testing.teleporter.client.ClientSideTeleporter;

public class ITCustomizer implements TeleporterRule.Customizer {

    public static final String BASE_URL_PROP = "launchpad.http.server.url";
    @Override
    public void customize(TeleporterRule t, String options) {
        final ClientSideTeleporter cst = (ClientSideTeleporter)t;
        cst.setBaseUrl(System.getProperty(BASE_URL_PROP, BASE_URL_PROP + "_IS_NOT_SET"));
        cst.setServerCredentials("admin", "admin");
        cst.includeDependencyPrefix("org.apache.sling.repoinit.it");
    }
}
