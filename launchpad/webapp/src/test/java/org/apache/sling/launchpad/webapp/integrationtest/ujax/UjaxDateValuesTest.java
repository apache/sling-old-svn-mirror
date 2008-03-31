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
package org.apache.sling.launchpad.webapp.integrationtest.ujax;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.sling.launchpad.webapp.integrationtest.HttpTestBase;
import org.apache.sling.servlets.post.impl.UjaxPostServlet;

/**
 *  checks if the date parsing for non jcr-dates works.
 */

public class UjaxDateValuesTest extends HttpTestBase {

    public static final String TEST_BASE_PATH = "/ujax-tests";

    // TODO: the commented formats do not work beacuse of SLING-242
    //       the + of the timezone offset is stripped by sling

    private final SimpleDateFormat[] testFormats = new SimpleDateFormat[]{
        new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US),
        new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    };

    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    private void doDateTest(String expected, String input)
            throws IOException {
        final Map<String, String> props = new HashMap<String, String>();
        props.put("someDate", input);
        props.put("someDate@TypeHint", "Date");

        final String createdNodeUrl = testClient.createNode(postUrl + UjaxPostServlet.DEFAULT_CREATE_SUFFIX, props);
        String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        // default behaviour writes empty string
        assertJavascript(expected, content, "out.println(data.someDate)");
    }

    public void testDateValues() throws IOException {
        SimpleDateFormat ecmaFmt = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US);
        Date now = new Date();
        String nowStr = ecmaFmt.format(now);
        for (SimpleDateFormat fmt: testFormats) {
            String testStr = fmt.format(now);
            doDateTest(nowStr, testStr);
        }
    }

}