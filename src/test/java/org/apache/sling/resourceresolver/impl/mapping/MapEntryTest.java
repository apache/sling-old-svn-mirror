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
package org.apache.sling.resourceresolver.impl.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import junit.framework.TestCase;

import org.apache.sling.resourceresolver.impl.mapping.MapEntry;
import org.junit.Test;

public class MapEntryTest {

    @Test public void test_to_url_http_80() {
        assertEqualUri("http://sling.apache.org", "http/sling.apache.org.80");
        assertEqualUri("http://sling.apache.org/", "http/sling.apache.org.80/");
        assertEqualUri("http://sling.apache.org/site/index.html",
            "http/sling.apache.org.80/site/index.html");
    }

    @Test public void test_to_url_https_443() {
        assertEqualUri("https://sling.apache.org", "https/sling.apache.org.443");
        assertEqualUri("https://sling.apache.org/",
            "https/sling.apache.org.443/");
        assertEqualUri("https://sling.apache.org/site/index.html",
            "https/sling.apache.org.443/site/index.html");
    }

    @Test public void test_to_url_any_999() {
        // http with arbitrary port
        assertEqualUri("http://sling.apache.org:123",
            "http/sling.apache.org.123");
        assertEqualUri("http://sling.apache.org:456/",
            "http/sling.apache.org.456/");
        assertEqualUri("http://sling.apache.org:456/site/index.html",
            "http/sling.apache.org.456/site/index.html");

        // https with arbitrary port
        assertEqualUri("https://sling.apache.org:987",
            "https/sling.apache.org.987");
        assertEqualUri("https://sling.apache.org:654/",
            "https/sling.apache.org.654/");
        assertEqualUri("https://sling.apache.org:321/site/index.html",
            "https/sling.apache.org.321/site/index.html");

        // any scheme with arbitrary port
        assertEqualUri("gurk://sling.apache.org:987",
            "gurk/sling.apache.org.987");
        assertEqualUri("gurk://sling.apache.org:654/",
            "gurk/sling.apache.org.654/");
        assertEqualUri("gurk://sling.apache.org:321/site/index.html",
            "gurk/sling.apache.org.321/site/index.html");
    }

    @Test public void test_to_url_any() {
        // http without port
        assertEqualUri("http://sling.apache.org", "http/sling.apache.org");
        assertEqualUri("http://sling.apache.org/", "http/sling.apache.org/");
        assertEqualUri("http://sling.apache.org/site/index.html",
            "http/sling.apache.org/site/index.html");

        // https without port
        assertEqualUri("https://sling.apache.org", "https/sling.apache.org");
        assertEqualUri("https://sling.apache.org/", "https/sling.apache.org/");
        assertEqualUri("https://sling.apache.org/site/index.html",
            "https/sling.apache.org/site/index.html");

        // any scheme without port
        assertEqualUri("gurk://sling.apache.org", "gurk/sling.apache.org");
        assertEqualUri("gurk://sling.apache.org/", "gurk/sling.apache.org/");
        assertEqualUri("gurk://sling.apache.org/site/index.html",
            "gurk/sling.apache.org/site/index.html");
    }

    @Test public void test_fixUriPath() {
        // http without port
        assertEqualUriPath("http/sling.apache.org.80", "http/sling.apache.org");
        assertEqualUriPath("http/sling.apache.org.80/",
            "http/sling.apache.org/");
        assertEqualUriPath("http/sling.apache.org.80/site/index.html",
            "http/sling.apache.org/site/index.html");

        // http with port
        assertEqualUriPath("http/sling.apache.org.80",
            "http/sling.apache.org.80");
        assertEqualUriPath("http/sling.apache.org.80/",
            "http/sling.apache.org.80/");
        assertEqualUriPath("http/sling.apache.org.80/site/index.html",
            "http/sling.apache.org.80/site/index.html");

        // https without port
        assertEqualUriPath("https/sling.apache.org.443",
            "https/sling.apache.org");
        assertEqualUriPath("https/sling.apache.org.443/",
            "https/sling.apache.org/");
        assertEqualUriPath("https/sling.apache.org.443/site/index.html",
            "https/sling.apache.org/site/index.html");

        // https with port
        assertEqualUriPath("https/sling.apache.org.443",
            "https/sling.apache.org.443");
        assertEqualUriPath("https/sling.apache.org.443/",
            "https/sling.apache.org.443/");
        assertEqualUriPath("https/sling.apache.org.443/site/index.html",
            "https/sling.apache.org.443/site/index.html");

        // anything without port
        assertEqualUriPath("gurk/sling.apache.org", "gurk/sling.apache.org");
        assertEqualUriPath("gurk/sling.apache.org/", "gurk/sling.apache.org/");
        assertEqualUriPath("gurk/sling.apache.org/site/index.html",
            "gurk/sling.apache.org/site/index.html");

        // http with port
        assertEqualUriPath("gurk/sling.apache.org.123",
            "gurk/sling.apache.org.123");
        assertEqualUriPath("gurk/sling.apache.org.456/",
            "gurk/sling.apache.org.456/");
        assertEqualUriPath("gurk/sling.apache.org.789/site/index.html",
            "gurk/sling.apache.org.789/site/index.html");

    }

    @Test public void test_isRegExp() {
        TestCase.assertFalse(isRegExp("http/www.example.com.8080/bla"));
        TestCase.assertTrue(isRegExp("http/.+\\.www.example.com.8080/bla"));
        TestCase.assertTrue(isRegExp("http/(.+)\\.www.example.com.8080/bla"));
        TestCase.assertTrue(isRegExp("http/(.+)\\.www.example.com.8080/bla"));
        TestCase.assertTrue(isRegExp("http/[^.]+.www.example.com.8080/bla"));
    }

    @Test public void test_filterRegExp() {
        TestCase.assertNull(filterRegExp((String[]) null));
        TestCase.assertNull(filterRegExp(new String[0]));

        String aString = "plain";
        String aString2 = "plain2";
        String aPattern = "http/[^.]+.www.example.com.8080/bla";

        TestCase.assertNull(filterRegExp(aPattern));

        String[] res = filterRegExp(aString);
        TestCase.assertNotNull(res);
        TestCase.assertEquals(1, res.length);
        TestCase.assertEquals(aString, res[0]);

        res = filterRegExp(aString, aPattern);
        TestCase.assertNotNull(res);
        TestCase.assertEquals(1, res.length);
        TestCase.assertEquals(aString, res[0]);

        res = filterRegExp(aPattern, aString, aPattern);
        TestCase.assertNotNull(res);
        TestCase.assertEquals(1, res.length);
        TestCase.assertEquals(aString, res[0]);

        res = filterRegExp(aPattern, aString);
        TestCase.assertNotNull(res);
        TestCase.assertEquals(1, res.length);
        TestCase.assertEquals(aString, res[0]);
    }

    private void assertEqualUri(String expected, String uriPath) {
        String uri = MapEntry.toURI(uriPath);
        assertNotNull("Failed converting " + uriPath, uri);
        assertEquals(expected, uri.toString());
    }

    private void assertEqualUriPath(String expected, String uriPath) {
        String fixed = MapEntry.fixUriPath(uriPath);
        assertNotNull(fixed);
        assertEquals(expected, fixed);
    }

    private boolean isRegExp(final String string) {
        try {
            Method m = MapEntry.class.getDeclaredMethod("isRegExp", String.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(null, string);
        } catch (Exception e) {
            fail(e.toString());
            return false; // quiesc compiler
        }
    }

    private String[] filterRegExp(final String... strings) {
        try {
            Method m = MapEntry.class.getDeclaredMethod("filterRegExp", String[].class);
            m.setAccessible(true);
            return (String[]) m.invoke(null, new Object[] {
                strings
            });
        } catch (Exception e) {
            fail(e.toString());
            return null; // quiesc compiler
        }
    }
}
