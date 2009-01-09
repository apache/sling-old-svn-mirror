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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import java.io.IOException;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;


public class PostServletNopTest extends HttpTestBase {

    private static final String TEST_URL = HTTP_BASE_URL + "/"
        + System.currentTimeMillis();

    public void testDefault() throws IOException {
        post(TEST_URL, null, 200);
    }

    public void testStatus200() throws IOException {
        post(TEST_URL, 200);
    }

    public void testStatus708() throws IOException {
        post(TEST_URL, 708);
    }

    // request status <100, expect default 200
    public void testStatus88() throws IOException {
        post(TEST_URL, "88", 200);
    }

    // request status >999, expect default 200
    public void testStatus1234() throws IOException {
        post(TEST_URL, "1234", 200);
    }

    // request non-numeric status, expect default 200
    public void testStatusNonNumeric() throws IOException {
        post(TEST_URL, "nonumber", 200);
    }

    private void post(String url, int status) throws IOException {
        post(url, String.valueOf(status), status);
    }

    private void post(String url, String code, int expectedStatus)
            throws IOException {
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);

        post.addParameter(":operation", "nop");

        if (code != null) {
            post.addParameter(":nopstatus", code);
        }

        int actualStatus = httpClient.executeMethod(post);
        assertEquals(expectedStatus, actualStatus);
    }
}