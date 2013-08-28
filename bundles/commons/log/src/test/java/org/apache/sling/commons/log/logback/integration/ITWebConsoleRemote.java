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

package org.apache.sling.commons.log.logback.integration;

import java.io.IOException;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.apache.sling.commons.log.logback.integration.LogTestBase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.spi.DefaultExamSystem;
import org.ops4j.pax.exam.spi.PaxExamRuntime;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

public class ITWebConsoleRemote extends LogTestBase {

    private static final String PLUGIN_SUFFIX = "slinglog";

    private static final String PRINTER_SUFFIX = "config/slinglogs.nfo";

    private static TestContainer testContainer;

    private WebClient webClient;

    @Override
    protected Option addPaxExamSpecificOptions() {
        return null;
    }

    @Override
    protected Option addExtraOptions() {
        return composite(webSupport(),
            mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.http.jetty").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.webconsole").versionAsInProject(),
            mavenBundle("commons-io", "commons-io").versionAsInProject(),
            wrappedBundle(mavenBundle("commons-fileupload", "commons-fileupload").versionAsInProject()),
            wrappedBundle(mavenBundle("org.json", "json").versionAsInProject()));
    }

    @Before
    public void setUp() throws IOException {
        // Had to use a @Before instead of @BeforeClass as that requires a
        // static method
        if (testContainer == null) {
            ExamSystem system = DefaultExamSystem.create(config());
            testContainer = PaxExamRuntime.createContainer(system);
            testContainer.start();
        }
    }

    @Before
    public void prepareWebClient() {
        webClient = new WebClient();
        ((DefaultCredentialsProvider) webClient.getCredentialsProvider()).addCredentials("admin", "admin");
    }

    @Test
    public void testWebConsolePlugin() throws IOException {
        final HtmlPage page = webClient.getPage(prepareUrl(PLUGIN_SUFFIX));
    }

    @Test
    public void testPrinter() throws IOException {
        final HtmlPage page = webClient.getPage(prepareUrl(PRINTER_SUFFIX));
    }

    @AfterClass
    public static void tearDownClass() {
        if (testContainer != null) {
            testContainer.stop();
            testContainer = null;
        }
    }

    private static String prepareUrl(String suffix) {
        return String.format("http://localhost:%s/system/console/%s", getServerPort(), suffix);
    }
}
