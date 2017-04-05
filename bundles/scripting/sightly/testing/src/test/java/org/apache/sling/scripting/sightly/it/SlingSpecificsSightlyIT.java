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

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import io.sightly.tck.html.HTMLExtractor;
import io.sightly.tck.http.Client;

import static org.junit.Assert.*;

public class SlingSpecificsSightlyIT {

    private static Client client;
    private static String launchpadURL;
    private static final String SLING_USE = "/sightly/use.html";
    private static final String SLING_JAVA_USE_NPE = "/sightly/use.javaerror.html";
    private static final String SLING_RESOURCE = "/sightly/resource.html";
    private static final String SLING_RESOURCE_ACTUAL = "/sightly/actualresource.html";
    private static final String SLING_TEMPLATE = "/sightly/template.html";
    private static final String SLING_TEMPLATE_BAD_IDENTIFIER = "/sightly/template.bad-id.html";
    private static final String SLING_JS_USE = "/sightly/use.jsuse.html";
    private static final String SLING_JS_DEPENDENCY_RESOLUTION = "/sightly/use-sibling-dependency-resolution.html";
    private static final String SLING_USE_INHERITANCE_WITHOVERLAY = "/sightly/useinheritance.html";
    private static final String SLING_USE_INHERITANCE_WITHOUTOVERLAY = "/sightly/useinheritance.notoverlaid.html";
    private static final String SLING_JAVA_USE_POJO_UPDATE = "/sightly/use.repopojo.html";
    private static final String SLING_ATTRIBUTE_QUOTES = "/sightly/attributequotes.html";
    private static final String SLING_CRLF = "/sightly/crlf";
    private static final String SLING_CRLF_NOPKG = SLING_CRLF + ".nopkg.html";
    private static final String SLING_CRLF_PKG = SLING_CRLF + ".pkg.html";
    private static final String SLING_CRLF_WRONGPKG = SLING_CRLF + ".wrongpkg.html";
    private static final String SLING_SCRIPT_UPDATE = "/sightly/update.html";
    private static final String SLING_REQUEST_ATTRIBUTES = "/sightly/requestattributes.html";
    private static final String SLING_REQUEST_ATTRIBUTES_INCLUDE = "/sightly/requestattributes.include.html";
    private static final String SLING_RESOURCE_USE = "/sightly/use.resource.html";
    private static final String SLING_I18N = "/sightly/i18n";
    private static final String TCK_XSS = "/sightlytck/exprlang/xss.html";
    private static final String WHITESPACE = "/sightly/whitespace.html";

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
        assertEquals("nt:unstructured", HTMLExtractor.innerHTML(url, pageContent, "#reqmodel-bindings"));
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
    public void testEnumConstantAsString() {
        String url = launchpadURL + SLING_USE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("ENUM_CONSTANT", HTMLExtractor.innerHTML(url, pageContent, "#test-enum"));
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
    public void testDataSlyResourceResolution() {
        String url = launchpadURL + SLING_RESOURCE;
        String pageContent = client.getStringContent(url, 200);

        assertEquals("resource.with.dots.in.path", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_resource_with_dots_in_path span" +
                ".name"));
        assertEquals("/sightly/resource.with.dots.in.path", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_resource_with_dots_in_path span" +
                ".path"));
        assertEquals("false", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_resource_with_dots_in_path span" +
                ".synthetic"));

        assertEquals("nonexistingresource", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_nonexistingresource span" +
                ".name"));
        assertEquals("/sightly/nonexistingresource", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_nonexistingresource span" +
                ".path"));
        assertEquals("true", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_nonexistingresource span.synthetic"));

        assertEquals("resource", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_resource span" +
                ".name"));
        assertEquals("/sightly/resource", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_resource span" +
                ".path"));
        assertEquals("false", HTMLExtractor.innerHTML(url, pageContent, "#_sightly_resource span.synthetic"));
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
        assertTrue(pageContent.contains(
                "org.apache.sling.scripting.sightly.java.compiler.SightlyJavaCompilerException: Unsupported identifier name: bad-template-id"));
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
        assertEquals("child.partials.javascript", HTMLExtractor.innerHTML(url, pageContent, "#partialsjs"));
        assertEquals("child.partials.ecmaobject", HTMLExtractor.innerHTML(url, pageContent, "#partialsecma"));
        assertEquals("child.partials.template", HTMLExtractor.innerHTML(url, pageContent, "#partialstemplate"));
        assertEquals("child.partials.included", HTMLExtractor.innerHTML(url, pageContent, "#partialsincluded"));
        assertEquals("child.partials.javaobject", HTMLExtractor.innerHTML(url, pageContent, "#partialsjava"));
    }

    @Test
    public void testUseAPIInheritanceWithoutOverlay() {
        String url = launchpadURL + SLING_USE_INHERITANCE_WITHOUTOVERLAY;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("notoverlaid", HTMLExtractor.innerHTML(url, pageContent, "#notoverlaid"));
    }

    @Test
    public void testRepositoryPojoUpdate() throws Exception {
        String url = launchpadURL + SLING_JAVA_USE_POJO_UPDATE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("original", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#repopojo"));
        uploadFile("RepoPojo.java.updated", "RepoPojo.java", "/apps/sightly/scripts/use");
        Thread.sleep(1000);
        pageContent = client.getStringContent(url, 200);
        assertEquals("updated", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#repopojo"));
        uploadFile("RepoPojo.java.original", "RepoPojo.java", "/apps/sightly/scripts/use");
        Thread.sleep(1000);
        pageContent = client.getStringContent(url, 200);
        assertEquals("original", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#repopojo"));
    }

    @Test
    public void testRepositoryPojoUpdateDirty() throws Exception {
        String url = launchpadURL + SLING_JAVA_USE_POJO_UPDATE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("original", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#repopojo"));

        uploadFile("RepoPojo.java.updated", "RepoPojo.java", "/apps/sightly/scripts/use");
        Thread.sleep(1000);
        restartSightlyEngineBundle();

        pageContent = client.getStringContent(url, 200);
        assertEquals("updated", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#repopojo"));

        uploadFile("RepoPojo.java.original", "RepoPojo.java", "/apps/sightly/scripts/use");
        Thread.sleep(1000);
        restartSightlyEngineBundle();

        pageContent = client.getStringContent(url, 200);
        assertEquals("original", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#repopojo"));
    }

    @Test
    public void testScriptUpdate() throws Exception {
        String url = launchpadURL + SLING_SCRIPT_UPDATE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("Hello world!", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#update"));
        uploadFile("update.v2.html", "update.html", "/apps/sightly/scripts/update");
        Thread.sleep(1000);
        pageContent = client.getStringContent(url, 200);
        assertEquals("Hello, John Doe!", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#update"));
        uploadFile("update.html", "update.html", "/apps/sightly/scripts/update");
        Thread.sleep(1000);
        pageContent = client.getStringContent(url, 200);
        assertEquals("Hello world!", HTMLExtractor.innerHTML(url + System.currentTimeMillis(), pageContent, "#update"));
    }

    @Test
    public void testRepositoryPojoNoPkg() {
        String url = launchpadURL + SLING_USE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("nopkg", HTMLExtractor.innerHTML(url, pageContent, "#repopojo-nopkg"));
    }

    @Test
    public void testAttributeQuotes() {
        String url = launchpadURL + SLING_ATTRIBUTE_QUOTES;
        String pageContent = client.getStringContent(url, 200);
        // need to test against the raw content
        assertTrue(pageContent.contains("<span data-resource='{\"resource\" : \"/sightly/attributequotes\"}'>/sightly/attributequotes</span>"));
        assertTrue(pageContent.contains("<span data-resource=\"/sightly/attributequotes\">/sightly/attributequotes</span>"));
        assertTrue(pageContent.contains("<span data-resource=\"/sightly/attributequotes\">/sightly/attributequotes</span>"));
    }

    @Test
    public void testCRLFNoPkg() {
        String url = launchpadURL + SLING_CRLF_NOPKG;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("nopkg", HTMLExtractor.innerHTML(url, pageContent, "#repopojocrlf-nopkg"));
    }

    @Test
    public void testCRLFPkg() {
        String url = launchpadURL + SLING_CRLF_PKG;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("pkg", HTMLExtractor.innerHTML(url, pageContent, "#repopojocrlf-pkg"));
    }

    @Test
    public void testCRLFWrongPkg() {
        String url = launchpadURL + SLING_CRLF_WRONGPKG;
        String pageContent = client.getStringContent(url, 500);
        assertTrue(pageContent.contains("Compilation errors in apps/sightly/scripts/crlf/RepoPojoWrongPkgCRLF.java"));
    }

    @Test
    public void actualResource() {
        String url = launchpadURL + SLING_RESOURCE_ACTUAL;
        String pageContent = client.getStringContent(url, 200);
        String hash = HTMLExtractor.innerHTML(url, pageContent, "#hash");
        String actual = HTMLExtractor.innerHTML(url, pageContent, "#actual");
        String path = HTMLExtractor.innerHTML(url, pageContent, "#path");
        assertEquals(hash, actual);
        assertNotEquals(hash, path);
    }

    @Test
    public void testRequestAttributes() {
        String url = launchpadURL + SLING_REQUEST_ATTRIBUTES;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("testValue", HTMLExtractor.innerHTML(url, pageContent, "#attrs-set"));
        assertEquals("", HTMLExtractor.innerHTML(url, pageContent, "#attrs-unset"));
    }

    @Test
    public void testRequestAttributesInclude() {
        String url = launchpadURL + SLING_REQUEST_ATTRIBUTES_INCLUDE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("testValue", HTMLExtractor.innerHTML(url, pageContent, "#attrs-set"));
        assertEquals("", HTMLExtractor.innerHTML(url, pageContent, "#attrs-unset"));
    }

    @Test
    public void testResourceUse() {
        String url = launchpadURL + SLING_RESOURCE_USE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("foobar-someresource", HTMLExtractor.innerHTML(url, pageContent, "#someresource .data"));
        assertEquals("foobar-somefolder", HTMLExtractor.innerHTML(url, pageContent, "#somefolder .data"));
        assertEquals("foobar-sometemplate", HTMLExtractor.innerHTML(url, pageContent, "#sometemplate .data"));
        assertEquals("foobar-somejava", HTMLExtractor.innerHTML(url, pageContent, "#somejava .data"));
        assertEquals("foobar-somejs", HTMLExtractor.innerHTML(url, pageContent, "#somejs .data"));
        assertEquals("foobar-someecma", HTMLExtractor.innerHTML(url, pageContent, "#someecma .data"));
    }

    @Test
    public void testI18nBasename() {
        String url = launchpadURL + SLING_I18N + ".basename.html";
        String pageContent = client.getStringContent(url, 200);
        assertEquals("die Bank", HTMLExtractor.innerHTML(url, pageContent, "#i18n-basename-finance"));
        assertEquals("das Ufer", HTMLExtractor.innerHTML(url, pageContent, "#i18n-nobasename"));
    }

    @Test
    public void testXSSAttributeEscaping() {
        String url = launchpadURL + TCK_XSS;
        String pageContent = client.getStringContent(url, 200);
        assertTrue(pageContent.contains("<p id=\"req-context-8\" onclick=\"console.log('red')\">Paragraph</p>"));
    }

    @Test
    public void testWhiteSpaceExpressions() {
        String url = launchpadURL + WHITESPACE;
        String pageContent = client.getStringContent(url, 200);
        assertEquals("true", HTMLExtractor.innerHTML(url, pageContent, "#nbsp"));
        assertEquals("true", HTMLExtractor.innerHTML(url, pageContent, "#tab"));
        assertEquals("true", HTMLExtractor.innerHTML(url, pageContent, "#newline"));
    }

    private void restartSightlyEngineBundle() throws InterruptedException, IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(launchpadURL + "/system/console/bundles/org.apache.sling.scripting.sightly");
        // Stop bundle
        FormEntityBuilder formBuilder = FormEntityBuilder.create();
        post.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        formBuilder.addParameter("action", "stop");
        post.setEntity(formBuilder.build());
        httpClient.execute(post);
        Thread.sleep(1000);
        // Start bundle
        formBuilder = FormEntityBuilder.create();
        post.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        formBuilder.addParameter("action", "start");
        post.setEntity(formBuilder.build());
        httpClient.execute(post);
        Thread.sleep(1000);
    }

    private void uploadFile(String fileName, String serverFileName, String url) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(launchpadURL + url);
        post.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        InputStreamBody inputStreamBody = new InputStreamBody(this.getClass().getClassLoader().getResourceAsStream(fileName),
                ContentType.TEXT_PLAIN, fileName);
        entityBuilder.addPart(serverFileName, inputStreamBody);
        post.setEntity(entityBuilder.build());
        httpClient.execute(post);
    }

}
