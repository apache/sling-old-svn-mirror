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

import javax.inject.Inject;
import javax.script.ScriptEngineFactory;

import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.launchpad.karaf.testing.KarafTestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.util.Filter;
import org.thymeleaf.ITemplateEngine;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

public abstract class ThymeleafTestSupport extends KarafTestSupport {

    @Inject
    protected SlingRequestProcessor slingRequestProcessor;

    @Inject
    protected AuthenticationSupport authenticationSupport;

    @Inject
    @Filter(value = "(names=thymeleaf)")
    protected ScriptEngineFactory scriptEngineFactory;

    @Inject
    protected ITemplateEngine templateEngine;

    @Configuration
    public Option[] configuration() {
        final String filename = System.getProperty("bundle.filename");
        final File file = new File(filename);
        return OptionUtils.combine(baseConfiguration(),
            editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j.rootLogger", "ERROR, out, sift, osgi:*"),
            bundle(file.toURI().toString()),
            addSlingFeatures(
                "sling-launchpad-oak-tar",
                "sling-scripting",
                "sling-extension-i18n",
                "sling-extension-models",
                "sling-scripting-jsp"
            ),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.thymeleaf").versionAsInProject(),
            mavenBundle().groupId("org.javassist").artifactId("javassist").versionAsInProject(),
            mavenBundle().groupId("org.jsoup").artifactId("jsoup").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject()
        );
    }

}
