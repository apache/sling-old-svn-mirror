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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * The <code>RangeStreamingTest</code> tests the Range request header support
 * for the StreamRendererServlet introduced with SLING-1814
 */
public class RangeStreamingTest extends HttpTestBase {

    private String rootUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create the test nodes under a path that's specific to this class to
        // allow collisions
        final String path = "/" + getClass().getSimpleName() + "/"
            + System.currentTimeMillis();
        testClient.mkdirs(WEBDAV_BASE_URL, path);
        rootUrl = uploadTestScript(path, "rangestreaming.txt",
            "rangestreaming.txt");
    }

    @Override
    protected void tearDown() throws Exception {
        testClient.delete(rootUrl);
        super.tearDown();
    }

    public void test_full_range() throws IOException {
        GetMethod get = new GetMethod(rootUrl);
        int status = httpClient.executeMethod(get);
        assertEquals(200, status);
        assertEquals(79, get.getResponseContentLength());
    }

    public void test_wrong_ranges() throws IOException {
        GetMethod get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "unit 1-10"));
        int status = httpClient.executeMethod(get);
        assertEquals("Expect 416 for unsupported unit", 416, status);

        get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes 5"));
        status = httpClient.executeMethod(get);
        assertEquals("Expect 416 for missing dash", 416, status);

        get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes -x"));
        status = httpClient.executeMethod(get);
        assertEquals("Expect 416 for illegal negative number", 416, status);

        get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes -x"));
        status = httpClient.executeMethod(get);
        assertEquals("Expect 416 for illegal negative number", 416, status);

        get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes y-10"));
        status = httpClient.executeMethod(get);
        assertEquals("Expect 416 for unparseable number", 416, status);

        get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes 10-5"));
        status = httpClient.executeMethod(get);
        assertEquals("Expect 416 for end < start", 416, status);
    }

    public void test_single_range() throws IOException {
        GetMethod get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes 0-9"));
        int status = httpClient.executeMethod(get);
        assertEquals("Expect 206/PARTIAL CONTENT", 206, status);
        assertEquals(10, get.getResponseContentLength());
        assertEquals("The quick ", get.getResponseBodyAsString());

        get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes -10"));
        status = httpClient.executeMethod(get);
        assertEquals("Expect 206/PARTIAL CONTENT", 206, status);
        assertEquals(10, get.getResponseContentLength());
        assertEquals("corpus sic", get.getResponseBodyAsString());

        get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes 16-38"));
        status = httpClient.executeMethod(get);
        assertEquals("Expect 206/PARTIAL CONTENT", 206, status);
        assertEquals(23, get.getResponseContentLength());
        assertEquals("fox jumps over the lazy", get.getResponseBodyAsString());
    }

    public void test_multiple_ranges() throws IOException {
        GetMethod get = new GetMethod(rootUrl);
        get.setRequestHeader(new Header("Range", "bytes 0-9,-10"));
        int status = httpClient.executeMethod(get);
        assertEquals("Expect 206/PARTIAL CONTENT", 206, status);

        String contentType = get.getResponseHeader("Content-Type").getValue();
        assertTrue("Content Type must be multipart/byteranges",
            contentType.contains("multipart/byteranges"));
        String boundary = contentType.substring(contentType.indexOf("boundary=")
            + "boundary=".length());

        BufferedReader reader = new BufferedReader(new InputStreamReader(
            get.getResponseBodyAsStream()));

        String line = reader.readLine();
        while (!("--" + boundary).equals(line)) {
            line = reader.readLine();
        }

        assertEquals("Expected content to start with boundary",
            "--" + boundary, line);
        assertEntityHeaders(reader, "text/plain", "bytes 0-9/79");
        assertEquals("The quick ", reader.readLine());

        assertEquals("Expected content to start with boundary",
            "--" + boundary, reader.readLine());
        assertEntityHeaders(reader, "text/plain", "bytes 69-78/79");
        assertEquals("corpus sic", reader.readLine());

        char[] buf = new char[boundary.length() + 4];
        reader.read(buf);
        assertEquals("Expected content to start with boundary", "--" + boundary
            + "--", new String(buf));
    }

    private void assertEntityHeaders(final BufferedReader reader,
            final String expectedContentType, final String expectedRange)
            throws IOException {
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.toLowerCase();
            if (line.startsWith("content-type:")) {
                assertTrue("Wrong content type: " + line + "; expected="
                    + expectedContentType, line.contains(expectedContentType));
            } else if (line.startsWith("content-range:")) {
                assertTrue("Wrong content range: " + line + "; expected="
                    + expectedRange, line.contains(expectedRange));
            } else if (line.length() == 0) {
                return;
            }
        }

        // exhausted reader without reaching end of headers
        fail("Unexpected end of data");
    }
}
