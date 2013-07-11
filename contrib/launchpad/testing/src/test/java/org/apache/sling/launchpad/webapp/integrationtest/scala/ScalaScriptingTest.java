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
package org.apache.sling.launchpad.webapp.integrationtest.scala;

import java.io.IOException;

import junit.framework.AssertionFailedError;

import org.apache.sling.commons.testing.integration.HttpTestBase;

public class ScalaScriptingTest extends HttpTestBase {

    /*
     * FIXME - workaround
     * 
     * The first request on a fresh Sling instance always fails with javax.script.ScriptException: ERROR : error while
     * loading ScriptHelper, class file 'org/apache/sling/scripting/core/ScriptHelper.class' is broken (class
     * org.osgi.framework.BundleContext not found.)
     * 
     * For the time being this is a known issue. Until this is fixed this method is here to make tests pass
     * 
     * @throws IOException
     */
    public void setUp() throws Exception {
        super.setUp();

        String url = HTTP_BASE_URL + "/content/helloworld.html";
        try {
            getContent(url, CONTENT_TYPE_HTML, null);
        } catch (AssertionFailedError e) {
            // expected
        }
    }

    public void testHelloWorldApp() throws IOException {
        String url = HTTP_BASE_URL + "/content/helloworld.html";
        String content = getContent(url, CONTENT_TYPE_HTML);

        assertTrue(content.contains("<h1>Hello World</h1>"));
        assertTrue(content.contains("My path is /content/helloworld <br></br>"));
    }

    public void testForumApp() throws IOException {
        String url = HTTP_BASE_URL + "/content/forum.html";
        String content = getContent(url, CONTENT_TYPE_HTML);

        assertTrue(content.contains("Welcome to the Scala for Sling forum"));
        assertTrue(content.contains("(<a href=\"/content/forum/scala4sling.thread.html\">show thread</a>)"));
        assertTrue(content.contains("(<a href=\"/content/forum/sling.thread.html\">show thread</a>)"));
        assertTrue(content.contains("<img src=\"/content/forum/sling/logo/jcr:content\"></img>"));
    }

}
