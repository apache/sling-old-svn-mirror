package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.methods.MultipartPostMethod;

/**
 * Check if multipart post request are handled well
 */
public class PostParameterTest extends RenderingTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "." + System.currentTimeMillis();
        final Map<String,String> props = new HashMap<String,String>();
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/nt/unstructured";
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }
    
    public void testNothing() {
        // TODO remove this all TODO_FAILS_ are gone 
    }

    public void TODO_FAILS_testBinaryUpload() throws IOException {
        final String toDelete = uploadTestScript("post-test.esp","POST.esp");
        File f = null;
        try {

            MultipartPostMethod post = new MultipartPostMethod(displayUrl);
            post.setFollowRedirects(false);

            // create simple test file
            f = File.createTempFile("posttest",".txt");
            FileWriter fstream = new FileWriter(f.getAbsolutePath());
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Hello uSling");
            out.close();

            post.addParameter("file", f);

            final int status = httpClient.executeMethod(post);
            final String response = post.getResponseBodyAsString();

            // check confirmed file length sent from script
            assertEquals(200, status);
            assertEquals(f.length(), (long) Double.parseDouble(response));

            post.releaseConnection();
        } finally {
            if (f != null) {
                f.delete();
            }
            testClient.delete(toDelete);
        }
    }
}
