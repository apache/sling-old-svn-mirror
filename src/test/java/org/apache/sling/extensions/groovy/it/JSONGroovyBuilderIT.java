package org.apache.sling.extensions.groovy.it;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.launchpad.webapp.integrationtest.RenderingTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

public class JSONGroovyBuilderIT extends RenderingTestBase {
    private String slingResourceType;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        slingResourceType = "integration-test/srt." + System.currentTimeMillis();
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis() + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("sling:resourceType", slingResourceType);
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }

    public void testJSONGroovyBuilder() throws IOException, JSONException {
        final String toDelete = uploadTestScript("builder.groovy","json.groovy");
        try {
            final String content = getContent(displayUrl + ".json", CONTENT_TYPE_JSON);
            JSONObject jo = new JSONObject(content);
            assertEquals("Content contained wrong number of items", 1, jo.length());
            assertEquals("Content contained wrong key", "text", jo.keys().next());
            assertEquals("Content contained wrong data", testText, jo.get("text"));
        } finally {
            testClient.delete(toDelete);
        }
    }

    public void testJSONGroovyBuilder2() throws IOException, JSONException {
        final String toDelete = uploadTestScript("builder2.groovy","json.groovy");
        try {
            final String content = getContent(displayUrl + ".json", CONTENT_TYPE_JSON);
            JSONObject jo = new JSONObject(content);
            assertEquals("Content contained wrong number of items", 1, jo.length());
            assertEquals("Content contained wrong key", "text", jo.keys().next());
            assertEquals("Content contained wrong data", testText, jo.get("text"));
        } finally {
            testClient.delete(toDelete);
        }
    }
}
