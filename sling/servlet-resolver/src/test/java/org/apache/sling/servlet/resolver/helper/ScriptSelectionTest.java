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
package org.apache.sling.servlet.resolver.helper;

import java.util.Collection;

import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;

/** Various tests that explain and demonstrate how scripts are 
 *  selected
 */
public class ScriptSelectionTest extends LocationTestBase {
    
    /** Test set of available scripts */
    protected final String [] SET_A = {
            "/apps/foo/bar/html.esp",
            "/apps/foo/bar/POST.esp",
            "/apps/foo/bar/print.esp",
            "/apps/foo/bar/print/POST.esp",
            "/apps/foo/bar/xml.esp",
            "/apps/foo/bar/print.xml.esp"
    };
    
    /** Given a list of available scripts and the request method, selectors 
     *  and extension, check that the expected script is selected.
     *  The resource type is foo:bar, set by LocationTestBase
     */ 
    protected void assertScript(String method, String selectors, String extension,
            String [] scripts, String expectedScript) 
    {
        // Add given scripts to our mock resource resolver
        for(String script : scripts) {
            final MockResource r = new MockResource(resourceResolver, script, "nt:file");
            resourceResolver.addResource(r);            
        }
        
        // Create mock request and get scripts from LocationUtil
        final MockSlingHttpServletRequest req = makeRequest(method, selectors, extension);
        final LocationUtil u = LocationUtil.create(req);
        final Collection<LocationResource> s = u.getScripts(req);
        
        if(expectedScript == null) {
            assertFalse("No script must be found", s.iterator().hasNext());
        } else {
            // Verify that the expected script is the first in the list of candidates
            assertTrue("A script must be found", s.iterator().hasNext());
            final String scriptPath = s.iterator().next().getResource().getPath();
            assertEquals("First script is the expected one", expectedScript, scriptPath);
        }
    }
    
    public void testHtmlGet() {
        assertScript("GET", null, "html", SET_A, "/apps/foo/bar/html.esp");
    }
    
    public void testHtmlGetSelectors() {
        assertScript("GET", "print.a4", "html", SET_A, "/apps/foo/bar/print.esp");
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
        assertScript("POST", "print.a4", "html", SET_A, "/apps/foo/bar/POST.esp");
    }
    
    public void testHtmlPostSelectorsAreIgnored() {
        final String [] scripts = {
                "/apps/foo/bar/print/POST.esp"
        };
        assertScript("POST", "print.a4", "html", scripts, null);
    }
    
    public void testHtmlPostExtensionIsIgnored() {
        final String [] scripts = {
                "/apps/foo/bar/POST.html.esp"
        };
        assertScript("POST", "print.a4", "html", scripts, null);
    }
    
    public void testHtmlPostCannotIncludeResourceType() {
        final String [] scripts = {
                "/apps/foo/bar/bar.POST.esp"
        };
        assertScript("POST", "print.a4", "html", scripts, null);
    }
}
