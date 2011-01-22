/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.login;

import java.util.Arrays;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * Test of form generation from auth.core.
 */
public class FormGenerationTest extends HttpTestBase {

    public void testSelectorFormForDefaultResource() throws Exception {
        String content = getContent(HTTP_BASE_URL + "/system/sling/selector/login", CONTENT_TYPE_HTML,
                null, 200);

        assertTrue("form action is not correct.", content.contains("action=\"" + SERVLET_CONTEXT + "/j_security_check\""));
        assertTrue("sling image reference is not correct.",
                content.contains("<img border=\"0\" src=\"" + SERVLET_CONTEXT + "/sling-logo.png\"/>"));
    }

    public void testSelectorFormForRootResource() throws Exception {
        String resource = SERVLET_CONTEXT.equals("") ? "/" : SERVLET_CONTEXT;
        
        String content = getContent(HTTP_BASE_URL + "/system/sling/selector/login", CONTENT_TYPE_HTML,
                Arrays.asList(new NameValuePair("resource", resource)), 200);

        assertTrue("form action is not correct.", content.contains("action=\"" + SERVLET_CONTEXT + "/j_security_check\""));
        assertTrue("sling image reference is not correct.",
                content.contains("<img border=\"0\" src=\"" + SERVLET_CONTEXT + "/sling-logo.png\"/>"));
    }

    public void testSelectorFormForNonRootResource() throws Exception {
        String resource = SERVLET_CONTEXT + "/var/classes.json";
        
        String content = getContent(HTTP_BASE_URL + "/system/sling/selector/login", CONTENT_TYPE_HTML,
                Arrays.asList(new NameValuePair("resource", resource)), 200);

        assertTrue("form action is not correct.",
                content.contains("action=\"" + SERVLET_CONTEXT + "/var/classes.json/j_security_check\""));
        assertTrue("sling image reference is not correct.",
                content.contains("<img border=\"0\" src=\"" + SERVLET_CONTEXT + "/sling-logo.png\"/>"));
    }

}
