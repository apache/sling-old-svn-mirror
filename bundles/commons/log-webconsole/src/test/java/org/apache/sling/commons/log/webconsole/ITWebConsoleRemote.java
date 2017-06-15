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

package org.apache.sling.commons.log.webconsole;

import java.io.File;
import java.io.IOException;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.io.FilenameUtils;
import org.apache.sling.commons.log.webconsole.remote.WebConsoleTestActivator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.spi.DefaultExamSystem;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Constants;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public class ITWebConsoleRemote extends LogTestBase {

    private static final String PLUGIN_SUFFIX = "slinglog";

    private static final String PRINTER_SUFFIX = "status-slinglogs";

    private static TestContainer testContainer;

    private WebClient webClient;

    @Override
    protected Option addPaxExamSpecificOptions() {
        return null;
    }

    @Override
    protected Option addExtraOptions() {
        return composite(
            frameworkProperty("org.apache.sling.commons.log.configurationFile").value(
                FilenameUtils.concat(new File(".").getAbsolutePath(), "src/test/resources/test-webconsole-remote.xml")),
            createWebConsoleTestBundle()
        );
    }

    private Option createWebConsoleTestBundle() {
        TinyBundle bundle = bundle();
        for(Class c : WebConsoleTestActivator.BUNDLE_CLASS_NAMES){
            bundle.add(c);
        }

        bundle.set(Constants.BUNDLE_SYMBOLICNAME,"org.apache.sling.common.log.testbundle")
              .set(Constants.BUNDLE_ACTIVATOR , WebConsoleTestActivator.class.getName());
        return provision(bundle.build(withBnd()));

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
        String text = page.asText();

        //Filter name should be part of Filter table
        assertThat(text, containsString("WebConsoleTestTurboFilter"));

        //Console name should be part of console table
        assertThat(text, containsString("WebConsoleTestAppender"));

        //Should show file name testremote.log
        assertThat(text, containsString("testremote.log"));
    }

    @Test
    public void testPrinter() throws IOException {
        final HtmlPage page = webClient.getPage(prepareUrl(PRINTER_SUFFIX));
        String text = page.asText();

        //Should dump content of configured file testremote.log
        //with its name
        assertTrue(text.contains("testremote.log"));
    }

    @Test
    public void tailerHeader() throws Exception{
        Page page = webClient.getPage(prepareUrl("slinglog/tailer.txt?name=webconsoletest1.log"));
        String nosniffHeader = page.getWebResponse().getResponseHeaderValue("X-Content-Type-Options");
        assertEquals("nosniff", nosniffHeader);
    }

    @Test
    public void tailerGrep() throws Exception{
        TextPage page  = webClient.getPage(prepareUrl("slinglog/tailer.txt?name=FILE&tail=-1"));
        String text = page.getContent();

        assertThat(text, containsString(WebConsoleTestActivator.FOO_LOG));
        assertThat(text, containsString(WebConsoleTestActivator.BAR_LOG));

        page  = webClient.getPage(prepareUrl("slinglog/tailer.txt?name=FILE&tail=1000&grep="+WebConsoleTestActivator.FOO_LOG));
        text = page.getContent();

        //With grep pattern specified we should only see foo and not bar
        assertThat(text, containsString(WebConsoleTestActivator.FOO_LOG));
        assertThat(text, not(containsString(WebConsoleTestActivator.BAR_LOG)));
    }

    @AfterClass
    public static void tearDownClass() {
        if (testContainer != null) {
            testContainer.stop();
            testContainer = null;
        }
    }

    private static String prepareUrl(String suffix) {
        return String.format("http://localhost:%s/system/console/%s", LogTestBase.getServerPort(), suffix);
    }
}
