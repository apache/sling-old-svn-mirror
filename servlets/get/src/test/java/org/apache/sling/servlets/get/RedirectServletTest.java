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
package org.apache.sling.servlets.get;

import junit.framework.TestCase;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.get.RedirectServlet;

public class RedirectServletTest extends TestCase {

    public void testSameParent() {
        String base = "/a";
        String target = "/b";
        assertEquals("b", toRedirect(base, target));

        base = "/";
        target = "/a";
        assertEquals("a", toRedirect(base, target));

        base = "/a/b/c";
        target = "/a/b/d";
        assertEquals("d", toRedirect(base, target));
    }

    public void testTrailingSlash() {
        String base = "/a/b/c/";
        String target = "/a/b/c.html";
        assertEquals("../c.html", toRedirect(base, target));
    }

    public void testCommonAncestor() {
        String base = "/a/b/c/d";
        String target = "/a/b/x/y";
        assertEquals("../x/y", toRedirect(base, target));
    }

    public void testChild() {
        String base = "/a.html";
        String target = "/a/b.html";
        assertEquals("a/b.html", toRedirect(base, target));

        base = "/a";
        target = "/a/b.html";
        assertEquals("a/b.html", toRedirect(base, target));

        base = "/a";
        target = "/a/b";
        assertEquals("a/b", toRedirect(base, target));

        base = "/a.html";
        target = "/a/b/c.html";
        assertEquals("a/b/c.html", toRedirect(base, target));

        base = "/a";
        target = "/a/b/c.html";
        assertEquals("a/b/c.html", toRedirect(base, target));

        base = "/a";
        target = "/a/b/c";
        assertEquals("a/b/c", toRedirect(base, target));
    }

    public void testChildNonRoot() {
        String base = "/x/a.html";
        String target = "/x/a/b.html";
        assertEquals("a/b.html", toRedirect(base, target));

        base = "/x/a";
        target = "/x/a/b.html";
        assertEquals("a/b.html", toRedirect(base, target));

        base = "/x/a";
        target = "/x/a/b";
        assertEquals("a/b", toRedirect(base, target));

        base = "/x/a.html";
        target = "/x/a/b/c.html";
        assertEquals("a/b/c.html", toRedirect(base, target));

        base = "/x/a";
        target = "/x/a/b/c.html";
        assertEquals("a/b/c.html", toRedirect(base, target));

        base = "/x/a";
        target = "/x/a/b/c";
        assertEquals("a/b/c", toRedirect(base, target));
    }

    public void testChildRelative() {
        String base = "/a";
        String target = "b.html";
        assertEquals("a/b.html", toRedirect(base, target));

        base = "/a";
        target = "b";
        assertEquals("a/b", toRedirect(base, target));

        base = "/a";
        target = "b/c.html";
        assertEquals("a/b/c.html", toRedirect(base, target));

        base = "/a";
        target = "b/c";
        assertEquals("a/b/c", toRedirect(base, target));
    }

    public void testChildNonRootRelative() {
        String base = "/x/a";
        String target = "b.html";
        assertEquals("a/b.html", toRedirect(base, target));

        base = "/x/a";
        target = "b";
        assertEquals("a/b", toRedirect(base, target));

        base = "/x/a";
        target = "b/c.html";
        assertEquals("a/b/c.html", toRedirect(base, target));

        base = "/x/a";
        target = "b/c";
        assertEquals("a/b/c", toRedirect(base, target));
    }

    public void testUnCommon() {
        String base = "/a/b/c/d";
        String target = "/w/x/y/z";
        assertEquals("../../../w/x/y/z", toRedirect(base, target));
    }

    public void testSelectorsEtc() {
        String base = "/a/b/c";
        String target = "/a/b/d";
        String expected = "d";

        String selectors = null;
        String extension = null;
        String suffix = null;
        String queryString = null;
        assertEquals(expected, base, null, extension, suffix, queryString,
            target);

        selectors = null;
        extension = "html";
        suffix = null;
        queryString = null;
        assertEquals(expected, base, selectors, extension, suffix, queryString,
            target);

        selectors = "print";
        extension = "html";
        suffix = null;
        queryString = null;
        assertEquals(expected, base, selectors, extension, suffix, queryString,
            target);

        selectors = "print.a4";
        extension = "html";
        suffix = null;
        queryString = null;
        assertEquals(expected, base, selectors, extension, suffix, queryString,
            target);

        selectors = null;
        extension = "html";
        suffix = "/suffix.pdf";
        queryString = null;
        assertEquals("../" + expected, base, selectors, extension, suffix,
            queryString, target);

        selectors = null;
        extension = "html";
        suffix = null;
        queryString = "xy=1";
        assertEquals(expected, base, selectors, extension, suffix, queryString,
            target);

        selectors = null;
        extension = "html";
        suffix = "/suffix.pdf";
        queryString = "xy=1";
        assertEquals("../" + expected, base, selectors, extension, suffix,
            queryString, target);

        selectors = "print.a4";
        extension = "html";
        suffix = "/suffix.pdf";
        queryString = "xy=1";
        assertEquals("../" + expected, base, selectors, extension, suffix,
            queryString, target);
    }

    private void assertEquals(String expected, String basePath,
            String selectors, String extension, String suffix,
            String queryString, String targetPath) {

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

        assertEquals(expected, actual);
    }

    private String toRedirect(String basePath, String targetPath) {
        return toRedirect(basePath, null, null, null, null, targetPath);
    }

    private String toRedirect(String basePath, String selectors,
            String extension, String suffix, String queryString,
            String targetPath) {
        SlingHttpServletRequest request = new MockSlingHttpServletRequest(
            basePath, selectors, extension, suffix, queryString);
        return RedirectServlet.toRedirectPath(targetPath, request);
    }
}
