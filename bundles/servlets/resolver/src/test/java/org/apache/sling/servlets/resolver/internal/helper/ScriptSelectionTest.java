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
package org.apache.sling.servlets.resolver.internal.helper;

import java.util.Collection;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;

/** Various tests that explain and demonstrate how scripts are
 *  selected. See the assertScript methods for how to interpret
 *  the various tests.
 */
public class ScriptSelectionTest extends HelperTestBase {

    /** Test set of available scripts */
    protected final String [] SET_A = {
            "/apps/foo/bar/html.esp",
            "/apps/foo/bar/POST.esp",
            "/apps/foo/bar/print.esp",
            "/apps/foo/bar/print",
            "/apps/foo/bar/print/POST.esp",
            "/apps/foo/bar/mail.POST.esp",
            "/apps/foo/bar/xml.esp",
            "/apps/foo/bar/print.xml.esp",
            "/apps/foo/bar/print/DELETE.esp",
            "/apps/foo/bar/DELETE.esp",
            "/apps/foo/bar/SPURIOUS.esp",
            "/apps/foo/bar/UNKNOWN.esp"

    };

    /** Given a list of available scripts and the request method, selectors
     *  and extension, check that the expected script is selected.
     *  The resource type is foo:bar, set by HelperTestBase
     *
     *  @param method the HTTP method of the simulated request
     *  @param selectors the selectors of the simulated request
     *  @param extension the extension of the simulated request
     *  @param scripts the list of scripts that would be available in the repository
     *  @param expectedScript the script that we expect to be selected
     */
    protected void assertScript(String method, String selectors, String extension,
            String [] scripts, String expectedScript)
    {
        // Add given scripts to our mock resource resolver
        for(String script : scripts) {
            final MockResource r = new MockResource(resourceResolver, script, "nt:file");
            resourceResolver.addResource(r);
        }

        // Create mock request and get scripts from ResourceCollector
        final MockSlingHttpServletRequest req = makeRequest(method, selectors, extension);
        final ResourceCollector u = ResourceCollector.create(req, null, new String[] {"html"});
        final Collection<Resource> s = u.getServlets(req.getResource().getResourceResolver());

        if(expectedScript == null) {
            assertFalse("No script must be found", s.iterator().hasNext());
        } else {
            // Verify that the expected script is the first in the list of candidates
            assertTrue("A script must be found", s.iterator().hasNext());
            final String scriptPath = s.iterator().next().getPath();
            assertEquals("First script is the expected one", expectedScript, scriptPath);
        }
    }

    public void testHtmlGet() {
        assertScript("GET", null, "html", SET_A, "/apps/foo/bar/html.esp");
    }

    public void testHtmlGetSelectors() {
        assertScript("GET", "print.a4", "html", SET_A, "/apps/foo/bar/print.esp");
    }

    public void testHtmlGetSelectorsAndResourceLabel() {
        final String [] scripts = {
                "/apps/foo/bar/bar.esp",
                "/apps/foo/bar/bar.print.esp"
            };
        // the bar.print.esp script is not used, it must be named print.esp
        // to be selector-specific
        assertScript("GET", "print.a4", "html", scripts, "/apps/foo/bar/bar.esp");
    }

    public void testHtmlGetSingleSelector() {
        assertScript("GET", "print", "html", SET_A, "/apps/foo/bar/print.esp");
    }

    public void testHtmlGetHtmlHasPriorityA() {
        final String [] scripts = {
            "/apps/foo/bar/html.esp",
            "/apps/foo/bar/bar.esp"
        };
        assertScript("GET", null, "html", scripts, "/apps/foo/bar/html.esp");
    }

    public void testHtmlGetHtmlHasPriorityB() {
        final String [] scripts = {
            "/apps/foo/bar/bar.esp",
            "/apps/foo/bar/html.esp"
        };
        assertScript("GET", null, "html", scripts, "/apps/foo/bar/html.esp");
    }

    public void testHtmlGetBarUsedIfFound() {
        final String [] scripts = {
                "/apps/foo/bar/bar.esp",
                "/apps/foo/bar/pdf.esp"
            };
        assertScript("GET", null, "html", scripts, "/apps/foo/bar/bar.esp");
    }

    public void testXmlGetSetC() {
        assertScript("GET", null, "xml", SET_A, "/apps/foo/bar/xml.esp");
    }

    public void testXmlGetSelectors() {
        assertScript("GET", "print.a4", "xml", SET_A, "/apps/foo/bar/print.xml.esp");
    }

    public void testMultipleSelectorsA() {
        final String [] scripts = {
                "/apps/foo/bar/print",
                "/apps/foo/bar/print/a4.esp",
                "/apps/foo/bar/print.a4.esp",
                "/apps/foo/bar/html.print.a4.esp",
                "/apps/foo/bar/html.print.esp",
                "/apps/foo/bar/print.esp",
                "/apps/foo/bar/print.html.esp",
                "/apps/foo/bar/a4.esp",
                "/apps/foo/bar/a4.html.esp",
                "/apps/foo/bar/html.esp"
            };
            assertScript("GET", "print.a4", "html", scripts, "/apps/foo/bar/print/a4.esp");
    }

    public void testMultipleSelectorsB() {
        final String [] scripts = {
                "/apps/foo/bar/print.a4.esp",
                "/apps/foo/bar/print.esp",
                "/apps/foo/bar/a4.esp",
                "/apps/foo/bar/html.esp"
            };
            assertScript("GET", "print.a4", "html", scripts, "/apps/foo/bar/print.esp");
            assertScript("GET", "a4.print", "html", scripts, "/apps/foo/bar/a4.esp");
            assertScript("GET", null, "html", scripts, "/apps/foo/bar/html.esp");
    }

    public void testMultipleSelectorsC() {
        final String [] scripts = {
                "/apps/foo/bar/print.esp",
                "/apps/foo/bar/html.esp"
            };
            assertScript("GET", "print.a4", "html", scripts, "/apps/foo/bar/print.esp");
    }

    public void testMultipleSelectorsD() {
        final String [] scripts = {
                "/apps/foo/bar/a4.esp",
                "/apps/foo/bar/html.esp"
            };
            assertScript("GET", "print.a4", "html", scripts, "/apps/foo/bar/html.esp");
            assertScript("GET", "a4.print", "html", scripts, "/apps/foo/bar/a4.esp");
    }

    public void testMultipleSelectorsE() {
        final String [] scripts = {
                "/apps/foo/bar/bar.print.a4.esp",
                "/apps/foo/bar/bar.print.esp",
                "/apps/foo/bar/print.esp",
                "/apps/foo/bar/a4.esp",
                "/apps/foo/bar/bar.a4.esp",
                "/apps/foo/bar/print",
                "/apps/foo/bar/print/a4.esp",
                "/apps/foo/bar/html.esp"
            };
            assertScript("GET", "print.a4", "html", scripts, "/apps/foo/bar/print/a4.esp");
            assertScript("GET", "a4.print", "html", scripts, "/apps/foo/bar/a4.esp");
            assertScript("GET", null, "html", scripts, "/apps/foo/bar/html.esp");
    }

    public void testXmlGetSingleSelector() {
        assertScript("GET", "print", "xml", SET_A, "/apps/foo/bar/print.xml.esp");
    }

    public void testHtmlGetAppsOverridesLibsA() {
        final String [] scripts = {
                "/apps/foo/bar/bar.esp",
                "/libs/foo/bar/bar.esp"
            };
        assertScript("GET", null, "html", scripts, "/apps/foo/bar/bar.esp");
    }

    public void testHtmlGetAppsOverridesLibsB() {
        final String [] scripts = {
                "/libs/foo/bar/bar.esp",
                "/apps/foo/bar/bar.esp"
            };
        assertScript("GET", null, "html", scripts, "/apps/foo/bar/bar.esp");
    }

    public void testHtmlGetLibUsedIfFound() {
        final String [] scripts = {
                "/libs/foo/bar/bar.esp"
            };
        assertScript("GET", null, "html", scripts, "/libs/foo/bar/bar.esp");
    }

    public void testHtmlPost() {
        assertScript("POST", null, "html", SET_A, "/apps/foo/bar/POST.esp");
    }

    public void testHtmlPostBadCaseFindsNoScript() {
        final String [] scripts = {
                "/apps/foo/bar/html.esp",
                "/apps/foo/bar/POst.esp"
        };
        assertScript("POST", null, "html", scripts, null);
    }

    public void testHtmlPostSelectors() {
        assertScript("POST", "print.a4", "html", SET_A, "/apps/foo/bar/print/POST.esp");
        assertScript("POST", "print", "html", SET_A, "/apps/foo/bar/print/POST.esp");
        assertScript("POST", "mail", "html", SET_A, "/apps/foo/bar/mail.POST.esp");
        assertScript("POST", "a4.print", "html", SET_A, "/apps/foo/bar/POST.esp");
        assertScript("POST", null, "html", SET_A, "/apps/foo/bar/POST.esp");
    }

    public void testHtmlMethodSelectors() {
        assertScript("DELETE", "print.a4", "html", SET_A, "/apps/foo/bar/print/DELETE.esp");
        assertScript("SPURIOUS", "print", "html", SET_A, "/apps/foo/bar/SPURIOUS.esp");
        assertScript("UNKNOWN", "a4.print", "html", SET_A, "/apps/foo/bar/UNKNOWN.esp");
        assertScript("UNKNOWN", null, "html", SET_A, "/apps/foo/bar/UNKNOWN.esp");
    }

    public void testHtmlPostMethodSelectors() {
        final String [] scripts = {
                "/apps/foo/bar/print",
                "/apps/foo/bar/print/POST.esp"
        };
        assertScript("POST", "print.a4", "html", scripts, "/apps/foo/bar/print/POST.esp");
    }

    public void testHtmlPostMethodExtension() {
        final String [] scripts = {
                "/apps/foo/bar/html.POST.esp"
        };
        assertScript("POST", "print.a4", "html", scripts, "/apps/foo/bar/html.POST.esp");
    }

    public void testHtmlPostMethodResourceType() {
        final String [] scripts = {
                "/apps/foo/bar/bar.POST.esp"
        };
        assertScript("POST", "print.a4", "html", scripts, "/apps/foo/bar/bar.POST.esp");
    }
}
