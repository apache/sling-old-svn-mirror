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
package org.apache.sling.testing.paxexam;

import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

/**
 * NOTE: This file is generated from Sling's Karaf Features
 */
public class SlingOptions {

    public static SlingVersionResolver versionResolver = new SlingVersionResolver();

    public static Option config() {
        return mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version(versionResolver);
    }

    public static Option eventadmin() {
        return composite(
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.eventadmin").version(versionResolver),
            config()
        );
    }

    public static Option http() {
        return composite(
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.http.jetty").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.http.servlet-api").version(versionResolver),
            config()
        );
    }

    public static Option httpWhiteboard() {
        return composite(
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.http.whiteboard").version(versionResolver),
            http()
        );
    }

    public static Option management() {
        return composite(
            mavenBundle().groupId("org.apache.aries").artifactId("org.apache.aries.util").version(versionResolver),
            mavenBundle().groupId("org.apache.aries.jmx").artifactId("org.apache.aries.jmx.api").version(versionResolver),
            mavenBundle().groupId("org.apache.aries.jmx").artifactId("org.apache.aries.jmx.core").version(versionResolver),
            mavenBundle().groupId("org.apache.aries.jmx").artifactId("org.apache.aries.jmx.whiteboard").version(versionResolver),
            config()
        );
    }

    public static Option scr() {
        return composite(
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.metatype").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").version(versionResolver),
            config()
        );
    }

    public static Option webconsole() {
        return composite(
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.webconsole").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.inventory").version(versionResolver),
            mavenBundle().groupId("commons-fileupload").artifactId("commons-fileupload").version(versionResolver),
            mavenBundle().groupId("commons-io").artifactId("commons-io").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.bundles").artifactId("json").version(versionResolver),
            http()
        );
    }

    public static Option paxUrl() {
        return composite(
            mavenBundle().groupId("org.ops4j.pax.url").artifactId("pax-url-commons").version(versionResolver),
            mavenBundle().groupId("org.ops4j.base").artifactId("ops4j-base-lang").version(versionResolver),
            mavenBundle().groupId("org.ops4j.base").artifactId("ops4j-base-util-property").version(versionResolver),
            mavenBundle().groupId("org.ops4j.pax.swissbox").artifactId("pax-swissbox-property").version(versionResolver),
            config()
        );
    }

    public static Option paxUrlClasspath() {
        return composite(
            mavenBundle().groupId("org.ops4j.pax.url").artifactId("pax-url-classpath").version(versionResolver),
            paxUrl()
        );
    }

    public static Option sling() {
        return composite(
            config(),
            eventadmin(),
            scr(),
            management(),
            http(),
            httpWhiteboard(),
            slingCommonsClassloader(),
            slingCommonsScheduler(),
            slingCommonsThreads(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.api").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.auth.core").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.engine").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.resourceresolver").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.serviceusermapper").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.settings").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.json").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.mime").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.osgi").version(versionResolver),
            mavenBundle().groupId("javax.jcr").artifactId("jcr").version(versionResolver),
            mavenBundle().groupId("commons-codec").artifactId("commons-codec").version(versionResolver),
            mavenBundle().groupId("commons-collections").artifactId("commons-collections").version(versionResolver),
            mavenBundle().groupId("commons-fileupload").artifactId("commons-fileupload").version(versionResolver),
            mavenBundle().groupId("commons-io").artifactId("commons-io").version(versionResolver),
            mavenBundle().groupId("commons-lang").artifactId("commons-lang").version(versionResolver),
            mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").version(versionResolver),
            mavenBundle().groupId("org.apache.commons").artifactId("commons-math").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.bundles").artifactId("json").version(versionResolver)
        );
    }

    public static Option slingAuthForm() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.auth.form").version(versionResolver)
        );
    }

    public static Option slingCommonsCompiler() {
        return composite(
            slingCommonsClassloader(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.compiler").version(versionResolver)
        );
    }

    public static Option slingCommonsClassloader() {
        return composite(
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.classloader").version(versionResolver)
        );
    }

    public static Option slingCommonsFsclassloader() {
        return composite(
            scr(),
            webconsole(),
            slingCommonsClassloader(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.fsclassloader").version(versionResolver),
            mavenBundle().groupId("commons-io").artifactId("commons-io").version(versionResolver),
            mavenBundle().groupId("commons-lang").artifactId("commons-lang").version(versionResolver)
        );
    }

    public static Option slingCommonsMessaging() {
        return composite(
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.messaging").version(versionResolver)
        );
    }

    public static Option slingCommonsMessagingMail() {
        return composite(
            scr(),
            slingCommonsMessaging(),
            slingCommonsThreads(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.messaging.mail").version(versionResolver),
            mavenBundle().groupId("com.sun.mail").artifactId("javax.mail").version(versionResolver),
            mavenBundle().groupId("javax.mail").artifactId("javax.mail-api").version(versionResolver),
            mavenBundle().groupId("org.apache.commons").artifactId("commons-email").version(versionResolver)
        );
    }

    public static Option slingCommonsMetrics() {
        return composite(
            scr(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.metrics").version(versionResolver),
            mavenBundle().groupId("io.dropwizard.metrics").artifactId("metrics-core").version(versionResolver)
        );
    }

    public static Option slingCommonsScheduler() {
        return composite(
            scr(),
            slingCommonsThreads(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.scheduler").version(versionResolver),
            mavenBundle().groupId("javax.servlet").artifactId("javax.servlet-api").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jta_1.1_spec").version(versionResolver)
        );
    }

    public static Option slingCommonsThreads() {
        return composite(
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.threads").version(versionResolver)
        );
    }

    public static Option slingExtensionAdapter() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.adapter").version(versionResolver)
        );
    }

    public static Option slingExtensionBundleresource() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.bundleresource.impl").version(versionResolver)
        );
    }

    public static Option slingExtensionDiscovery() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.discovery.api").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.discovery.base").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.discovery.commons").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.discovery.support").version(versionResolver),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").version(versionResolver),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").version(versionResolver)
        );
    }

    public static Option slingExtensionDiscoveryImpl() {
        return composite(
            webconsole(),
            slingExtensionDiscovery(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.discovery.impl").version(versionResolver)
        );
    }

    public static Option slingExtensionDiscoveryOak() {
        return composite(
            webconsole(),
            slingExtensionDiscovery(),
            slingExtensionHealthcheck(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.discovery.oak").version(versionResolver)
        );
    }

    public static Option slingExtensionDiscoveryStandalone() {
        return composite(
            slingExtensionDiscovery(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.discovery.standalone").version(versionResolver)
        );
    }

    public static Option slingExtensionEvent() {
        return composite(
            sling(),
            slingExtensionDiscovery(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.event").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.event.dea").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.inventory").version(versionResolver)
        );
    }

    public static Option slingExtensionExplorer() {
        return composite(
            sling(),
            slingScriptingJavascript(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.extensions.explorer").version(versionResolver)
        );
    }

    public static Option slingExtensionFeatureflags() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.featureflags").version(versionResolver)
        );
    }

    public static Option slingExtensionFsresource() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.fsresource").version(versionResolver)
        );
    }

    public static Option slingExtensionI18n() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.i18n").version(versionResolver)
        );
    }

    public static Option slingExtensionJmxProvider() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jmx.provider").version(versionResolver)
        );
    }

    public static Option slingExtensionModels() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.models.api").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.models.impl").version(versionResolver)
        );
    }

    public static Option slingExtensionResourceInventory() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.resource.inventory").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.inventory").version(versionResolver)
        );
    }

    public static Option slingExtensionThreaddump() {
        return composite(
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.extensions.threaddump").version(versionResolver)
        );
    }

    public static Option slingExtensionValidation() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.validation.api").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.validation.core").version(versionResolver)
        );
    }

    public static Option slingExtensionXss() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.xss").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.bundles").artifactId("commons-httpclient").version(versionResolver)
        );
    }

    public static Option slingInstaller() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.installer.console").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.installer.core").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.installer.factory.configuration").version(versionResolver)
        );
    }

    public static Option slingInstallerProviderFile() {
        return composite(
            slingInstaller(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.installer.provider.file").version(versionResolver)
        );
    }

    public static Option slingInstallerProviderJcr() {
        return composite(
            slingInstaller(),
            slingJcr(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.installer.provider.jcr").version(versionResolver)
        );
    }

    public static Option slingJcr() {
        return composite(
            webconsole(),
            sling(),
            jackrabbitSling(),
            tikaSling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.api").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.base").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.classloader").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.contentloader").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.davex").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.registration").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.resource").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.webconsole").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.webdav").version(versionResolver)
        );
    }

    public static Option slingJcrCompiler() {
        return composite(
            sling(),
            slingJcr(),
            slingCommonsCompiler(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.compiler").version(versionResolver)
        );
    }

    public static Option slingJcrJackrabbitSecurity() {
        return composite(
            slingJcr(),
            slingServlets(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.jackrabbit.accessmanager").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.jackrabbit.usermanager").version(versionResolver)
        );
    }

    public static Option slingJcrOak() {
        return composite(
            scr(),
            slingJcr(),
            jackrabbitSling(),
            tikaSling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.oak.server").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-core").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-commons").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-blob").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-jcr").version(versionResolver),
            mavenBundle().groupId("com.google.guava").artifactId("guava").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.jaas").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-lucene").version(versionResolver)
        );
    }

    public static Option slingJcrRepoinit() {
        return composite(
            sling(),
            slingJcr(),
            slingJcrJackrabbitSecurity(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.repoinit").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.repoinit.parser").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.provisioning.model").version(versionResolver)
        );
    }

    public static Option slingLaunchpadContent() {
        return composite(
            sling(),
            slingAuthForm(),
            slingExtensionExplorer(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.launchpad.content").version(versionResolver)
        );
    }

    public static Option slingLaunchpadOak() {
        return composite(
            webconsole(),
            sling(),
            slingServlets(),
            slingInstaller(),
            slingExtensionAdapter(),
            slingExtensionBundleresource(),
            slingExtensionDiscoveryOak(),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.prefs").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.webconsole.plugins.memoryusage").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.webconsole.plugins.packageadmin").version(versionResolver),
            newConfiguration("org.apache.sling.resourceresolver.impl.observation.OsgiObservationBridge")
                .put("resource.change.types", "[\"ADDED\", \"CHANGED\", \"REMOVED\"]")
                .put("resource.paths", "/")
                .asOption(),
            factoryConfiguration("org.apache.felix.jaas.Configuration.factory")
                .put("jaas.ranking", "I\"300\"")
                .put("jaas.classname", "org.apache.jackrabbit.oak.spi.security.authentication.GuestLoginModule")
                .put("jaas.controlFlag", "optional")
                .asOption(),
            factoryConfiguration("org.apache.felix.jaas.Configuration.factory")
                .put("jaas.classname", "org.apache.jackrabbit.oak.security.authentication.user.LoginModuleImpl")
                .put("jaas.controlFlag", "required")
                .asOption(),
            factoryConfiguration("org.apache.felix.jaas.Configuration.factory")
                .put("jaas.ranking", "I\"200\"")
                .put("jaas.classname", "org.apache.jackrabbit.oak.security.authentication.token.TokenLoginModule")
                .put("jaas.controlFlag", "sufficient")
                .asOption(),
            newConfiguration("org.apache.felix.jaas.ConfigurationSpi")
                .put("jaas.configProviderName", "FelixJaasProvider")
                .put("jaas.defaultRealmName", "jackrabbit.oak")
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.security.authentication.AuthenticationConfigurationImpl")
                .put("org.apache.jackrabbit.oak.authentication.configSpiName", "FelixJaasProvider")
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.security.user.UserConfigurationImpl")
                .put("usersPath", "/home/users")
                .put("importBehavior", "besteffort")
                .put("defaultDepth", "1")
                .put("groupsPath", "/home/groups")
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.spi.security.user.action.DefaultAuthorizableActionProvider")
                .put("groupPrivilegeNames", "[\"jcr:read\"]")
                .put("enabledActions", "[\"org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction\"]")
                .put("userPrivilegeNames", "[\"jcr:all\"]")
                .asOption()
        );
    }

    public static Option slingLaunchpadOakTar() {
        return composite(
            slingJcrOak(),
            slingLaunchpadOak(),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-segment").version(versionResolver),
            newConfiguration("org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStoreService")
                .put("repository.home", "sling/repository")
                .put("name", "Default NodeStore")
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProviderService")
                .put("localIndexDir", "sling/repository/index")
                .asOption()
        );
    }

    public static Option slingLaunchpadOakMongo() {
        return composite(
            slingJcrOak(),
            slingLaunchpadOak(),
            mavenBundle().groupId("org.mongodb").artifactId("mongo-java-driver").version(versionResolver),
            newConfiguration("org.apache.jackrabbit.oak.plugins.document.DocumentNodeStoreService")
                .put("mongouri", "mongodb://localhost:27017")
                .put("db", "sling")
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProviderService")
                .put("localIndexDir", "sling/repository/index")
                .asOption()
        );
    }

    public static Option slingScripting() {
        return composite(
            sling(),
            webconsole(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.api").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.core").version(versionResolver)
        );
    }

    public static Option slingScriptingJavascript() {
        return composite(
            slingScripting(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.javascript").version(versionResolver)
        );
    }

    public static Option slingScriptingJsp() {
        return composite(
            slingScripting(),
            slingCommonsCompiler(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.jsp").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.jsp.taglib").version(versionResolver)
        );
    }

    public static Option slingScriptingSightly() {
        return composite(
            sling(),
            slingJcr(),
            slingScripting(),
            slingExtensionI18n(),
            slingExtensionXss(),
            slingJcrCompiler(),
            slingCommonsFsclassloader(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.sightly").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.sightly.js.provider").version(versionResolver),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.rhino").version(versionResolver)
        );
    }

    public static Option slingServlets() {
        return composite(
            sling(),
            slingJcr(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlets.get").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlets.post").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlets.resolver").version(versionResolver)
        );
    }

    public static Option jackrabbitSling() {
        return composite(
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("jackrabbit-api").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("jackrabbit-data").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("jackrabbit-jcr-commons").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("jackrabbit-jcr-rmi").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("jackrabbit-spi").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("jackrabbit-spi-commons").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("jackrabbit-webdav").version(versionResolver),
            mavenBundle().groupId("javax.jcr").artifactId("jcr").version(versionResolver),
            mavenBundle().groupId("commons-codec").artifactId("commons-codec").version(versionResolver),
            mavenBundle().groupId("commons-collections").artifactId("commons-collections").version(versionResolver),
            mavenBundle().groupId("commons-fileupload").artifactId("commons-fileupload").version(versionResolver),
            mavenBundle().groupId("commons-io").artifactId("commons-io").version(versionResolver),
            mavenBundle().groupId("commons-lang").artifactId("commons-lang").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.bundles").artifactId("commons-httpclient").version(versionResolver),
            mavenBundle().groupId("com.google.guava").artifactId("guava").version(versionResolver),
            mavenBundle().groupId("javax.servlet").artifactId("javax.servlet-api").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-atinject_1.0_spec").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-el_2.2_spec").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-interceptor_1.1_spec").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jcdi_1.0_spec").version(versionResolver),
            mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jta_1.1_spec").version(versionResolver)
        );
    }

    public static Option tikaSling() {
        return composite(
            mavenBundle().groupId("org.apache.tika").artifactId("tika-core").version(versionResolver),
            mavenBundle().groupId("org.apache.tika").artifactId("tika-bundle").version(versionResolver)
        );
    }

    public static Option slingAuthOpenid() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.auth.openid").version(versionResolver)
        );
    }

    public static Option slingAuthSelector() {
        return composite(
            slingAuthForm(),
            slingAuthOpenid(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.auth.selector").version(versionResolver)
        );
    }

    public static Option slingCommonsHtml() {
        return composite(
            scr(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.html").version(versionResolver)
        );
    }

    public static Option slingExtensionDistribution() {
        return composite(
            sling(),
            slingJcr(),
            slingExtensionEvent(),
            slingExtensionHealthcheck(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.distribution.api").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.distribution.core").version(versionResolver),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").version(versionResolver),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit.vault").artifactId("org.apache.jackrabbit.vault").version(versionResolver)
        );
    }

    public static Option slingExtensionHealthcheck() {
        return composite(
            sling(),
            slingJcr(),
            slingScripting(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.hc.core").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.hc.jmx").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.hc.support").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.hc.webconsole").version(versionResolver)
        );
    }

    public static Option slingExtensionQuery() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.query").version(versionResolver)
        );
    }

    public static Option slingExtensionResourcemerger() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.resourcemerger").version(versionResolver)
        );
    }

    public static Option slingExtensionRewriter() {
        return composite(
            sling(),
            slingCommonsHtml(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.rewriter").version(versionResolver)
        );
    }

    public static Option slingExtensionSecurity() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.security").version(versionResolver)
        );
    }

    public static Option slingExtensionUrlrewriter() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.urlrewriter").version(versionResolver),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.urlrewritefilter").version(versionResolver)
        );
    }

    public static Option slingLaunchpadNosqlCouchbase() {
        return composite(
            slingNosqlCouchbase(),
            factoryConfiguration("org.apache.sling.nosql.couchbase.resourceprovider.CouchbaseNoSqlResourceProviderFactory.factory.config")
                .put("provider.roots", "[\"/\"]")
                .asOption(),
            factoryConfiguration("org.apache.sling.nosql.couchbase.client.CouchbaseClient.factory.config")
                .put("bucketName", "sling")
                .put("enabled", "B\"true\"")
                .put("couchbaseHosts", "localhost:8091")
                .put("clientId", "sling-resourceprovider-couchbase")
                .asOption()
        );
    }

    public static Option slingLaunchpadNosqlMongodb() {
        return composite(
            slingNosqlMongodb(),
            factoryConfiguration("org.apache.sling.nosql.mongodb.resourceprovider.MongoDBNoSqlResourceProviderFactory.factory.config")
                .put("collection", "resources")
                .put("database", "sling")
                .put("connectionString", "localhost:27017")
                .put("provider.roots", "[\"/\"]")
                .asOption()
        );
    }

    public static Option slingNosqlGeneric() {
        return composite(
            sling(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.nosql.generic").version(versionResolver)
        );
    }

    public static Option slingNosqlCouchbase() {
        return composite(
            slingNosqlGeneric(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.nosql.couchbase-client").version(versionResolver),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.nosql.couchbase-resourceprovider").version(versionResolver),
            mavenBundle().groupId("io.wcm.osgi.wrapper").artifactId("io.wcm.osgi.wrapper.rxjava").version(versionResolver)
        );
    }

    public static Option slingNosqlMongodb() {
        return composite(
            slingNosqlGeneric(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.nosql.mongodb-resourceprovider").version(versionResolver),
            mavenBundle().groupId("org.mongodb").artifactId("mongo-java-driver").version(versionResolver)
        );
    }

    public static Option slingScriptingFreemarker() {
        return composite(
            sling(),
            slingScripting(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.freemarker").version(versionResolver)
        );
    }

    public static Option slingScriptingGroovy() {
        return composite(
            sling(),
            slingScripting(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.groovy").version(versionResolver),
            mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy").version(versionResolver),
            mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy-json").version(versionResolver),
            mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy-templates").version(versionResolver)
        );
    }

    public static Option slingScriptingJava() {
        return composite(
            sling(),
            slingScripting(),
            slingCommonsCompiler(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.java").version(versionResolver)
        );
    }

    public static Option slingScriptingThymeleaf() {
        return composite(
            sling(),
            slingScripting(),
            slingExtensionI18n(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.thymeleaf").version(versionResolver),
            mavenBundle().groupId("org.javassist").artifactId("javassist").version(versionResolver)
        );
    }

    public static Option slingSamplesFling() {
        return composite(
            sling(),
            slingScriptingThymeleaf(),
            slingCommonsMessaging(),
            slingCommonsMessagingMail(),
            slingExtensionModels(),
            slingExtensionQuery(),
            slingExtensionValidation(),
            slingAuthForm(),
            mavenBundle().groupId("org.apache.sling.samples").artifactId("org.apache.sling.samples.fling").version(versionResolver),
            newConfiguration("org.apache.sling.commons.messaging.mail.internal.SimpleMailBuilder")
                .put("from", "fling@sling.apache.org")
                .put("smtpUsername", "sling")
                .put("smtpPort", "8025")
                .put("subject", "message from fling")
                .put("smtpPassword", "fling")
                .put("smtpHostname", "localhost")
                .asOption(),
            newConfiguration("org.apache.sling.samples.fling.internal.WiserSmtpService")
                .put("smtpPort", "8025")
                .asOption()
        );
    }

    public static Option composumSling() {
        return composite(
            sling(),
            slingJcr(),
            slingScriptingJsp(),
            slingExtensionEvent(),
            mavenBundle().groupId("com.composum.sling.core").artifactId("composum-sling-core-commons").version(versionResolver),
            mavenBundle().groupId("com.composum.sling.core").artifactId("composum-sling-core-console").version(versionResolver),
            mavenBundle().groupId("com.composum.sling.core").artifactId("composum-sling-core-jslibs").version(versionResolver)
        );
    }

    public static Option slingLaunchpadOakTar(final String workingDirectory, final int httpPort) {
        final String slingHome = String.format("%s/sling", workingDirectory);
        final String repositoryHome = String.format("%s/repository", slingHome);
        final String localIndexDir = String.format("%s/index", repositoryHome);
        return composite(
            slingJcrOak(),
            slingLaunchpadOak(),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-segment").version(versionResolver),
            newConfiguration("org.apache.felix.http")
                .put("org.osgi.service.http.port", httpPort)
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStoreService")
                .put("repository.home", repositoryHome)
                .put("name", "Default NodeStore")
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProviderService")
                .put("localIndexDir", localIndexDir)
                .asOption()
        );
    }

}
