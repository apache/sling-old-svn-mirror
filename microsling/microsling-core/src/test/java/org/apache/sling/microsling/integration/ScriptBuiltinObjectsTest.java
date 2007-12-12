package org.apache.sling.microsling.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ScriptBuiltinObjectsTest extends RenderingTestBase {
    
    private String slingResourceType;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        slingResourceType = "integration-test/srt." + System.currentTimeMillis();
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + "/*";
        final Map<String,String> props = new HashMap<String,String>();
        props.put("sling:resourceType", slingResourceType);
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /sling/scripts in the repository
        scriptPath = "/sling/scripts/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }
    
    public void testEspBuiltinObjects() throws IOException {
        final String toDelete = uploadTestScript("builtin-objects.esp","html.esp");
        try {
            final String content = getContent(displayUrl + ".html", CONTENT_TYPE_HTML);
            assertTrue("Content includes ESP marker",content.contains("ESP template"));
            assertTrue("Content includes test text", content.contains(testText));
            assertTrue("Content includes ServletContext data",content.contains("ServletContext:text/plain"));
            assertTrue("Content includes response data",content.contains("SlingHttpServletResponse:false"));
        } finally {
            testClient.delete(toDelete);
        }
    }
    
}
