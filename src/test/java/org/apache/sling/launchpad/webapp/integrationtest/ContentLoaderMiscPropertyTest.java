package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * @author justin
 *
 */
public class ContentLoaderMiscPropertyTest extends HttpTestBase {

    /**
     * Verify that the test node's node type is set correctly.
     */
    public void testLoadedNodeType() throws IOException {
        final String expected = "sling:propertySetTestNodeType";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    /**
     * Verify that the URI-type property loaded correctly.
     *
     * This test is only really useful post-JCR 2 upgrade.
     */
    public void testLoadedURI() throws IOException {
        final String expected = "http://www.google.com/";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node/uri.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    /**
     * Verify that the Name-type property loaded correctly.
     *
     * This test is only really useful post-JCR 2 upgrade.
     */

    public void testLoadedName() throws IOException {
        final String expected = "sling:test";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node/name.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    /**
     * Verify that the Path-type property loaded correctly.
     *
     * This test is only really useful post-JCR 2 upgrade.
     */

    public void testLoadedPath() throws IOException {
        final String expected = "/sling-test/initial-content-folder/folder-content-test";
        final String content = getContent(
                HTTP_BASE_URL + "/sling-test/property-types-test/test-node/path.txt", CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

}
