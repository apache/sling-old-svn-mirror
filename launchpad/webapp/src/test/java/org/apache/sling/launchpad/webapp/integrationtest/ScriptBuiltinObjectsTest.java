package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.servlets.post.impl.SlingPostServlet;

public class ScriptBuiltinObjectsTest extends RenderingTestBase {

    private String slingResourceType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        slingResourceType = "integration-test/srt." + System.currentTimeMillis();
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostServlet.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("sling:resourceType", slingResourceType);
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }

    public void testEspBuiltinObjects() throws IOException {
        final String toDelete = uploadTestScript("builtin-objects.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes ESP marker (" + content + ")",content.contains("ESP template"));
            assertTrue("Content includes test text (" + content + ")", content.contains(testText));
            assertTrue("Content includes currentNode text (" + content + ")", content.contains("currentNode.text:" + testText));
            assertTrue("Content includes sc data (" + content + ")",content.contains("sc:null"));
            assertTrue("Content includes response data (" + content + ")",content.contains("SlingHttpServletResponse:false"));
        } finally {
            testClient.delete(toDelete);
        }
    }

}
