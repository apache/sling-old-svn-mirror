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
package org.apache.sling.jcr.contentloader.it;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import java.io.File;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.cm.ConfigurationAdminOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** Utilities for Pax Exam testing */
public final class PaxExamUtilities {

    private static final String SYS_PROP_BUILD_DIR = "bundle.build.dir";

    private static final String DEFAULT_BUILD_DIR = "target";

    public static Option[] paxConfig() {
        final File thisProjectsBundle = new File(System.getProperty( "bundle.file.name", "BUNDLE_FILE_NOT_SET" ));

        final String buildDir = System.getProperty(SYS_PROP_BUILD_DIR, DEFAULT_BUILD_DIR);

        final String jackrabbitVersion = "2.13.1";
        final String oakVersion = "1.5.7";

        final String slingHome = new File(buildDir + File.separatorChar + "sling_" + System.currentTimeMillis()).getAbsolutePath();

        return options(
                frameworkProperty("sling.home").value(slingHome),
                frameworkProperty("repository.home").value(slingHome + File.separatorChar + "repository"),
                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),

                ConfigurationAdminOptions.newConfiguration("org.apache.felix.jaas.ConfigurationSpi")
                    .create(true)
                    .put("jaas.defaultRealmName", "jackrabbit.oak")
                    .put("jaas.configProviderName", "FelixJaasProvider")
                    .asOption(),
                ConfigurationAdminOptions.factoryConfiguration("org.apache.felix.jaas.Configuration.factory")
                    .create(true)
                    .put("jaas.controlFlag", "optional")
                    .put("jaas.classname", "org.apache.jackrabbit.oak.spi.security.authentication.GuestLoginModule")
                    .put("jaas.ranking", 300)
                    .asOption(),
                ConfigurationAdminOptions.factoryConfiguration("org.apache.felix.jaas.Configuration.factory")
                    .create(true)
                    .put("jaas.controlFlag", "required")
                    .put("jaas.classname", "org.apache.jackrabbit.oak.security.authentication.user.LoginModuleImpl")
                    .asOption(),
                ConfigurationAdminOptions.factoryConfiguration("org.apache.felix.jaas.Configuration.factory")
                    .create(true)
                    .put("jaas.controlFlag", "sufficient")
                    .put("jaas.classname", "org.apache.jackrabbit.oak.security.authentication.token.TokenLoginModule")
                    .put("jaas.ranking", 200)
                    .asOption(),
                ConfigurationAdminOptions.newConfiguration("org.apache.jackrabbit.oak.security.authentication.AuthenticationConfigurationImpl")
                    .create(true)
                    .put("org.apache.jackrabbit.oak.authentication.configSpiName", "FelixJaasProvider")
                    .asOption(),
                ConfigurationAdminOptions.newConfiguration("org.apache.jackrabbit.oak.security.user.UserConfigurationImpl")
                    .create(true)
                    .put("groupsPath", "/home/groups")
                    .put("usersPath", "/home/users")
                    .put("defaultPath", "1")
                    .put("importBehavior", "besteffort")
                    .asOption(),
                ConfigurationAdminOptions.newConfiguration("org.apache.jackrabbit.oak.security.user.RandomAuthorizableNodeName")
                    .create(true)
                    .put("enabledActions", new String[] {"org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction"})
                    .put("userPrivilegeNames", new String[] {"jcr:all"})
                    .put("groupPrivilegeNames", new String[] {"jcr:read"})
                    .asOption(),
                ConfigurationAdminOptions.newConfiguration("org.apache.jackrabbit.oak.spi.security.user.action.DefaultAuthorizableActionProvider")
                    .create(true)
                    .put("length", 21)
                    .asOption(),
                ConfigurationAdminOptions.newConfiguration("org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStoreService")
                    .create(true)
                    .put("name", "Default NodeStore")
                    .asOption(),

                ConfigurationAdminOptions.factoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended")
                    .create(true)
                    .put("user.mapping", "org.apache.sling.event=admin")
                    .asOption(),
                ConfigurationAdminOptions.newConfiguration("org.apache.sling.jcr.resource.internal.JcrSystemUserValidator")
                    .create(true)
                    .put("allow.only.system.user", "false")
                    .asOption(),

                    // logging
                systemProperty("pax.exam.logging").value("none"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "4.0.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.6"),
                mavenBundle("org.slf4j", "slf4j-api", "1.7.13"),
                mavenBundle("org.slf4j", "jcl-over-slf4j", "1.7.13"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.13"),

                mavenBundle("commons-io", "commons-io", "2.4"),
                mavenBundle("commons-fileupload", "commons-fileupload", "1.3.1"),
                mavenBundle("commons-collections", "commons-collections", "3.2.2"),
                mavenBundle("commons-codec", "commons-codec", "1.10"),
                mavenBundle("commons-lang", "commons-lang", "2.6"),
                mavenBundle("org.apache.commons", "commons-lang3", "3.5"),
                mavenBundle("commons-pool", "commons-pool", "1.6"),

                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.concurrent", "1.3.4_1"),

                mavenBundle("org.apache.geronimo.bundles", "commons-httpclient", "3.1_1"),
                mavenBundle("org.apache.tika", "tika-core", "1.9"),
                mavenBundle("org.apache.tika", "tika-bundle", "1.9"),

                // infrastructure
                mavenBundle("org.apache.felix", "org.apache.felix.http.servlet-api", "1.1.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "3.1.6"),
                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.4.8"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.6"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.10"),
                mavenBundle("org.apache.felix", "org.apache.felix.inventory", "1.0.4"),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.1.2"),

                // sling
                mavenBundle("org.apache.sling", "org.apache.sling.settings", "1.3.8"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.3.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.mime", "2.1.8"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.classloader", "1.3.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.johnzon", "1.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.scheduler", "2.4.14"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.threads", "3.2.4"),

                mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.3.12"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.api", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.commons", "1.0.20"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.standalone", "1.0.2"),

                mavenBundle("org.apache.sling", "org.apache.sling.launchpad.api", "1.2.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.api", "2.14.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.resourceresolver", "1.4.18"),
                mavenBundle("org.apache.sling", "org.apache.sling.adapter", "2.1.10"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.resource", "2.8.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.classloader", "3.2.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.contentloader", "2.2.4"),
                mavenBundle("org.apache.sling", "org.apache.sling.engine", "2.6.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.serviceusermapper", "1.3.2"),

                mavenBundle("org.apache.sling", "org.apache.sling.jcr.jcr-wrapper", "2.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.api", "2.4.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.base", "2.4.0"),

                mavenBundle("com.google.guava", "guava", "15.0"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-api", jackrabbitVersion),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-commons", jackrabbitVersion),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-spi", jackrabbitVersion),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-spi-commons", jackrabbitVersion),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-rmi", jackrabbitVersion),

                mavenBundle("org.apache.felix", "org.apache.felix.jaas", "0.0.4"),

                mavenBundle("org.apache.jackrabbit", "oak-core", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-commons", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-lucene", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-blob", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-jcr", oakVersion),

                mavenBundle("org.apache.jackrabbit", "oak-segment", oakVersion),

                mavenBundle("org.apache.sling", "org.apache.sling.jcr.oak.server", "1.1.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.testing.tools", "1.0.17-SNAPSHOT"),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.1.2"),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.1.2"),

                provision(bundle(thisProjectsBundle.toURI().toString())),
                wrappedBundle(mavenBundle("org.apache.sling", "org.apache.sling.commons.testing").versionAsInProject()),
                wrappedBundle(mavenBundle("org.ops4j.pax.tinybundles", "tinybundles").versionAsInProject()),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.johnzon").versionAsInProject(),
                mavenBundle("biz.aQute.bnd", "bndlib").versionAsInProject(),

                junitBundles()
           );

    }

    public static Bundle findBundle(BundleContext ctx, String symbolicName) {
        for(Bundle b : ctx.getBundles()) {
            if(symbolicName.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }
}
