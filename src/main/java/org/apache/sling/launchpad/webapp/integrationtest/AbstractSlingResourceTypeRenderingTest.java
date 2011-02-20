package org.apache.sling.launchpad.webapp.integrationtest;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.servlets.post.SlingPostConstants;

/** Base abstract test for creating a Node and rendering it using scripts in
 *  various supported languages, using slingResourceType-based
 *  script resolution
 */
public abstract class AbstractSlingResourceTypeRenderingTest extends RenderingTestBase {
    
    protected String slingResourceType;
    protected String secondFolderOfContentPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        slingResourceType = "integration-test/srt." + System.currentTimeMillis();
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        secondFolderOfContentPath = "" + System.currentTimeMillis();
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + secondFolderOfContentPath + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("sling:resourceType", slingResourceType);
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }
}
