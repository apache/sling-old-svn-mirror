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
package org.apache.sling.servlets.get.impl;

import java.util.Collections;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;

public class RedirectServletTest extends TestCase {

    static final String TEST_SCHEME = "http";

    static final String TEST_HOST = "the.host.any";

    static final int TEST_PORT = 80;

    static final String TEST_PREFIX = TEST_SCHEME + "://" + TEST_HOST;

    public void testToAbsoluteURI() {
        final String http = "http";
        final String https = "https";
        final String scheme = "test";
        final String host = TEST_HOST;
        final int port80 = 80;
        final int port443 = 443;
        final int portAny = 9999;
        final int portNone = -1;
        final String target = "/target";

        // regular building without default ports
        assertEquals(http + "://" + host + ":" + portAny + target,
            RedirectServlet.toAbsoluteUri(http, host, portAny, target));
        assertEquals(https + "://" + host + ":" + portAny + target,
            RedirectServlet.toAbsoluteUri(https, host, portAny, target));
        assertEquals(scheme + "://" + host + ":" + portAny + target,
            RedirectServlet.toAbsoluteUri(scheme, host, portAny, target));

        // building with default ports
        assertEquals(http + "://" + host + target,
            RedirectServlet.toAbsoluteUri(http, host, port80, target));
        assertEquals(https + "://" + host + target,
            RedirectServlet.toAbsoluteUri(https, host, port443, target));
        assertEquals(scheme + "://" + host + ":" + port80 + target,
            RedirectServlet.toAbsoluteUri(scheme, host, port80, target));

        // building without ports
        assertEquals(http + "://" + host + target,
            RedirectServlet.toAbsoluteUri(http, host, portNone, target));
        assertEquals(https + "://" + host + target,
            RedirectServlet.toAbsoluteUri(https, host, portNone, target));
        assertEquals(scheme + "://" + host + target,
            RedirectServlet.toAbsoluteUri(scheme, host, portNone, target));
    }

    public void testGetStatus() {
        final int found = HttpServletResponse.SC_FOUND;
        final int valid = 768;
        final int invalidLow = 77;
        final int invalidHigh = 1234;
        final int min = 100;
        final int max = 999;

        assertStatus(found, -2);
        assertStatus(found, -1);
        assertStatus(found, invalidLow);
        assertStatus(found, invalidHigh);

        assertStatus(valid, valid);
        assertStatus(min, min);
        assertStatus(max, max);
    }

    public void testSameParent() {
        String base = "/a";
        String target = "/b";
        assertEqualsUri("/b", toRedirect(base, target), false);

        base = "/";
        target = "/a";
        assertEqualsUri("/a", toRedirect(base, target), false);

        base = "/a/b/c";
        target = "/a/b/d";
        assertEqualsUri("/a/b/d", toRedirect(base, target), false);
    }

    public void testTrailingSlash() {
        String base = "/a/b/c/";
        String target = "/a/b/c.html";
        assertEqualsUri("/a/b/c.html", toRedirect(base, target), false);
    }

    public void testCommonAncestor() {
        String base = "/a/b/c/d";
        String target = "/a/b/x/y";
        assertEqualsUri("/a/b/x/y", toRedirect(base, target), false);
    }

    public void testChild() {
        String base = "/a.html";
        String target = "/a/b.html";
        assertEqualsUri("/a/b.html", toRedirect(base, target), false);

        base = "/a";
        target = "/a/b.html";
        assertEqualsUri("/a/b.html", toRedirect(base, target), false);

        base = "/a";
        target = "/a/b";
        assertEqualsUri("/a/b", toRedirect(base, target), false);

        base = "/a.html";
        target = "/a/b/c.html";
        assertEqualsUri("/a/b/c.html", toRedirect(base, target), false);

        base = "/a";
        target = "/a/b/c.html";
        assertEqualsUri("/a/b/c.html", toRedirect(base, target), false);

        base = "/a";
        target = "/a/b/c";
        assertEqualsUri("/a/b/c", toRedirect(base, target), false);
    }

    public void testChildNonRoot() {
        String base = "/x/a.html";
        String target = "/x/a/b.html";
        assertEqualsUri("/x/a/b.html", toRedirect(base, target), false);

        base = "/x/a";
        target = "/x/a/b.html";
        assertEqualsUri("/x/a/b.html", toRedirect(base, target), false);

        base = "/x/a";
        target = "/x/a/b";
        assertEqualsUri("/x/a/b", toRedirect(base, target), false);

        base = "/x/a.html";
        target = "/x/a/b/c.html";
        assertEqualsUri("/x/a/b/c.html", toRedirect(base, target), false);

        base = "/x/a";
        target = "/x/a/b/c.html";
        assertEqualsUri("/x/a/b/c.html", toRedirect(base, target), false);

        base = "/x/a";
        target = "/x/a/b/c";
        assertEqualsUri("/x/a/b/c", toRedirect(base, target), false);
    }

    public void testChildRelative() {
        String base = "/a";
        String target = "b.html";
        assertEqualsUri("/a/b.html", toRedirect(base, target), false);

        base = "/a";
        target = "b";
        assertEqualsUri("/a/b", toRedirect(base, target), false);

        base = "/a";
        target = "b/c.html";
        assertEqualsUri("/a/b/c.html", toRedirect(base, target), false);

        base = "/a";
        target = "b/c";
        assertEqualsUri("/a/b/c", toRedirect(base, target), false);
    }

    public void testChildNonRootRelative() {
        String base = "/x/a";
        String target = "b.html";
        assertEqualsUri("/x/a/b.html", toRedirect(base, target), false);

        base = "/x/a";
        target = "b";
        assertEqualsUri("/x/a/b", toRedirect(base, target), false);

        base = "/x/a";
        target = "b/c.html";
        assertEqualsUri("/x/a/b/c.html", toRedirect(base, target), false);

        base = "/x/a";
        target = "b/c";
        assertEqualsUri("/x/a/b/c", toRedirect(base, target), false);
    }

    public void testUnCommon() {
        String base = "/a/b/c/d";
        String target = "/w/x/y/z";
        assertEqualsUri("/w/x/y/z", toRedirect(base, target), false);
    }

    public void testSibbling() {
        String base = "/a/b";
        String target0 = "../y/z";
        assertEqualsUri("/a/y/z", toRedirect(base, target0), false);

        String target1 = "../../y/z";
        assertEqualsUri("/y/z", toRedirect(base, target1), false);

        String target2 = "../../../y/z";
        assertEqualsUri(base + "/" + target2, toRedirect(base, target2), false);
    }

    public void testSelectorsEtc() {
        assertEquals(null, null, null, null);

        assertEquals(null, "html", null, null);

        assertEquals("print", "html", null, null);

        assertEquals("print.a4", "html", null, null);

        assertEquals(null, "html", "/suffix.pdf", null);

        assertEquals(null, "html", null, "xy=1");

        assertEquals(null, "html", "/suffix.pdf", "xy=1");

        assertEquals("print.a4", "html", "/suffix.pdf", "xy=1");
    }

    public void testEmptyPath() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/",
            null, null, null, null, "", TEST_SCHEME, TEST_HOST, TEST_PORT, "/webapp");
        request.setResourceResolver(new MockResourceResolver());
        String path = RedirectServlet.toRedirectPath("/index.html", request);
        assertEqualsUri("/webapp/index.html", path, false);

        request = new MockSlingHttpServletRequest("/", null, null, null, null,
            "/", TEST_SCHEME, TEST_HOST, TEST_PORT, "/webapp");
        request.setResourceResolver(new MockResourceResolver());
        path = RedirectServlet.toRedirectPath("/index.html", request);
        assertEqualsUri("/webapp/index.html", path, false);
    }

    //---------- Helper

    private static void assertEquals(
            String selectors, String extension, String suffix,
            String queryString) {
        final String basePath = "/a/b/c";
        final String targetPath = "/a/b/d";
        String expected = "/a/b/d";

        if (selectors != null) {
            expected += "." + selectors;
        }
        if (extension != null) {
            expected += "." + extension;
        }
        if (suffix != null) {
            expected += suffix;
        }
        if (queryString != null) {
            expected += "?" + queryString;
        }

        String actual = toRedirect(basePath, selectors, extension, suffix,
            queryString, targetPath);

        assertEqualsUri(expected, actual, false);
    }

    private static void assertEqualsUri(String expected, String actual, boolean isAbsolute) {
        if (isAbsolute) {
            assertEquals(expected, actual);
        } else {
            assertEquals(expected, actual);
        }
    }

    private static void assertStatus(final int expectedStatus,
            final int testStatus) {
        final ValueMap valueMap;
        if (testStatus == -2) {
            valueMap = null;
        } else if (testStatus == -1) {
            valueMap = new ValueMapDecorator(new HashMap<String, Object>());
        } else {
            valueMap = new ValueMapDecorator(Collections.singletonMap(
                RedirectServlet.STATUS_PROP, (Object) testStatus));
        }

        final int actualStatus = RedirectServlet.getStatus(valueMap);

        assertEquals(expectedStatus, actualStatus);
    }

    private static String toRedirect(String basePath, String targetPath) {
        return toRedirect(basePath, null, null, null, null, targetPath);
    }

    private static String toRedirect(String basePath, String selectors,
            String extension, String suffix, String queryString,
            String targetPath) {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
            basePath, selectors, extension, suffix, queryString,
            basePath, TEST_SCHEME, TEST_HOST, TEST_PORT, "");
        request.setResourceResolver(new MockResourceResolver());
        return RedirectServlet.toRedirectPath(targetPath, request);
    }

}
