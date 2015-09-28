/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.it;

import org.junit.BeforeClass;
import org.junit.Test;

import io.sightly.tck.http.Client;
import io.sightly.tck.html.HTMLExtractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlingSpecificsSightlyIT {

    private static Client client;
    private static String launchpadURL;
    private static final String SLING_USE = "/sightly/use.html";
    private static final String SLING_JAVA_USE_NPE = "/sightly/use.javaerror.html";
    private static final String SLING_RESOURCE = "/sightly/resource.html";
    private static final String SLING_TEMPLATE = "/sightly/template.html";
    private static final String SLING_TEMPLATE_BAD_IDENTIFIER = "/sightly/template.bad-id.html";
    private static final String SLING_JS_USE = "/sightly/use.jsuse.html";
    private static final String SLING_JS_DEPENDENCY_RESOLUTION = "/sightly/use-sibling-dependency-resolution.html";
    private static final String SLING_USE_INHERITANCE_WITHOVERLAY = "/sightly/useinheritance.html";
    private static final String SLING_USE_INHERITANCE_WITHOUTOVERLAY = "/sightly/useinheritance.notoverlaid.html";

    @BeforeClass
    public static void init() {
        client = new Client();
        launchpadURL = System.getProperty("launchpad.http.server.url");
    }

    @Test
    public void testSlingModelsUseAPI() {
        String url = launchpadURL + SLING_USE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("SUCCESS", HTMLExtractor.innerHTML(url, pageContent, "#reqmodel"));
        assertEquals("SUCCESS", HTMLExtractor.innerHTML(url, pageContent, "#reqmodel-reqarg"));
        assertEquals("SUCCESS", HTMLExtractor.innerHTML(url, pageContent, "#resmodel"));
        
    }
    
    @Test
    public void testAdaptablesUseAPI() {
        String url = launchpadURL + SLING_USE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("SUCCESS", HTMLExtractor.innerHTML(url, pageContent, "#resadapt"));
        assertEquals("SUCCESS", HTMLExtractor.innerHTML(url, pageContent, "#reqadapt"));
    }

    @Test
    public void testUseAPIWithOSGIService() {
        String url = launchpadURL + SLING_USE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("Hello World!", HTMLExtractor.innerHTML(url, pageContent, "#osgi"));
    }

    @Test
    public void testErroneousUseObject() {
        String url = launchpadURL + SLING_JAVA_USE_NPE;
        String pageContent = client.getStringContent(url, 500);
        assertTrue(pageContent.contains("java.lang.NullPointerException"));
    }

    @Test
    public void testDataSlyResourceArraySelectors() {
        String url = launchpadURL + SLING_RESOURCE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("selectors: a.b", HTMLExtractor.innerHTML(url, pageContent, "#selectors span.selectors"));
        assertEquals("selectors: a.b", HTMLExtractor.innerHTML(url, pageContent, "#selectors-remove-c span.selectors"));
        assertEquals("selectors: a.c", HTMLExtractor.innerHTML(url, pageContent, "#removeselectors-remove-b span.selectors"));
        assertEquals("selectors: a.b.c", HTMLExtractor.innerHTML(url, pageContent, "#addselectors span.selectors"));
        assertEquals("It works", HTMLExtractor.innerHTML(url, pageContent, "#dot"));
        assertEquals("path: /sightly/text.txt", HTMLExtractor.innerHTML(url, pageContent, "#extension-selectors span.path"));
        assertEquals("selectors: a.b", HTMLExtractor.innerHTML(url, pageContent, "#extension-selectors span.selectors"));
        assertEquals("path: /sightly/text.txt", HTMLExtractor.innerHTML(url, pageContent, "#extension-replaceselectors span.path"));
        assertEquals("selectors: c", HTMLExtractor.innerHTML(url, pageContent, "#extension-replaceselectors span.selectors"));
    }

    @Test
    public void testDataSlyTemplate() {
        String url = launchpadURL + SLING_TEMPLATE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("SUCCESS", HTMLExtractor.innerHTML(url, pageContent, "#template"));
    }

    @Test
    public void testBadTemplateIdentifier() {
        String url = launchpadURL + SLING_TEMPLATE_BAD_IDENTIFIER;
        String pageContent = client.getStringContent(url, 500);
        assertTrue(pageContent.contains("org.apache.sling.scripting.sightly.impl.compiler.SightlyParsingException"));
    }

    @Test
    public void testJSUseAPI() {
        String url = launchpadURL + SLING_JS_USE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("use", HTMLExtractor.innerHTML(url, pageContent, "#resource-name"));
        assertEquals("use", HTMLExtractor.innerHTML(url, pageContent, "#resource-getName"));
        assertEquals("/apps/sightly/scripts/use", HTMLExtractor.innerHTML(url, pageContent, "#resource-resourceType"));
        assertEquals("/apps/sightly/scripts/use", HTMLExtractor.innerHTML(url, pageContent, "#resource-getResourceType"));
    }

    @Test
    public void testJSUseAPISiblingDependencies() {
        String url = launchpadURL + SLING_JS_DEPENDENCY_RESOLUTION;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("/apps/sightly/scripts/siblingdeps/dependency.js", HTMLExtractor.innerHTML(url, pageContent, "#js-rep-res"));
    }

    @Test
    public void testUseAPIInheritanceOverlaying() {
        String url = launchpadURL + SLING_USE_INHERITANCE_WITHOVERLAY;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("child.javaobject", HTMLExtractor.innerHTML(url, pageContent, "#javaobj"));
        assertEquals("child.javascriptobject", HTMLExtractor.innerHTML(url, pageContent, "#javascriptobj"));
        assertEquals("child.ecmaobject", HTMLExtractor.innerHTML(url, pageContent, "#ecmaobj"));
        assertEquals("child.template", HTMLExtractor.innerHTML(url, pageContent, "#templateobj"));
    }

    @Test
    public void testUseAPIInheritanceWithoutOverlay() {
        String url = launchpadURL + SLING_USE_INHERITANCE_WITHOUTOVERLAY;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("notoverlaid", HTMLExtractor.innerHTML(url, pageContent, "#notoverlaid"));
    }

}
