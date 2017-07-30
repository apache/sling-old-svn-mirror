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
package org.apache.sling.i18n.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.cm.ConfigurationAdminOptions;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ResourceBundleProviderIT {

    private static final String BUNDLE_JAR_SYS_PROP = "bundle.filename";

    /** The property containing the build directory. */
    private static final String SYS_PROP_BUILD_DIR = "bundle.build.dir";

    private static final String DEFAULT_BUILD_DIR = "target";

    private static final String PORT_CONFIG = "org.osgi.service.http.port";

    public static final int RETRY_TIMEOUT_MSEC = 50000;
    public static final String MSG_KEY1 = "foo";
    public static final String MSG_KEY2 = "foo2";

    @Inject
    private SlingRepository repository;

    @Inject
    private ResourceBundleProvider resourceBundleProvider;

    private Session session;
    private Node i18nRoot;
    private Node deRoot;
    private Node deDeRoot;
    private Node frRoot;
    private Node enRoot;

    @Configuration
    public Option[] config() {
        final String buildDir = System.getProperty(SYS_PROP_BUILD_DIR, DEFAULT_BUILD_DIR);
        final String bundleFileName = System.getProperty( BUNDLE_JAR_SYS_PROP );
        final File bundleFile = new File( bundleFileName );
        if ( !bundleFile.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from bundle file " + bundleFileName + " specified in the "
                + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        String localRepo = System.getProperty("maven.repo.local", "");

        final String jackrabbitVersion = "2.13.1";
        final String oakVersion = "1.5.7";

        final String slingHome = new File(buildDir + File.separatorChar + "sling_" + System.currentTimeMillis()).getAbsolutePath();

        return options(
                frameworkProperty("sling.home").value(slingHome),
                frameworkProperty("repository.home").value(slingHome + File.separatorChar + "repository"),
                when( localRepo.length() > 0 ).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
                ),
                when( System.getProperty(PORT_CONFIG) != null ).useOptions(
                        systemProperty(PORT_CONFIG).value(System.getProperty(PORT_CONFIG))),
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
                ConfigurationAdminOptions.newConfiguration("org.apache.sling.resourceresolver.impl.observation.OsgiObservationBridge")
                    .create(true)
                    .put("enabled", true)
                    .asOption(),
                ConfigurationAdminOptions.factoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended")
                    .create(true)
                    .put("user.mapping", new String[]{"org.apache.sling.i18n=sling-i18n"})
                    .asOption(),
                ConfigurationAdminOptions.newConfiguration("org.apache.sling.jcr.repoinit.impl.RepositoryInitializer")
                    .put("references", new String[]{references()})
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
                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.4.4"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.4"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.10"),
                mavenBundle("org.apache.felix", "org.apache.felix.inventory", "1.0.4"),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.1.2"),

                // sling
                mavenBundle("org.apache.sling", "org.apache.sling.settings", "1.3.8"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.3.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.mime", "2.1.8"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.classloader", "1.3.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.scheduler", "2.4.14"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.threads", "3.2.4"),

                mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.3.12"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.api", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.commons", "1.0.20"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.standalone", "1.0.2"),

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
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.repoinit", "1.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.repoinit.parser", "1.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.provisioning.model", "1.4.2"),

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
                mavenBundle("org.apache.sling", "org.apache.sling.commons.johnzon", "1.0.0"),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.1.2"),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.1.2"),

                junitBundles(),

                CoreOptions.bundle( bundleFile.toURI().toString() )
           );
    }

    static abstract class Retry {
        Retry(int timeoutMsec) {
            final long timeout = System.currentTimeMillis() + timeoutMsec;
            Throwable lastT = null;
            while(System.currentTimeMillis() < timeout) {
                try {
                    lastT = null;
                    exec();
                    break;
                } catch(Throwable t) {
                    lastT = t;
                }
            }

            if(lastT != null) {
                fail("Failed after " + timeoutMsec + " msec: " + lastT);
            }
        }

        protected abstract void exec() throws Exception;
    }

    @Before
    public void setup() throws RepositoryException {
        session = repository.loginAdministrative(null);
        final Node root = session.getRootNode();
        final Node libs;
        if(root.hasNode("libs")) {
           libs = root.getNode("libs");
        } else {
           libs = root.addNode("libs", "nt:unstructured");
        }
        i18nRoot = libs.addNode("i18n", "nt:unstructured");
        deRoot = addLanguageNode(i18nRoot, "de");
        frRoot = addLanguageNode(i18nRoot, "fr");
        deDeRoot = addLanguageNode(i18nRoot, "de_DE");
        enRoot = addLanguageNode(i18nRoot, "en");
        session.save();
    }

    @After
    public void cleanup() throws RepositoryException {
        i18nRoot.remove();
        session.save();
        session.logout();
    }

    private Node addLanguageNode(Node parent, String language) throws RepositoryException {
        final Node child = parent.addNode(language, "nt:folder");
        child.addMixin("mix:language");
        child.setProperty("jcr:language", language);
        return child;
    }

    private void assertMessages(final String key, final String deMessage, final String deDeMessage, final String frMessage) {
        new Retry(RETRY_TIMEOUT_MSEC) {
            @Override
            protected void exec() {
                {
                    final ResourceBundle deDE = resourceBundleProvider.getResourceBundle(Locale.GERMANY); // this is the resource bundle for de_DE
                    assertNotNull(deDE);
                    assertEquals(deDeMessage, deDE.getString(key));
                }
                {
                    final ResourceBundle de = resourceBundleProvider.getResourceBundle(Locale.GERMAN);
                    assertNotNull(de);
                    assertEquals(deMessage, de.getString(key));
                }
                {
                    final ResourceBundle fr = resourceBundleProvider.getResourceBundle(Locale.FRENCH);
                    assertNotNull(fr);
                    assertEquals(frMessage, fr.getString(key));
                }
            }
        };
    }

    private void setMessage(final Node rootNode, final String key, final String message) throws RepositoryException {
        final String nodeName = "node_" + key;
        final Node node;
        if ( rootNode.hasNode(nodeName) ) {
            node = rootNode.getNode(nodeName);
        } else {
            node = rootNode.addNode(nodeName, "sling:MessageEntry");
        }
        node.setProperty("sling:key", key);
        node.setProperty("sling:message", message);
    }

    @Test
    public void testChangesDetection() throws RepositoryException {
        // set a key which is only available in the en dictionary
        setMessage(enRoot, MSG_KEY2, "EN_message");
        session.save();

        // since "en" is the fallback for all other resource bundle, the value from "en" must be exposed
        assertMessages(MSG_KEY2, "EN_message", "EN_message", "EN_message");

        setMessage(deRoot, MSG_KEY1, "DE_message");
        setMessage(frRoot, MSG_KEY1, "FR_message");
        session.save();
        assertMessages(MSG_KEY1, "DE_message", "DE_message", "FR_message");

        setMessage(deRoot, MSG_KEY1, "DE_changed");
        setMessage(frRoot, MSG_KEY1, "FR_changed");
        session.save();
        assertMessages(MSG_KEY1, "DE_changed", "DE_changed", "FR_changed");

        setMessage(deRoot, MSG_KEY1, "DE_message");
        setMessage(deDeRoot, MSG_KEY1, "DE_DE_message");
        setMessage(frRoot, MSG_KEY1, "FR_message");
        session.save();
        assertMessages(MSG_KEY1, "DE_message", "DE_DE_message", "FR_message");

        // now change a key which is only available in the "en" dictionary
        setMessage(enRoot, MSG_KEY2, "EN_changed");
        session.save();
        assertMessages(MSG_KEY2, "EN_changed", "EN_changed", "EN_changed");
    }

    private String references() {
        try {
            String repoInitUrl = getClass().getResource("/repoinit.txt").toURI().toString();
            return String.format("raw:%s", repoInitUrl);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to compute repoinit references", e);
        }
    }
}
