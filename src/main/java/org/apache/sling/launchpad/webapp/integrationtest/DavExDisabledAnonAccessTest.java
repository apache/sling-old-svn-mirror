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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.util.Collections;

import org.apache.commons.httpclient.NameValuePair;

/**
 * Test that the repository is accessible via DavEx when anonymous access is
 * disabled.
 */
public class DavExDisabledAnonAccessTest extends DavExIntegrationTest {

    private static String ANON_SERVLET_URL = HTTP_BASE_URL + "/testing/AnonymousAccessConfigServlet.txt";

    @Override
    protected void configureServerAfterTest() throws Exception {
        assertPostStatus(ANON_SERVLET_URL, 200, Collections.singletonList(new NameValuePair("action", "enable")),
                "Unable to enable anonymous access");

    }

    @Override
    protected void configureServerBeforeTest() throws Exception {
        assertPostStatus(ANON_SERVLET_URL, 200, Collections.singletonList(new NameValuePair("action", "disable")),
                "Unable to disable anonymous access");
    }
}
