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
package org.apache.sling.scripting.thymeleaf.it.tests;

import javax.inject.Inject;
import javax.script.ScriptEngineFactory;

import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.scripting.thymeleaf.it.app.Activator;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpService;
import org.thymeleaf.ITemplateEngine;

import static org.apache.sling.testing.paxexam.SlingOptions.slingExtensionI18n;
import static org.apache.sling.testing.paxexam.SlingOptions.slingExtensionModels;
import static org.apache.sling.testing.paxexam.SlingOptions.slingInstallerProviderJcr;
import static org.apache.sling.testing.paxexam.SlingOptions.slingLaunchpadOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScripting;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScriptingJsp;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

public abstract class ThymeleafTestSupport extends TestSupport {

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
    protected ServiceUserMapped serviceUserMapped;

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),
            launchpad(),
            // Sling Scripting Thymeleaf
            testBundle("bundle.filename"),
            mavenBundle().groupId("org.attoparser").artifactId("attoparser").versionAsInProject(),
            mavenBundle().groupId("org.unbescape").artifactId("unbescape").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.ognl").versionAsInProject(),
            mavenBundle().groupId("org.javassist").artifactId("javassist").versionAsInProject(),
            // testing
            mavenBundle().groupId("org.jsoup").artifactId("jsoup").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject(),
            junitBundles(),
            logging()
        };
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(final TestProbeBuilder testProbeBuilder) {
        testProbeBuilder.setHeader(Constants.BUNDLE_ACTIVATOR, Activator.class.getName());
        testProbeBuilder.setHeader(Constants.BUNDLE_SYMBOLICNAME, "org.apache.sling.scripting.thymeleaf.it.app");
        testProbeBuilder.setHeader(Constants.EXPORT_PACKAGE, "org.apache.sling.scripting.thymeleaf.it.app");
        testProbeBuilder.setHeader("Sling-Model-Packages", "org.apache.sling.scripting.thymeleaf.it.app");
        testProbeBuilder.setHeader("Sling-Initial-Content", String.join(",",
            "apps/jsp;path:=/apps/jsp;overwrite:=true;uninstall:=true",
            "apps/thymeleaf;path:=/apps/thymeleaf;overwrite:=true;uninstall:=true",
            "content;path:=/content;overwrite:=true;uninstall:=true"
        ));
        return testProbeBuilder;
    }

    protected Option launchpad() {
        final int httpPort = findFreePort();
        final String workingDirectory = workingDirectory();
        return composite(
            slingLaunchpadOakTar(workingDirectory, httpPort),
            slingExtensionI18n(),
            slingExtensionModels(),
            slingInstallerProviderJcr(),
            slingScripting(),
            slingScriptingJsp()
        );
    }

    protected Option logging() {
        final String filename = String.format("file:%s/src/test/resources/logback.xml", PathUtils.getBaseDir());
        return composite(
            systemProperty("logback.configurationFile").value(filename),
            mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").version("1.7.21"),
            mavenBundle().groupId("org.slf4j").artifactId("jcl-over-slf4j").version("1.7.21"),
            mavenBundle().groupId("ch.qos.logback").artifactId("logback-core").version("1.1.7"),
            mavenBundle().groupId("ch.qos.logback").artifactId("logback-classic").version("1.1.7")
        );
    }

}
