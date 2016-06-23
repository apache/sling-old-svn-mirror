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
package org.apache.sling.scripting.thymeleaf.it;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Dictionary;

import javax.inject.Inject;
import javax.script.ScriptEngineFactory;

import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.engine.SlingRequestProcessor;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.thymeleaf.ITemplateEngine;

import static org.apache.sling.testing.paxexam.SlingOptions.slingExtensionI18n;
import static org.apache.sling.testing.paxexam.SlingOptions.slingExtensionModels;
import static org.apache.sling.testing.paxexam.SlingOptions.slingJcrOak;
import static org.apache.sling.testing.paxexam.SlingOptions.slingLaunchpadOak;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScripting;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScriptingJsp;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

public abstract class ThymeleafTestSupport {

    @Inject
    protected ServletResolver servletResolver;

    @Inject
    protected SlingRequestProcessor slingRequestProcessor;

    @Inject
    protected AuthenticationSupport authenticationSupport;

    @Inject
    protected HttpService httpService;

    @Inject
    @Filter(value = "(names=thymeleaf)")
    protected ScriptEngineFactory scriptEngineFactory;

    @Inject
    protected ITemplateEngine templateEngine;

    @Inject
    protected ConfigurationAdmin configurationAdmin;

    protected static synchronized int findFreePort() {
        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            final int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected int httpPort() throws IOException {
        final Dictionary<String, Object> properties = configurationAdmin.getConfiguration("org.apache.felix.http").getProperties();
        return Integer.parseInt(properties.get("org.osgi.service.http.port").toString());
    }

    @Configuration
    public Option[] configuration() {
        final String workingDirectory = String.format("target/paxexam/%s", getClass().getSimpleName());
        final String filename = System.getProperty("bundle.filename");
        final File file = new File(filename);
        return new Option[]{
            keepCaches(),
            workingDirectory(workingDirectory),
            launchpad(workingDirectory),
            // test app bundle
            bundle(file.toURI().toString()),
            // Thymeleaf
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.thymeleaf").versionAsInProject(),
            mavenBundle().groupId("org.javassist").artifactId("javassist").versionAsInProject(),
            // testing
            mavenBundle().groupId("org.jsoup").artifactId("jsoup").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject(),
            junitBundles()
        };
    }

    protected static Option launchpad(final String workingDirectory) {
        final int httpPort = findFreePort();
        final String slingHome = String.format("%s/sling", workingDirectory);
        final String repositoryHome = String.format("%s/repository", slingHome);
        final String localIndexDir = String.format("%s/index", repositoryHome);
        return composite(
            slingJcrOak(), // TODO if slingJcrOak() is called elsewhere, ResourceResolverFactory will not be created
            slingLaunchpadOak(),
            slingExtensionI18n(),
            slingExtensionModels(),
            slingScripting(),
            slingScriptingJsp(),
            newConfiguration("org.apache.felix.http")
                .put("org.osgi.service.http.port", httpPort)
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStoreService")
                .put("repository.home", repositoryHome)
                .put("name", "Default NodeStore")
                .asOption(),
            newConfiguration("org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProviderService")
                .put("localIndexDir", localIndexDir)
                .asOption(),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-segment").version("1.5.3")
        );
    }

}
