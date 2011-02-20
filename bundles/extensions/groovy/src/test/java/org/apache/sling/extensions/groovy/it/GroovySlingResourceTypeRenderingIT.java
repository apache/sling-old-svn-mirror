package org.apache.sling.extensions.groovy.it;

import java.io.IOException;

import org.apache.sling.launchpad.webapp.integrationtest.AbstractSlingResourceTypeRenderingTest;

public class GroovySlingResourceTypeRenderingIT extends AbstractSlingResourceTypeRenderingTest {

    public void testGspJavaCode() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.gsp","html.gsp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "GSP template");
            assertContains(content, "TestLinkedListTest");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testGspHtml() throws IOException {
        final String toDelete = uploadTestScript("rendering-test.gsp","html.gsp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "GSP template");
            assertContains(content, "<p>" + testText + "</p>");
            assertContains(content, "<div class=\"SLING-142\" id=\"22\"/>");
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testGspHtmlInAppsFolder() throws IOException {
        // make sure there's no leftover rendering script
        {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertFalse("Content must not contain script marker before testing", content.contains("GSP template"));
        }

        // put our script under /apps/<resource type>
        final String path = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, path);
        final String toDelete = uploadTestScript(path,"rendering-test.gsp","html.gsp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertContains(content, "GSP template");
            assertContains(content, "<p>" + testText + "</p>");
        } finally {
            testClient.delete(toDelete);
        }
    }
}
