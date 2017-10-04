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
package org.apache.sling.karaf.tests.configuration;

import org.apache.sling.karaf.testing.KarafTestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

public class SlingLaunchpadOakTarConfiguration extends KarafTestSupport {

    @Configuration
    public Option[] configuration() throws Exception {
        final int httpPort = Integer.getInteger("http.port");
        final String[] scripts = new String[]{
            "create path /repoinit/provisioningModelTest\ncreate service user provisioningModelUser"
        };
        return OptionUtils.combine(baseConfiguration(),
            cleanCaches(true),
            // configurations for tests
            editConfigurationFilePut("etc/custom.properties", "sling.run.modes", "oak_tar"),
            editConfigurationFilePut("etc/users.properties", "admin", "admin,_g_:admingroup"), // Sling’s default admin credentials used in tests
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", Integer.toString(httpPort)),
            editConfigurationFilePut("etc/integrationTestsConfig.config", "message", "This test config should be loaded at startup"),
            editConfigurationFilePut("etc/org.apache.sling.servlets.resolver.SlingServletResolver.config", "servletresolver.cacheSize", "0"),
            editConfigurationFilePut("etc/org.apache.sling.jcr.webdav.impl.servlets.SimpleWebDavServlet.config", "dav.root", "/dav"),
            editConfigurationFilePut("etc/org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet.config", "alias", "/server"),
            editConfigurationFilePut("etc/org.apache.sling.jcr.repoinit.RepositoryInitializer-test.config", "scripts", scripts),
            editConfigurationFilePut("etc/org.apache.sling.jcr.base.internal.LoginAdminWhitelist.config", "whitelist.bypass", true),
            addSlingFeatures(
                "sling-launchpad-oak-tar",
                "sling-launchpad-content",
                "sling-auth-form",
                "sling-auth-openid",
                "sling-auth-selector",
                "sling-scripting-groovy",
                "sling-scripting-javascript",
                "sling-scripting-jsp",
                "sling-installer-provider-jcr",
                "sling-jcr-jackrabbit-security"
            ),
            // legacy
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.json").versionAsInProject(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlets.compat").versionAsInProject(),
            // test support
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.core").versionAsInProject(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.remote").versionAsInProject(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.scriptable").versionAsInProject(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.launchpad.test-services").versionAsInProject(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.launchpad.test-fragment").versionAsInProject(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.testing.tools").versionAsInProject(),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").versionAsInProject(),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").versionAsInProject()
        );
    }

}
